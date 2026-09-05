// Teto ponytail da Fase 5: compactação da fila, delete-no-ack e backfill de uid vivem no Room/SQL
// — cobertos por revisão; exigiriam instrumentação p/ teste automatizado.
package com.chronopass.app.sync

import com.chronopass.app.data.entities.Employee
import com.chronopass.app.data.entities.Punch
import com.chronopass.app.data.entities.PunchType
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fase 5 (SUMUS-INTEGRACAO.md §10.5): codec da outbox — JUnit puro, sem Robolectric/mocks. */
class OutboxPayloadsTest {

    @Test
    fun roundTrip_employee() {
        val e = Employee(id = 5, uid = "emp-1", name = "Ana", code = "A1",
                active = true, deleted = false, createdAt = 1_234_567L)
        val d = OutboxPayloads.decodeEmployee(OutboxPayloads.employeeJson(e))

        assertEquals(5L, d.id)
        assertEquals("emp-1", d.uid)
        assertEquals("Ana", d.name)
        assertEquals("A1", d.code)
        assertTrue(d.active)
        assertFalse(d.deleted)
        assertEquals(1_234_567L, d.createdAt)
    }

    @Test
    fun roundTrip_punch_comESemNulos() {
        val completo = Punch(
                id = 3, uid = "p-1", employeeId = 2, timestamp = 1_600_000_000_000L, type = PunchType.IN,
                latitude = -23.5, longitude = -46.6, accuracy = 10f, photoPath = "fotos/x.jpg",
                createdAt = 1_600_000_000_100L, editedBy = "admin", editedAt = 1_600_000_000_200L,
                editReason = "correção", deleted = false)
        val json1 = OutboxPayloads.punchJson(completo)
        assertFalse(json1.isNull("latitude"))
        val d1 = OutboxPayloads.decodePunch(json1)
        assertEquals(3L, d1.id)
        assertEquals("p-1", d1.uid)
        assertEquals(2L, d1.employeeId)
        assertEquals(completo.timestamp, d1.timestamp)
        assertEquals(PunchType.IN, d1.type)
        assertEquals(-23.5, d1.latitude!!, 0.0)
        assertEquals(-46.6, d1.longitude!!, 0.0)
        assertEquals(10f, d1.accuracy!!, 0f)
        assertEquals("fotos/x.jpg", d1.photoPath)
        assertEquals("admin", d1.editedBy)
        assertEquals(completo.editedAt, d1.editedAt)
        assertEquals("correção", d1.editReason)
        assertFalse(d1.deleted)
        assertEquals(completo.createdAt, d1.createdAt)

        // só obrigatórios (uid null): nulos viram JSONObject.NULL no encode e null no decode
        val soObrigatorio = Punch(uid = null, employeeId = 1, timestamp = 1_000L, type = PunchType.OUT)
        val json2 = OutboxPayloads.punchJson(soObrigatorio)
        assertTrue(json2.isNull("uid"))
        assertTrue(json2.isNull("latitude"))
        assertTrue(json2.isNull("longitude"))
        assertTrue(json2.isNull("accuracy"))
        assertTrue(json2.isNull("photoPath"))
        assertTrue(json2.isNull("editedBy"))
        assertTrue(json2.isNull("editedAt"))
        assertTrue(json2.isNull("editReason"))
        val d2 = OutboxPayloads.decodePunch(json2)
        assertNull(d2.uid)
        assertNull(d2.latitude)
        assertNull(d2.longitude)
        assertNull(d2.accuracy)
        assertNull(d2.photoPath)
        assertNull(d2.editedBy)
        assertNull(d2.editedAt)
        assertNull(d2.editReason)
        assertEquals(1L, d2.employeeId)
        assertEquals(1_000L, d2.timestamp)
        assertEquals(PunchType.OUT, d2.type)
        assertEquals(0L, d2.id)
    }

    @Test
    fun roundTrip_photo() {
        val r = OutboxPayloads.PhotoRef(punchUid = "p-7", photoPath = "fotos/2026-08-25_08-03-12.jpg")
        val d = OutboxPayloads.decodePhoto(OutboxPayloads.photoJson(r))

        assertEquals("p-7", d.punchUid)
        assertEquals("fotos/2026-08-25_08-03-12.jpg", d.photoPath)
    }

    @Test
    fun decode_tolerante() {
        // employee vazio: campos ausentes viram ''/null, nunca exceção
        val e = OutboxPayloads.decodeEmployee(JSONObject())
        assertEquals(0L, e.id)
        assertNull(e.uid)
        assertEquals("", e.name)
        assertEquals("", e.code)
        assertNull(e.photoPath)
        assertTrue(e.active)
        assertFalse(e.deleted)
        assertEquals(0L, e.createdAt)

        // punch parcial: type é obrigatório (getString), o resto tolera ausência/NULL
        val p = OutboxPayloads.decodePunch(
                JSONObject().put("type", "IN").put("latitude", JSONObject.NULL))
        assertEquals(PunchType.IN, p.type)
        assertEquals(0L, p.id)
        assertNull(p.uid)
        assertEquals(0L, p.employeeId)
        assertEquals(0L, p.timestamp)
        assertNull(p.latitude)
        assertNull(p.longitude)
        assertNull(p.accuracy)
        assertNull(p.photoPath)
        assertNull(p.editedBy)
        assertNull(p.editedAt)
        assertNull(p.editReason)
        assertFalse(p.deleted)
        assertEquals(0L, p.createdAt)
    }
}
