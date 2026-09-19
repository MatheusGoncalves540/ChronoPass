package com.chronopass.app.sync

import com.chronopass.app.data.entities.PunchType
import java.time.OffsetDateTime
import org.json.JSONException
import org.json.JSONObject

/**
 * Fase 3 do plano bidirecional: parser do canal de descida
 * (GET /api/integrations/chronopass/pull).
 *
 * Arquivo 100% JVM — nenhum import android.* (mesmo padrão de SummusPayloads/OutboxPayloads), então
 * o envelope inteiro é testável em JUnit puro. Quem fala HTTP (SummusClient) e quem grava
 * (ChronoRepository.applyFromSummus) só recebem/devolvem os tipos daqui.
 *
 * Fronteira de confiança: NADA aqui estoura para o chamador. Envelope inválido, schemaVersion
 * desconhecido, data fora do RFC3339 ou punchType estranho viram [PullResult.Falha] — e falha
 * significa cursor parado, ou seja, o servidor reenvia a mesma janela no próximo pull. Perder uma
 * correção é irreversível; reprocessá-la é idempotente (guarda de revisão em SyncRules).
 */

/** Loja da descida. `radiusMeters` é Double no contrato; o Room guarda Float. */
data class StorePull(
        val uid: String,
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val radiusMeters: Double,
)

/**
 * Envelope decodificado.
 *
 * `serverTime` é cursor OPACO: volta como `?since=` sem nenhuma aritmética de data do lado do app —
 * relógio do aparelho não entra na conta.
 */
data class Pull(
        val serverTime: String,
        val store: StorePull?,
        val employees: List<SummusEmployee>,
        val correcoes: List<SummusPunchCorrection>,
)

sealed interface PullResult {
    data class Ok(val pull: Pull) : PullResult

    /** Falha limpa: o chamador reporta e NÃO avança o cursor. */
    data class Falha(val motivo: String) : PullResult
}

object PullPayloads {

    const val SCHEMA_VERSION = 1

    fun parse(body: String): PullResult =
            try {
                val o = JSONObject(body)
                val v = o.optInt("schemaVersion", -1)
                if (v != SCHEMA_VERSION) {
                    PullResult.Falha("schemaVersion $v não suportado (esperado $SCHEMA_VERSION)")
                } else {
                    PullResult.Ok(
                            Pull(
                                    serverTime = o.getString("serverTime"),
                                    store = o.optJSONObject("store")?.let { store(it) },
                                    employees = o.lista("employees") { employee(it) },
                                    correcoes = o.lista("punchCorrections") { correcao(it) },
                            )
                    )
                }
            } catch (e: Exception) {
                // Exception (não Throwable): JSONException, DateTimeParseException, campo faltando.
                PullResult.Falha("pull inválido: ${e.message ?: e.javaClass.simpleName}")
            }

    // --- helpers ---

    private fun store(o: JSONObject) =
            StorePull(
                    uid = o.getString("uid"),
                    name = o.getString("name"),
                    latitude = o.getDouble("latitude"),
                    longitude = o.getDouble("longitude"),
                    radiusMeters = o.getDouble("radiusMeters"),
            )

    private fun employee(o: JSONObject) =
            SummusEmployee(
                    uid = o.getString("uid"),
                    name = o.getString("name"),
                    role = o.strOrNull("role"),
                    active = o.optBoolean("active", true),
                    deleted = o.optBoolean("deleted", false),
                    photoHash = o.strOrNull("photoHash"),
                    // Ausente (servidor velho) -> lista vazia.
                    mergeUids =
                            o.optJSONArray("mergeUids")?.let { a ->
                                (0 until a.length()).map { a.getString(it) }
                            }
                                    ?: emptyList(),
            )

    private fun correcao(o: JSONObject) =
            SummusPunchCorrection(
                    uid = o.getString("uid"),
                    type = punchType(o.getString("punchType")),
                    timestamp = epochMs(o.getString("timestampUtc")),
                    editedBy = o.strOrNull("editedBy"),
                    editedAt = o.strOrNull("editedAt")?.let { epochMs(it) },
                    editReason = o.strOrNull("editReason"),
                    deleted = o.optBoolean("deleted", false),
                    revision = o.getInt("revision"),
            )

    /** "in"/"out" do servidor -> enum do app. Desconhecido derruba o pull inteiro (nunca some). */
    private fun punchType(s: String): PunchType =
            when (s.lowercase()) {
                "in" -> PunchType.IN
                "out" -> PunchType.OUT
                else -> throw JSONException("punchType desconhecido: $s")
            }

    // OffsetDateTime (não Instant.parse): aceita "…Z" e "…-03:00" — no Android 8 o ISO_INSTANT
    // ainda exige Z.
    private fun epochMs(s: String): Long = OffsetDateTime.parse(s).toInstant().toEpochMilli()

    private inline fun <T> JSONObject.lista(k: String, item: (JSONObject) -> T): List<T> {
        val a = optJSONArray(k) ?: return emptyList()
        return (0 until a.length()).map { item(a.getJSONObject(it)) }
    }

    // Ausente ou null explícito -> null (mesmo espírito de OutboxPayloads).
    private fun JSONObject.strOrNull(k: String): String? = if (isNull(k)) null else getString(k)
}
