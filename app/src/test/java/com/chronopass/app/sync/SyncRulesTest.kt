package com.chronopass.app.sync

import com.chronopass.app.data.entities.OutboxItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fase 3/4: backoff medido de lastAttemptAt e guarda de revisão da descida. JUnit puro. */
class SyncRulesTest {

    private fun item(tentativas: Int, createdAt: Long, lastAttemptAt: Long?) =
            OutboxItem(
                    id = 1,
                    tipo = "PUNCH",
                    refUid = "p-1",
                    payload = "{}",
                    status = "FAILED",
                    tentativas = tentativas,
                    createdAt = createdAt,
                    lastAttemptAt = lastAttemptAt,
            )

    @Test
    fun backoffMedeDeLastAttemptAt_naoDeCreatedAt() {
        val agora = 10_000_000L
        // Item VELHO (criado há uma semana) mas tentado agora há pouco: tem de esperar.
        val velhoTentadoAgora =
                item(tentativas = 3, createdAt = agora - 7 * 24 * 3_600_000L, lastAttemptAt = agora - 1_000L)
        assertFalse(SyncRules.elegivel(agora, velhoTentadoAgora))

        // O mesmo item medido pelo createdAt (regra antiga) passaria — é o bug que a coluna corrige.
        assertTrue(agora - velhoTentadoAgora.createdAt >= SyncRules.backoff(3))

        // Passado o intervalo de 5 min da última tentativa, volta a ser elegível.
        assertTrue(SyncRules.elegivel(agora, velhoTentadoAgora.copy(lastAttemptAt = agora - 300_000L)))
    }

    @Test
    fun semTentativaAindaCaiNoCreatedAt() {
        val agora = 1_000_000L
        assertTrue(SyncRules.elegivel(agora, item(0, agora, null)))
        // Linha gravada antes da v5 (lastAttemptAt nulo) com tentativas: usa createdAt como base.
        assertFalse(SyncRules.elegivel(agora, item(1, agora - 10_000L, null)))
        assertTrue(SyncRules.elegivel(agora, item(1, agora - 30_000L, null)))
    }

    @Test
    fun guardaDeRevisao_menorOuIgualNaoAplica() {
        assertFalse(SyncRules.aplicaRevisao(local = 3, recebida = 2))
        assertFalse(SyncRules.aplicaRevisao(local = 3, recebida = 3)) // reenvio: idempotente
        assertTrue(SyncRules.aplicaRevisao(local = 3, recebida = 4))
        assertTrue(SyncRules.aplicaRevisao(local = null, recebida = 1)) // nunca corrigido
        assertFalse(SyncRules.aplicaRevisao(local = null, recebida = 0))
    }
}
