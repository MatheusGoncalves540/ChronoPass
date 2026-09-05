package com.chronopass.app.sync

import com.chronopass.app.data.entities.Employee
import com.chronopass.app.data.entities.Punch
import com.chronopass.app.data.entities.Store
import java.time.Instant
import java.util.TimeZone
import org.json.JSONArray
import org.json.JSONObject

/**
 * Fase 3 (SUMUS-INTEGRACAO.md §5): montagem dos dois lotes de envio.
 *
 * Arquivo 100% JVM — nenhum import android.*. Tudo que é Android (device id/model,
 * appVersion, store, exportedAt) chega por parâmetro, então a Fase 5 testa em JUnit puro.
 *
 * CONTRATO REAL (2026-09): o shape emitido espelha o servidor SummusBackoffice
 * (apps/server/internal/modules/rh/chrono.go), não o rascunho antigo do §5. Diferenças
 * aplicadas: punchType "in"/"out", timestampUtc/editedAt/exportedAt em RFC3339 (não epoch ms),
 * employee denorm {name,role} sem uid/code, photoKey em vez de photo{}, store.id string,
 * employees[] sem code/createdAt, lote 1 sem exportedAt. A conversão do formato interno da fila
 * (epoch ms / type IN-OUT) para o formato do servidor acontece SÓ aqui.
 *
 * Regra de nulos (§6): campo sem valor vira JSONObject.NULL explícito — nunca é omitido.
 */
object SummusPayloads {

    /** Foto pronta para o Lote 2. Os bytes já vêm em Base64 (gerado na parte Android). */
    data class PhotoPayload(
            val key: String,
            val fileName: String,
            val contentType: String = "image/jpeg",
            val dataBase64: String,
    )

    /**
     * Lote 1 — metadados (employees + punches).
     *
     * @param timeZone fuso usado no tzOffsetMinutes de cada ponto. Default = fuso do aparelho no
     *   momento do sync; o offset é avaliado no instante do timestamp ([TimeZone.getOffset]), então
     *   DST entra certo para cada ponto. Determinístico: mesmos inputs + mesmo fuso → mesmo JSON
     *   (testes da Fase 5 injetam um fuso fixo). Limitação aceita: se o aparelho mudar de fuso entre
     *   a marcação e o sync, o offset reflete o fuso atual aplicado àquele instante (o Room não
     *   guarda o fuso por ponto).
     * @param exportedAt mantido na assinatura por compatibilidade de chamada — o struct do servidor
     *   (chrono.go) NÃO tem exportedAt no lote 1, então o campo não é emitido.
     */
    fun buildLote1(
            deviceId: String,
            deviceModel: String,
            appVersion: String,
            store: Store?,
            exportedAt: Long,
            employees: List<Employee>,
            punches: List<Punch>,
            timeZone: TimeZone = TimeZone.getDefault(),
    ): JSONObject {
        val byId = employees.associateBy { it.id }

        val employeesJson = JSONArray()
        for (e in employees) employeesJson.put(employeeJson(e))

        val punchesJson = JSONArray()
        for (p in punches) punchesJson.put(punchJson(p, byId[p.employeeId], timeZone))

        return JSONObject()
                .put("schemaVersion", 1)
                .put("app", "chronopass")
                .put("appVersion", appVersion)
                .put("device", deviceJson(deviceId, deviceModel))
                .put("store", storeJson(store))
                .put(
                        "summary",
                        JSONObject()
                                .put("punchCount", punches.size)
                                .put(
                                        "uniqueEmployeeCount",
                                        punches.mapNotNull { byId[it.employeeId]?.uid }.distinct().size
                                )
                )
                .put("employees", employeesJson)
                .put("punches", punchesJson)
    }

    /**
     * Lote 2 — fotos (envio separado; falha sozinho sem perder metadados, §5).
     * Não leva `app`/`appVersion` (o schema do servidor não os define neste envelope).
     */
    fun buildLote2(
            deviceId: String,
            deviceModel: String,
            store: Store?,
            exportedAt: Long,
            photos: List<PhotoPayload>,
    ): JSONObject {
        val photosJson = JSONArray()
        for (ph in photos) photosJson.put(photoJson(ph))
        return JSONObject()
                .put("schemaVersion", 1)
                .put("loteType", "photos")
                .put("device", deviceJson(deviceId, deviceModel))
                .put("store", storeJson(store))
                .put("exportedAt", Instant.ofEpochMilli(exportedAt).toString())
                .put("photos", photosJson)
    }

    /** Nome base do arquivo (ex.: "2026-08-25_08-03-12.jpg"), sem o diretório. */
    fun photoFileName(path: String): String {
        val i = path.lastIndexOf('/')
        return if (i >= 0) path.substring(i + 1) else path
    }

    // --- helpers ---

    private fun deviceJson(deviceId: String, deviceModel: String) =
            JSONObject().put("id", deviceId).put("model", deviceModel)

    // Contrato real: store.id é STRING (chronoStore.ID string); name nullable com default.
    private fun storeJson(s: Store?) =
            JSONObject().put("id", (s?.id ?: 1L).toString()).put("name", s?.name ?: "Loja Principal")

    // employees[] do servidor: {uid, name, role, active, deleted} — sem code/createdAt.
    private fun employeeJson(e: Employee) =
            JSONObject()
                    .put("uid", e.uid ?: JSONObject.NULL)
                    .put("name", e.name)
                    .put("role", JSONObject.NULL) // app não tem cargo → null explícito
                    .put("active", e.active)
                    .put("deleted", e.deleted)

    // Funcionário denormalizado DENTRO do punch (chronoPunchEmployee): SÓ name+role — sem uid/code.
    private fun punchEmployeeJson(e: Employee) =
            JSONObject()
                    .put("name", e.name)
                    .put("role", JSONObject.NULL) // app não tem cargo → null explícito

    private fun punchJson(p: Punch, emp: Employee?, timeZone: TimeZone) =
            JSONObject()
                    .put("uid", p.uid ?: JSONObject.NULL)
                    // emp nulo = funcionário ausente do lote (inconsistência de dados): null explícito.
                    .put("employee", emp?.let { punchEmployeeJson(it) } ?: JSONObject.NULL)
                    .put("punchType", p.type.name.lowercase()) // "in"/"out"
                    .put("timestampUtc", Instant.ofEpochMilli(p.timestamp).toString())
                    .put("tzOffsetMinutes", timeZone.getOffset(p.timestamp) / 60_000)
                    .put("latitude", p.latitude ?: JSONObject.NULL)
                    .put("longitude", p.longitude ?: JSONObject.NULL)
                    // photoKey = uid do ponto (ref da foto); os bytes/fileName/contentType vivem no Lote 2.
                    .put("photoKey", if (p.photoPath != null) p.uid else JSONObject.NULL)
                    .put("editedBy", p.editedBy ?: JSONObject.NULL)
                    .put(
                            "editedAt",
                            p.editedAt?.let { Instant.ofEpochMilli(it).toString() }
                                    ?: JSONObject.NULL
                    )
                    .put("editReason", p.editReason ?: JSONObject.NULL)
                    .put("deleted", p.deleted)

    private fun photoJson(ph: PhotoPayload) =
            JSONObject()
                    .put("key", ph.key)
                    .put("fileName", ph.fileName)
                    .put("contentType", ph.contentType)
                    .put("dataBase64", ph.dataBase64)
}
