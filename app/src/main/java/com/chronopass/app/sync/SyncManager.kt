package com.chronopass.app.sync

import android.content.Context
import com.chronopass.app.data.database.ChronoDatabase
import com.chronopass.app.data.repo.ChronoRepository
import com.chronopass.app.ui.SUMUS_API_KEY
import com.chronopass.app.ui.SUMUS_URL_KEY
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Fase 4 (SUMUS-INTEGRACAO.md §6-7, §10.4): motor de drenagem da sync_outbox.
 *
 * Roda os dois lotes do §5 com ack-delete: lote 1 (employees+punches) e lote 2 (fotos). Sem coluna
 * de última tentativa no schema (§7): o backoff p/ retry usa createdAt + tentativas (restart-safe —
 * após reiniciar o aparelho os FAILED esperam o mesmo intervalo). O backoff filtra em memória aqui;
 * as queries do DAO devolvem PENDING+FAILED.
 */

/** Resultado de uma execução do dreno (retornado a [SyncManager.sync]). */
sealed interface SyncOutcome {
    object Inativo : SyncOutcome // sem URL/api-key configurados
    object Ocioso : SyncOutcome // fila sem eventos elegíveis
    data class Ok(val funcionarios: Int, val pontos: Int, val fotos: Int) : SyncOutcome
    object JaRodando : SyncOutcome
    data class Falha(val mensagem: String) : SyncOutcome
}

object SyncManager {
    // Anti-corrida: um sync por vez; chamadas concorrentes devolvem JaRodando.
    private val guard = AtomicBoolean(false)

    suspend fun sync(context: Context, repo: ChronoRepository): SyncOutcome =
            withContext(Dispatchers.IO) {
                if (!guard.compareAndSet(false, true)) return@withContext SyncOutcome.JaRodando
                try {
                    val url = repo.setting(SUMUS_URL_KEY)
                    // url = URL BASE; o cliente anexa os paths fixos /sync e /photos.
                    val key = repo.setting(SUMUS_API_KEY)
                    if (url.isBlank() || key.isBlank()) return@withContext SyncOutcome.Inativo

                    val outbox = ChronoDatabase.get(context).outboxDao()
                    val agora = System.currentTimeMillis()

                    var lote1Ok = false
                    var lote1Falhou = false
                    var lote1Msg = ""
                    var redeFora = false
                    var funcionarios = 0
                    var pontos = 0

                    // --- Lote 1: funcionários + pontos (metadados) ---
                    val itens = outbox.pendingMetadata()
                    val elegiveis = itens.filter { agora - it.createdAt >= backoff(it.tentativas) }
                    if (elegiveis.isNotEmpty()) {
                        val snapshots =
                                elegiveis.filter { it.tipo == "EMPLOYEE" }.map {
                                    OutboxPayloads.decodeEmployee(JSONObject(it.payload))
                                }
                        val punches =
                                elegiveis.filter { it.tipo == "PUNCH" }.map {
                                    OutboxPayloads.decodePunch(JSONObject(it.payload))
                                }
                        pontos = punches.size

                        // Dedup por id: snapshots enfileirados mandam; punch cujo employee não
                        // veio no snapshot puxa o estado atual (null = inconsistência, buildLote1
                        // tolera).
                        val idsSnapshot = snapshots.map { it.id }.toSet()
                        val complemento =
                                punches.mapNotNull { p ->
                                    repo.employee(p.employeeId)?.takeIf { it.id !in idsSnapshot }
                                }
                        val funcionariosFinal =
                                (snapshots + complemento).associateBy { it.id }.values.toList()
                        funcionarios = funcionariosFinal.size

                        val body1 =
                                SummusPayloads.buildLote1(
                                        SummusClient.deviceId(context),
                                        SummusClient.deviceModel(),
                                        SummusClient.appVersion(context),
                                        repo.store(),
                                        agora,
                                        funcionariosFinal,
                                        punches,
                                )
                        val ids1 = elegiveis.map { it.id }
                        when (val r = SummusClient.postSync(url, key, body1)) {
                            is SummusClient.PostResult.Ack -> {
                                outbox.deleteAll(ids1)
                                lote1Ok = true
                            }
                            is SummusClient.PostResult.HttpError -> {
                                outbox.markFailed(ids1, "HTTP ${r.httpCode}")
                                lote1Falhou = true
                                lote1Msg = "HTTP ${r.httpCode}"
                            }
                            is SummusClient.PostResult.TransportError -> {
                                outbox.markFailed(ids1, "rede: ${r.message}")
                                lote1Falhou = true
                                lote1Msg = "rede: ${r.message}"
                                redeFora = true // rede fora: economiza, não tenta o lote 2 agora
                            }
                        }
                    }

                    // --- Lote 2: fotos (independente do lote 1, §5; HttpError não bloqueia) ---
                    var lote2Ok = false
                    var lote2Falhou = false
                    var lote2Msg = ""
                    var fotos = 0
                    if (!redeFora) {
                        val fotosPendentes = outbox.pendingPhotos()
                        val elegiveisFotos =
                                fotosPendentes.filter {
                                    agora - it.createdAt >= backoff(it.tentativas)
                                }
                        if (elegiveisFotos.isNotEmpty()) {
                            val photos = mutableListOf<SummusPayloads.PhotoPayload>()
                            val ids2 = mutableListOf<Long>()
                            for (item in elegiveisFotos) {
                                val ref = OutboxPayloads.decodePhoto(JSONObject(item.payload))
                                // photoPayload devolve null se o arquivo sumiu/ilegível:
                                // irrecuperável,
                                // metadados já carregam a ref — descarta e segue.
                                SummusClient.photoPayload(ref.punchUid, ref.photoPath)?.let { pp ->
                                    photos += pp
                                    ids2 += item.id
                                }
                                        ?: outbox.delete(item.id)
                            }
                            if (photos.isNotEmpty()) {
                                val body2 =
                                        SummusPayloads.buildLote2(
                                                SummusClient.deviceId(context),
                                                SummusClient.deviceModel(),
                                                repo.store(),
                                                agora,
                                                photos,
                                        )
                                when (val r = SummusClient.postPhotos(url, key, body2)) {
                                    is SummusClient.PostResult.Ack -> {
                                        outbox.deleteAll(ids2)
                                        fotos = ids2.size
                                        lote2Ok = true
                                    }
                                    is SummusClient.PostResult.HttpError -> {
                                        outbox.markFailed(ids2, "HTTP ${r.httpCode}")
                                        lote2Falhou = true
                                        lote2Msg = "HTTP ${r.httpCode}"
                                    }
                                    is SummusClient.PostResult.TransportError -> {
                                        outbox.markFailed(ids2, "rede: ${r.message}")
                                        lote2Falhou = true
                                        lote2Msg = "rede: ${r.message}"
                                    }
                                }
                            }
                        }
                    }

                    // Lote 1 falhou -> Falha (lote 2 pode ter rodado em HttpError; segue FAILED p/
                    // retry).
                    // Lote 1 Ack -> Ok mesmo se o lote 2 falhou: filas independentes, a falha fica
                    // marcada.
                    when {
                        lote1Falhou -> SyncOutcome.Falha(lote1Msg)
                        lote1Ok -> SyncOutcome.Ok(funcionarios, pontos, fotos)
                        lote2Falhou -> SyncOutcome.Falha(lote2Msg)
                        lote2Ok -> SyncOutcome.Ok(0, 0, fotos)
                        else -> SyncOutcome.Ocioso
                    }
                } finally {
                    guard.set(false)
                }
            }

    // Backoff p/ retry: 1ª -> 30s, 2ª -> 60s, demais -> 5min. Base = createdAt (schema §7 não tem
    // coluna de última tentativa); restart-safe: o relógio recomeça a cada sync.
    private fun backoff(tentativas: Int): Long =
            when (tentativas) {
                0 -> 0L
                1 -> 30_000L
                2 -> 60_000L
                else -> 300_000L
            }
}
