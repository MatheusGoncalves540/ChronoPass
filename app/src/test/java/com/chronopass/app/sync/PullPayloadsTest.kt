package com.chronopass.app.sync

import com.chronopass.app.data.entities.PunchType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fase 3: parser do canal de descida. O envelope aqui é o contrato CONGELADO do plano — se o
 * servidor mudar, este teste é o primeiro a gritar.
 */
class PullPayloadsTest {

    private fun ok(body: String): Pull {
        val r = PullPayloads.parse(body)
        assertTrue("esperava Ok, veio $r", r is PullResult.Ok)
        return (r as PullResult.Ok).pull
    }

    @Test
    fun parseCompleto() {
        val p =
                ok(
                        """
                {
                  "schemaVersion": 1,
                  "serverTime": "2026-09-05T14:03:12.000Z",
                  "store": { "uid": "s-1", "name": "Loja Centro", "latitude": -23.5,
                             "longitude": -46.6, "radiusMeters": 150.0 },
                  "employees": [
                    { "uid": "e-1", "name": "Ana", "role": "Caixa", "active": true,
                      "deleted": false, "photoHash": "abc123" }
                  ],
                  "punchCorrections": [
                    { "uid": "p-1", "punchType": "out", "timestampUtc": "2026-09-05T11:00:00.000Z",
                      "editedBy": "rh", "editedAt": "2026-09-05T12:00:00.000Z",
                      "editReason": "esqueceu de bater", "deleted": false, "revision": 3 }
                  ]
                }
                """
                )

        assertEquals("2026-09-05T14:03:12.000Z", p.serverTime)
        assertEquals("s-1", p.store?.uid)
        assertEquals(150.0, p.store!!.radiusMeters, 0.001)
        assertEquals("Caixa", p.employees.single().role)
        assertEquals("abc123", p.employees.single().photoHash)

        val c = p.correcoes.single()
        assertEquals(PunchType.OUT, c.type)
        assertEquals(3, c.revision)
        // RFC3339 -> epoch ms (o app guarda Long).
        assertEquals(1_788_606_000_000L, c.timestamp)
        assertEquals(1_788_609_600_000L, c.editedAt)
    }

    @Test
    fun nulosExplicitosNaoEstouram() {
        val p =
                ok(
                        """
                {
                  "schemaVersion": 1,
                  "serverTime": "2026-09-05T14:03:12.000Z",
                  "store": null,
                  "employees": [
                    { "uid": "e-1", "name": "Ana", "role": null, "active": true,
                      "deleted": false, "photoHash": null }
                  ],
                  "punchCorrections": [
                    { "uid": "p-1", "punchType": "in", "timestampUtc": "2026-09-05T11:00:00.000Z",
                      "editedBy": null, "editedAt": null, "editReason": null,
                      "deleted": false, "revision": 1 }
                  ]
                }
                """
                )

        assertNull(p.store)
        assertNull(p.employees.single().role)
        assertNull(p.employees.single().photoHash)
        assertNull(p.correcoes.single().editedBy)
        assertNull(p.correcoes.single().editedAt)
        assertNull(p.correcoes.single().editReason)
    }

    @Test
    fun listasVaziasEAusentes() {
        val vazias =
                ok(
                        """{"schemaVersion":1,"serverTime":"2026-09-05T14:03:12.000Z",
                            "employees":[],"punchCorrections":[]}"""
                )
        assertTrue(vazias.employees.isEmpty())
        assertTrue(vazias.correcoes.isEmpty())

        // Chave ausente = lista vazia, não crash.
        val ausentes = ok("""{"schemaVersion":1,"serverTime":"2026-09-05T14:03:12.000Z"}""")
        assertTrue(ausentes.employees.isEmpty())
        assertTrue(ausentes.correcoes.isEmpty())
        assertNull(ausentes.store)
    }

    @Test
    fun schemaVersionInesperadoNaoCrasha() {
        for (body in
                listOf(
                        """{"schemaVersion":2,"serverTime":"2026-09-05T14:03:12.000Z"}""",
                        """{"serverTime":"2026-09-05T14:03:12.000Z"}""", // sem schemaVersion
                        """{"schemaVersion":"um"}""",
                )) {
            assertTrue("deveria falhar limpo: $body", PullPayloads.parse(body) is PullResult.Falha)
        }
    }

    @Test
    fun envelopeInvalidoFalhaLimpa() {
        // Nenhum destes pode estourar: falha = cursor parado = servidor reenvia a janela.
        val ruins =
                listOf(
                        "não é json",
                        "",
                        """{"schemaVersion":1}""", // sem serverTime
                        // punchType desconhecido derruba o pull inteiro em vez de sumir com a
                        // correção.
                        """{"schemaVersion":1,"serverTime":"2026-09-05T14:03:12.000Z",
                            "punchCorrections":[{"uid":"p-1","punchType":"pause",
                            "timestampUtc":"2026-09-05T11:00:00.000Z","revision":2}]}""",
                        // data fora do RFC3339, idem.
                        """{"schemaVersion":1,"serverTime":"2026-09-05T14:03:12.000Z",
                            "punchCorrections":[{"uid":"p-1","punchType":"in",
                            "timestampUtc":"05/09/2026","revision":2}]}""",
                )
        for (body in ruins) {
            assertTrue("deveria falhar limpo: $body", PullPayloads.parse(body) is PullResult.Falha)
        }
    }

    @Test
    fun mergeUidsParseado_eAusenteViraListaVazia() {
        val p =
                ok(
                        """
                {
                  "schemaVersion": 1,
                  "serverTime": "2026-09-05T14:03:12.000Z",
                  "store": null,
                  "employees": [
                    { "uid": "rh-1", "name": "Ana", "mergeUids": ["local-1", "local-2"] },
                    { "uid": "rh-2", "name": "Bia" }
                  ],
                  "punchCorrections": []
                }
                """
                )

        assertEquals(listOf("local-1", "local-2"), p.employees[0].mergeUids)
        // Servidor velho não manda o campo: lista vazia, nunca exceção.
        assertEquals(emptyList<String>(), p.employees[1].mergeUids)
    }
}
