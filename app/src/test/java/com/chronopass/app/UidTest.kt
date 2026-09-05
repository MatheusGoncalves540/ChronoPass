// Teto ponytail da Fase 5: compactação da fila, delete-no-ack e backfill de uid vivem no Room/SQL
// — cobertos por revisão; exigiriam instrumentação p/ teste automatizado.
package com.chronopass.app

import com.chronopass.app.data.entities.Employee
import com.chronopass.app.data.entities.Punch
import com.chronopass.app.data.entities.PunchType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/** Fase 5 (SUMUS-INTEGRACAO.md §10.5): uid default das entidades — JUnit puro. */
class UidTest {

    @Test
    fun uidGeradoNaCriacao() {
        assertNotNull(Employee(name = "Ana").uid)
        assertNotNull(Punch(employeeId = 1, timestamp = 1_000L, type = PunchType.IN).uid)
    }

    @Test
    fun uidUnicoPorInstancia() {
        assertNotEquals(Employee(name = "Ana").uid, Employee(name = "Bia").uid)
        assertNotEquals(
                Punch(employeeId = 1, timestamp = 1L, type = PunchType.IN).uid,
                Punch(employeeId = 1, timestamp = 2L, type = PunchType.IN).uid)
    }

    @Test
    fun copyPreservaUid() {
        val e = Employee(name = "Ana")
        assertEquals(e.uid, e.copy(name = "Ana Editada").uid)
        val p = Punch(employeeId = 1, timestamp = 1L, type = PunchType.IN)
        assertEquals(p.uid, p.copy(timestamp = 2L).uid)
    }
}
