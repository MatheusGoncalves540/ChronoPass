package com.chronopass.app.backup

import android.content.Context
import com.chronopass.app.camera.PhotoStore
import com.chronopass.app.data.database.ChronoDatabase
import com.chronopass.app.data.entities.*
import com.chronopass.app.data.repo.ChronoRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// ponytail: JSON manifest + photos in one zip. No versioning framework;
// on restore we wipe and reload. Good enough for a single-store backup.
object BackupManager {

    suspend fun export(context: Context, repo: ChronoRepository, out: File) {
        val root = JSONObject()
        val emp = JSONArray()
        for (e in repo.allEmployees()) emp.put(employeeJson(e))
        root.put("employees", emp)

        val pun = JSONArray()
        for (p in repo.allPunches()) pun.put(punchJson(p))
        root.put("punches", pun)

        repo.store()?.let { root.put("store", storeJson(it)) }

        val settings = JSONArray()
        for (s in repo.allSettings()) settings.put(JSONObject().put("key", s.key).put("value", s.value))
        root.put("settings", settings)

        ZipOutputStream(out.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("data.json"))
            zip.write(root.toString().toByteArray())
            zip.closeEntry()
            PhotoStore.dir(context).listFiles()?.forEach { photo ->
                zip.putNextEntry(ZipEntry("punches/${photo.name}"))
                photo.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    suspend fun import(context: Context, input: InputStream) {
        // wipe photos
        PhotoStore.dir(context).listFiles()?.forEach { it.delete() }
        var json: String? = null
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "data.json") {
                    json = zip.readBytes().decodeToString()
                } else if (entry.name.startsWith("punches/")) {
                    val f = File(PhotoStore.dir(context), entry.name.removePrefix("punches/"))
                    f.outputStream().use { zip.copyTo(it) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        json?.let { loadJson(context, JSONObject(it)) }
    }

    // --- helpers ---
    private suspend fun loadJson(context: Context, root: JSONObject) {
        val db = ChronoDatabase.get(context)
        db.clearAllTables()
        val empDao = db.employeeDao(); val punDao = db.punchDao()
        val storeDao = db.storeDao(); val setDao = db.settingsDao()

        val emps = root.optJSONArray("employees") ?: JSONArray()
        for (i in 0 until emps.length()) {
            val o = emps.getJSONObject(i)
            empDao.insert(Employee(o.getLong("id"), o.getString("name"), o.optString("code"),
                o.optBoolean("active", true), o.optLong("createdAt")))
        }
        val puns = root.optJSONArray("punches") ?: JSONArray()
        for (i in 0 until puns.length()) {
            val o = puns.getJSONObject(i)
            punDao.insert(Punch(
                id = o.getLong("id"), employeeId = o.getLong("employeeId"),
                timestamp = o.getLong("timestamp"), type = PunchType.valueOf(o.getString("type")),
                latitude = o.optDoubleOrNull("latitude"), longitude = o.optDoubleOrNull("longitude"),
                accuracy = if (o.isNull("accuracy")) null else o.getDouble("accuracy").toFloat(),
                photoPath = if (o.isNull("photoPath")) null else o.getString("photoPath"),
                createdAt = o.optLong("createdAt"),
                editedBy = if (o.isNull("editedBy")) null else o.getString("editedBy"),
                editedAt = if (o.isNull("editedAt")) null else o.getLong("editedAt"),
                editReason = if (o.isNull("editReason")) null else o.getString("editReason"),
            ))
        }
        if (root.has("store")) {
            val o = root.getJSONObject("store")
            storeDao.insert(Store(o.getLong("id"), o.getString("name"),
                o.getDouble("latitude"), o.getDouble("longitude"), o.getDouble("radius").toFloat()))
        }
        val sets = root.optJSONArray("settings") ?: JSONArray()
        for (i in 0 until sets.length()) {
            val o = sets.getJSONObject(i)
            setDao.set(AppSetting(o.getString("key"), o.getString("value")))
        }
    }

    private fun employeeJson(e: Employee) = JSONObject()
        .put("id", e.id).put("name", e.name).put("code", e.code)
        .put("active", e.active).put("createdAt", e.createdAt)
    private fun punchJson(p: Punch) = JSONObject()
        .put("id", p.id).put("employeeId", p.employeeId).put("timestamp", p.timestamp)
        .put("type", p.type.name).put("latitude", p.latitude ?: JSONObject.NULL)
        .put("longitude", p.longitude ?: JSONObject.NULL)
        .put("accuracy", p.accuracy?.toDouble() ?: JSONObject.NULL)
        .put("photoPath", p.photoPath ?: JSONObject.NULL).put("createdAt", p.createdAt)
        .put("editedBy", p.editedBy ?: JSONObject.NULL)
        .put("editedAt", p.editedAt ?: JSONObject.NULL)
        .put("editReason", p.editReason ?: JSONObject.NULL)
    private fun storeJson(s: Store) = JSONObject()
        .put("id", s.id).put("name", s.name).put("latitude", s.latitude)
        .put("longitude", s.longitude).put("radius", s.radius.toDouble())

    private fun JSONObject.optDoubleOrNull(k: String): Double? = if (isNull(k)) null else getDouble(k)
}
