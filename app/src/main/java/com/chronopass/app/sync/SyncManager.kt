package com.chronopass.app.sync

import android.content.Context
import com.chronopass.app.camera.PhotoStore
import com.chronopass.app.data.database.ChronoDatabase
import com.chronopass.app.data.repo.ChronoRepository
import com.chronopass.app.ui.SUMUS_API_KEY
import com.chronopass.app.ui.SUMUS_PULL_SINCE_KEY
import com.chronopass.app.ui.SUMUS_URL_KEY
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Fase 4 (SUMUS-INTEGRACAO.md §6-7, §10.4): motor de drenagem da sync_outbox.
 *
 * Roda os dois lotes do §5 com ack-delete: lote 1 (employees+punches) e lote 2 (fotos). Cada lote
 * repete em rodadas com teto ([SyncRules.LOTE_METADADOS] / [SyncRules.LOTE_FOTOS]) até esvaziar —
 * um aparelho semanas offline não monta mais um JSONObject único em memória contra o ReadTimeout.
 *
 * O backoff mede de `lastAttemptAt` ([SyncRules.elegivel]) e filtra em memória aqui; as queries do
 * DAO devolvem PENDING+FAILED.
 */

/** Resultado de uma execução do dreno (retornado a [SyncManager.sync]). */
sealed interface SyncOutcome {
    object Inativo : SyncOutcome // sem URL/api-key configurados
    object Ocioso : SyncOutcome // fila sem eventos elegíveis
    data class Ok(
            val funcionarios: Int,
            val pontos: Int,
            val fotos: Int,
            val recebidos: Int = 0, // itens aplicados na descida (cadastro + correções + fotos)
    ) : SyncOutcome
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

                    // --- Descida (pull) ANTES da drenagem: o aparelho recebe cadastro/loja/
                    // correções e só então devolve o que tem. Falha aqui não impede a subida (a
                    // fila é independente), mas é reportada — descida travada em silêncio deixa
                    // ponto corrigido aparecendo errado na tela.
                    val descida = pull(context, repo, url, key)

                    val outbox = ChronoDatabase.get(context).outboxDao()
                    val agora = System.currentTimeMillis()

                    var lote1Ok = false
                    var lote1Falhou = false
                    var lote1Msg = ""
                    var redeFora = false
                    var funcionarios = 0
                    var pontos = 0

                    // --- Lote 1: funcionários + pontos (metadados), em rodadas de até 50 ---
                    while (true) {
                        val elegiveis =
                                outbox.pendingMetadata()
                                        .filter { SyncRules.elegivel(agora, it) }
                                        .take(SyncRules.LOTE_METADADOS)
                        if (elegiveis.isEmpty()) break

                        val snapshots =
                                elegiveis.filter { it.tipo == "EMPLOYEE" }.map {
                                    OutboxPayloads.decodeEmployee(JSONObject(it.payload))
                                }
                        val punches =
                                elegiveis.filter { it.tipo == "PUNCH" }.map {
                                    OutboxPayloads.decodePunch(JSONObject(it.payload))
                                }

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
                        val r = SummusClient.postSync(url, key, body1)
                        if (r is SummusClient.PostResult.Ack) {
                            outbox.deleteAll(ids1)
                            lote1Ok = true
                            funcionarios += funcionariosFinal.size
                            pontos += punches.size
                            continue // próxima rodada até a fila esvaziar
                        }
                        lote1Falhou = true
                        lote1Msg =
                                when (r) {
                                    is SummusClient.PostResult.HttpError -> "HTTP ${r.httpCode}"
                                    is SummusClient.PostResult.TransportError -> {
                                        // rede fora: economiza, não tenta o lote 2 agora
                                        redeFora = true
                                        "rede: ${r.message}"
                                    }
                                    else -> ""
                                }
                        outbox.markFailed(ids1, lote1Msg, agora)
                        break
                    }

                    // --- Lote 2: fotos (independente do lote 1, §5; HttpError não bloqueia) ---
                    var lote2Ok = false
                    var lote2Falhou = false
                    var lote2Msg = ""
                    var fotos = 0
                    while (!redeFora) {
                        val elegiveisFotos =
                                outbox.pendingPhotos()
                                        .filter { SyncRules.elegivel(agora, it) }
                                        .take(SyncRules.LOTE_FOTOS)
                        if (elegiveisFotos.isEmpty()) break

                        val photos = mutableListOf<SummusPayloads.PhotoPayload>()
                        val ids2 = mutableListOf<Long>()
                        for (item in elegiveisFotos) {
                            val ref = OutboxPayloads.decodePhoto(JSONObject(item.payload))
                            // photoPayload devolve null se o arquivo sumiu/ilegível: irrecuperável,
                            // metadados já carregam a ref — descarta e segue.
                            SummusClient.photoPayload(ref.punchUid, ref.photoPath)?.let { pp ->
                                photos += pp
                                ids2 += item.id
                            }
                                    ?: outbox.delete(item.id)
                        }
                        // Rodada só de arquivos sumidos: já foram removidos da fila, segue.
                        if (photos.isEmpty()) continue

                        val body2 =
                                SummusPayloads.buildLote2(
                                        SummusClient.deviceId(context),
                                        SummusClient.deviceModel(),
                                        repo.store(),
                                        agora,
                                        photos,
                                )
                        val r = SummusClient.postPhotos(url, key, body2)
                        if (r is SummusClient.PostResult.Ack) {
                            outbox.deleteAll(ids2)
                            fotos += ids2.size
                            lote2Ok = true
                            continue
                        }
                        lote2Falhou = true
                        lote2Msg =
                                when (r) {
                                    is SummusClient.PostResult.HttpError -> "HTTP ${r.httpCode}"
                                    is SummusClient.PostResult.TransportError ->
                                            "rede: ${r.message}"
                                    else -> ""
                                }
                        outbox.markFailed(ids2, lote2Msg, agora)
                        break
                    }

                    // Lote 1 falhou -> Falha (lote 2 pode ter rodado em HttpError; segue FAILED p/
                    // retry).
                    // Lote 1 Ack -> Ok mesmo se o lote 2 falhou: filas independentes, a falha fica
                    // marcada.
                    when {
                        lote1Falhou -> SyncOutcome.Falha(lote1Msg)
                        descida.erro != null -> SyncOutcome.Falha("descida: ${descida.erro}")
                        lote1Ok ->
                                SyncOutcome.Ok(funcionarios, pontos, fotos, descida.recebidos)
                        lote2Falhou -> SyncOutcome.Falha(lote2Msg)
                        lote2Ok -> SyncOutcome.Ok(0, 0, fotos, descida.recebidos)
                        descida.recebidos > 0 -> SyncOutcome.Ok(0, 0, 0, descida.recebidos)
                        else -> SyncOutcome.Ocioso
                    }
                } finally {
                    guard.set(false)
                }
            }

    private data class Descida(val erro: String?, val recebidos: Int)

    /**
     * Um pull: GET -> parse -> aplica -> avança cursor -> baixa fotos.
     *
     * O cursor só anda dentro de [ChronoRepository.applyPull], depois da escrita: qualquer falha
     * antes disso deixa a janela para o próximo pull. Reprocessar é idempotente (guarda de
     * revisão); perder uma correção não é.
     */
    private suspend fun pull(
            context: Context,
            repo: ChronoRepository,
            url: String,
            key: String,
    ): Descida {
        val since = repo.setting(SUMUS_PULL_SINCE_KEY) // vazio = pull completo
        val r = SummusClient.get(SummusClient.pullUrl(url, since), key)
        if (r is SummusClient.GetResult.Erro) return Descida(r.motivo, 0)
        val p =
                when (val parsed = PullPayloads.parse((r as SummusClient.GetResult.Ok).body)) {
                    is PullResult.Falha -> return Descida(parsed.motivo, 0)
                    is PullResult.Ok -> parsed.pull
                }
        try {
            repo.applyPull(p)
        } catch (e: Exception) {
            return Descida("aplicação: ${e.message ?: e.javaClass.simpleName}", 0)
        }
        // Fotos fora do corpo do pull e depois do cursor: falha nelas não segura correção de ponto,
        // e o photoHash faz a próxima rodada tentar de novo (o roster vem inteiro em todo pull).
        val fotos = baixarFotos(context, repo, url, key, p.employees)
        return Descida(null, p.employees.size + p.correcoes.size + fotos)
    }

    /** Baixa UMA POR VEZ, só quando o hash mudou, com teto por rodada. */
    private suspend fun baixarFotos(
            context: Context,
            repo: ChronoRepository,
            url: String,
            key: String,
            employees: List<SummusEmployee>,
    ): Int {
        var baixadas = 0
        for (e in employees) {
            if (baixadas >= SyncRules.LOTE_FOTOS) break
            val hash = e.photoHash ?: continue
            if (repo.photoHashSummus(e.uid) == hash) continue
            val bytes =
                    SummusClient.getBytes(SummusClient.employeePhotoUrl(url, e.uid), key) ?: continue
            // Nome fixo por uid: sobrescrever muda o lastModified, que entra na chave de cache do
            // Coil — a tela mostra a foto nova sem invalidação manual.
            val destino = File(PhotoStore.employeeDir(context), "summus_${e.uid}.webp")
            if (runCatching { destino.writeBytes(bytes) }.isFailure) continue
            repo.applyEmployeePhotoFromSummus(e.uid, destino.absolutePath, hash)
            baixadas++
        }
        return baixadas
    }
}
