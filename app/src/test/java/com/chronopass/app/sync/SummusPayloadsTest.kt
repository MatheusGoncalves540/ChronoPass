// Teto ponytail da Fase 5: compactação da fila, delete-no-ack e backfill de uid vivem no Room/SQL
// — cobertos por revisão; exigiriam instrumentação p/ teste automatizado.
package com.chronopass.app.sync

import com.chronopass.app.data.entities.Employee
import com.chronopass.app.data.entities.Punch
import com.chronopass.app.data.entities.PunchType
import com.chronopass.app.data.entities.Store
import java.time.Instant
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fase 5 (SUMUS-INTEGRACAO.md §10.5): montagem dos lotes — JUnit puro, sem Robolectric/mocks. */
class SummusPayloadsTest {

    private val deviceId = "dev-123"
    private val deviceModel = "Pixel 8"
    private val appVersion = "2.2.0"
    private val exportedAt = 1_700_000_000_000L
    // GMT-3 fixo = determinístico (sem DST): tzOffsetMinutes sempre -180.
    private val gmtMinus3 = TimeZone.getTimeZone("GMT-03:00")

    @Test
    fun lote1_incluiEnvelopeESummary() {
        val ana = Employee(id = 1, uid = "emp-1", name = "Ana", code = "A1")
        val bia = Employee(id = 2, uid = "emp-2", name = "Bia", code = "B2")
        val store = Store(id = 7, name = "Loja Centro", latitude = -23.5, longitude = -46.6, radius = 100f)
        val punches = listOf(
                Punch(uid = "p-1", employeeId = 1, timestamp = 1L, type = PunchType.IN),
                Punch(uid = "p-2", employeeId = 1, timestamp = 2L, type = PunchType.OUT),
                Punch(uid = "p-3", employeeId = 2, timestamp = 3L, type = PunchType.IN),
        )
        val lote = SummusPayloads.buildLote1(
                deviceId, deviceModel, appVersion, store, exportedAt, listOf(ana, bia), punches, gmtMinus3)

        assertEquals(1, lote.optInt("schemaVersion"))
        assertEquals("chronopass", lote.optString("app"))
        assertEquals(appVersion, lote.optString("appVersion"))
        // Contrato real (chrono.go): lote 1 NÃO tem exportedAt.
        assertFalse(lote.has("exportedAt"))

        val device = lote.getJSONObject("device")
        assertEquals(deviceId, device.optString("id"))
        assertEquals(deviceModel, device.optString("model"))

        val storeJson = lote.getJSONObject("store")
        // store.id é STRING no contrato real.
        assertEquals("7", storeJson.optString("id"))
        assertEquals("Loja Centro", storeJson.optString("name"))

        val summary = lote.getJSONObject("summary")
        assertEquals(3, summary.optInt("punchCount"))
        assertEquals(2, summary.optInt("uniqueEmployeeCount"))

        val employees = lote.optJSONArray("employees")
        assertEquals(2, employees.length())
        // employees[] = {uid, name, role, active, deleted} — sem code/createdAt.
        val emp1 = employees.getJSONObject(0)
        assertEquals("emp-1", emp1.optString("uid"))
        assertEquals("Ana", emp1.optString("name"))
        assertTrue(emp1.isNull("role"))
        assertTrue(emp1.optBoolean("active"))
        assertFalse(emp1.optBoolean("deleted"))
        assertFalse(emp1.has("code"))
        assertFalse(emp1.has("createdAt"))

        assertEquals(3, lote.optJSONArray("punches").length())
    }

    @Test
    fun lote1_usaLojaDefault() {
        val ana = Employee(id = 1, uid = "emp-1", name = "Ana", code = "A1")
        val lote = SummusPayloads.buildLote1(
                deviceId, deviceModel, appVersion, null, exportedAt, listOf(ana),
                listOf(Punch(uid = "p-1", employeeId = 1, timestamp = 1L, type = PunchType.IN)), gmtMinus3)

        val store = lote.getJSONObject("store")
        // store null → id "1" (STRING), name "Loja Principal".
        assertEquals("1", store.optString("id"))
        assertEquals("Loja Principal", store.optString("name"))
    }

    @Test
    fun punch_carregaCamposDenormENulosExplicitos() {
        val ana = Employee(id = 1, uid = "emp-1", name = "Ana", code = "A1")
        val semFoto = Punch(uid = "p-1", employeeId = 1, timestamp = 1_600_000_000_000L, type = PunchType.IN)
        val comFoto = Punch(
                uid = "p-2", employeeId = 1, timestamp = 1_600_000_000_001L, type = PunchType.OUT,
                latitude = -23.5, longitude = -46.6, accuracy = 8f, photoPath = "fotos/x.jpg",
                editedBy = "admin", editedAt = 1_600_000_000_100L, editReason = "correção")
        val lote = SummusPayloads.buildLote1(
                deviceId, deviceModel, appVersion, null, exportedAt, listOf(ana),
                listOf(semFoto, comFoto), gmtMinus3)
        val punches = lote.optJSONArray("punches")

        // ponto sem foto: denormalização + nulos explícitos
        val p = punches.getJSONObject(0)
        assertEquals("p-1", p.optString("uid"))
        // employeeUid NÃO existe no contrato real.
        assertFalse(p.has("employeeUid"))
        val emp = p.getJSONObject("employee")
        // employee denorm = {name, role} — sem uid/code.
        assertFalse(emp.has("uid"))
        assertFalse(emp.has("code"))
        assertEquals("Ana", emp.optString("name"))
        assertTrue(emp.isNull("role"))
        assertEquals("in", p.optString("punchType"))
        // timestampUtc é STRING RFC3339 (parseável), não epoch ms.
        assertNull(p.opt("timestampUtc")?.let { it as? Number })
        val ts = p.optString("timestampUtc")
        assertTrue(ts.isNotEmpty())
        assertEquals(Instant.ofEpochMilli(semFoto.timestamp).toString(), Instant.parse(ts).toString())
        assertTrue(p.has("tzOffsetMinutes"))
        assertEquals(-180, p.optInt("tzOffsetMinutes"))
        assertTrue(p.isNull("latitude"))
        assertTrue(p.isNull("longitude"))
        // accuracyMeters não existe no contrato real.
        assertFalse(p.has("accuracyMeters"))
        assertTrue(p.isNull("editedBy"))
        assertTrue(p.isNull("editedAt"))
        assertTrue(p.isNull("editReason"))
        // photo vira photoKey: null quando não há foto.
        assertFalse(p.has("photo"))
        assertTrue(p.isNull("photoKey"))
        assertFalse(p.optBoolean("deleted"))

        // ponto com foto: photoKey = uid do punch (sem objeto photo{})
        val p2 = punches.getJSONObject(1)
        assertFalse(p2.has("photo"))
        assertEquals("p-2", p2.optString("photoKey"))
        assertEquals("out", p2.optString("punchType"))
        assertEquals(
                Instant.ofEpochMilli(comFoto.timestamp).toString(),
                Instant.parse(p2.optString("timestampUtc")).toString())
        assertEquals("admin", p2.optString("editedBy"))
        // editedAt em RFC3339 quando presente; null explícito quando ausente.
        assertEquals(
                Instant.ofEpochMilli(1_600_000_000_100L).toString(),
                Instant.parse(p2.optString("editedAt")).toString())
        assertEquals("correção", p2.optString("editReason"))
    }

    @Test
    fun lote2_estruturaFotos() {
        val fotos = listOf(
                SummusPayloads.PhotoPayload(
                        key = "p-9",
                        fileName = "2026-08-25_08-03-12.jpg",
                        contentType = "image/jpeg",
                        dataBase64 = "aGVsbG8="))
        val lote = SummusPayloads.buildLote2(deviceId, deviceModel, null, exportedAt, fotos)

        assertEquals(1, lote.optInt("schemaVersion"))
        assertEquals("photos", lote.optString("loteType"))
        assertEquals(deviceId, lote.getJSONObject("device").optString("id"))
        assertEquals("1", lote.getJSONObject("store").optString("id"))
        assertEquals("Loja Principal", lote.getJSONObject("store").optString("name"))
        // exportedAt é STRING RFC3339 (chronoPhotosPayload.ExportedAt *time.Time), não epoch ms.
        assertNull(lote.opt("exportedAt")?.let { it as? Number })
        assertEquals(Instant.ofEpochMilli(exportedAt).toString(), Instant.parse(lote.optString("exportedAt")).toString())

        val arr = lote.optJSONArray("photos")
        assertEquals(1, arr.length())
        val f = arr.getJSONObject(0)
        assertEquals("p-9", f.optString("key"))
        assertEquals("2026-08-25_08-03-12.jpg", f.optString("fileName"))
        assertEquals("image/jpeg", f.optString("contentType"))
        assertEquals("aGVsbG8=", f.optString("dataBase64"))
    }
}
