package com.chronopass.app.sync

import com.chronopass.app.data.entities.Employee
import com.chronopass.app.data.entities.Punch
import com.chronopass.app.data.entities.PunchType
import org.json.JSONObject

/**
 * Fase 4 (SUMUS-INTEGRACAO.md §7): codec dos snapshots enfileirados na sync_outbox.
 *
 * Arquivo 100% JVM — nenhum import android.* (mesmo padrão de SummusPayloads), então a Fase 5 testa
 * em JUnit puro. O payload é o "estado atual" da entidade no momento da escrita; o dreno lê com
 * decode* e reenvia ao Summus.
 *
 * Regra de nulos (§7): campo sem valor vira JSONObject.NULL explícito no encode — nunca é omitido.
 * No decode, campo ausente ou NULL vira null (tolerante a snapshots antigos).
 */
object OutboxPayloads {

    /** Referência de foto enfileirada como PHOTO (os bytes ficam no aparelho até o dreno). */
    data class PhotoRef(val punchUid: String, val photoPath: String)

    fun employeeJson(e: Employee) =
            JSONObject()
                    .put("id", e.id)
                    .put("uid", e.uid ?: JSONObject.NULL)
                    .put("name", e.name)
                    .put("code", e.code)
                    .put("active", e.active)
                    .put("deleted", e.deleted)
                    .put("createdAt", e.createdAt)

    fun punchJson(p: Punch) =
            JSONObject()
                    .put("id", p.id)
                    .put("uid", p.uid ?: JSONObject.NULL)
                    .put("employeeId", p.employeeId)
                    .put("timestamp", p.timestamp)
                    .put("type", p.type.name)
                    .put("latitude", p.latitude ?: JSONObject.NULL)
                    .put("longitude", p.longitude ?: JSONObject.NULL)
                    .put("accuracy", p.accuracy?.toDouble() ?: JSONObject.NULL)
                    .put("photoPath", p.photoPath ?: JSONObject.NULL)
                    .put("editedBy", p.editedBy ?: JSONObject.NULL)
                    .put("editedAt", p.editedAt ?: JSONObject.NULL)
                    .put("editReason", p.editReason ?: JSONObject.NULL)
                    .put("deleted", p.deleted)
                    .put("createdAt", p.createdAt)

    fun photoJson(r: PhotoRef) =
            JSONObject().put("punchUid", r.punchUid).put("photoPath", r.photoPath)

    fun decodeEmployee(o: JSONObject): Employee =
            Employee(
                    id = o.optLong("id"),
                    uid = o.optStringOrNull("uid"),
                    name = o.optStringOrNull("name") ?: "",
                    code = o.optStringOrNull("code") ?: "",
                    photoPath = o.optStringOrNull("photoPath"),
                    active = o.optBoolean("active", true),
                    deleted = o.optBoolean("deleted", false),
                    createdAt = o.optLong("createdAt"),
            )

    fun decodePunch(o: JSONObject): Punch =
            Punch(
                    id = o.optLong("id"),
                    uid = o.optStringOrNull("uid"),
                    employeeId = o.optLong("employeeId"),
                    timestamp = o.optLong("timestamp"),
                    type = PunchType.valueOf(o.getString("type")),
                    latitude = o.optDoubleOrNull("latitude"),
                    longitude = o.optDoubleOrNull("longitude"),
                    accuracy = o.optDoubleOrNull("accuracy")?.toFloat(),
                    photoPath = o.optStringOrNull("photoPath"),
                    createdAt = o.optLong("createdAt"),
                    editedBy = o.optStringOrNull("editedBy"),
                    editedAt = o.optLongOrNull("editedAt"),
                    editReason = o.optStringOrNull("editReason"),
                    deleted = o.optBoolean("deleted", false),
            )

    fun decodePhoto(o: JSONObject): PhotoRef =
            PhotoRef(punchUid = o.getString("punchUid"), photoPath = o.getString("photoPath"))

    // --- helpers (mesmo espírito do BackupManager: campo ausente/NULL → null) ---

    private fun JSONObject.optStringOrNull(k: String): String? =
            if (isNull(k)) null else getString(k)

    private fun JSONObject.optLongOrNull(k: String): Long? = if (isNull(k)) null else getLong(k)

    private fun JSONObject.optDoubleOrNull(k: String): Double? =
            if (isNull(k)) null else getDouble(k)
}
