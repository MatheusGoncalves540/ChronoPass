package com.chronopass.app.sync

import com.chronopass.app.data.entities.Employee
import com.chronopass.app.data.entities.ORIGIN_SUMMUS
import com.chronopass.app.data.entities.OutboxItem
import com.chronopass.app.data.entities.Punch
import com.chronopass.app.data.entities.PunchType

/**
 * Fase 3/4 do plano de sincronização bidirecional: regras puras do sync.
 *
 * Arquivo 100% JVM — nenhum import android.* (mesmo padrão de SummusPayloads/OutboxPayloads), então
 * backoff, guarda de revisão e merge da descida são testáveis em JUnit puro. Quem toca Room/Android
 * (ChronoRepository, SyncManager) só chama daqui.
 */

/**
 * Funcionário vindo da descida (pull) do Summus. Parser em PullPayloads.
 *
 * `photoHash` não entra no merge: é só o gatilho do download da imagem (SyncManager compara com o
 * último hash aplicado; igual = não baixa nada).
 */
data class SummusEmployee(
        val uid: String,
        val name: String,
        val role: String? = null,
        val active: Boolean = true,
        val deleted: Boolean = false,
        val photoHash: String? = null,
        // uids locais que o vínculo declarou serem a mesma pessoa: as batidas vêm para cá.
        val mergeUids: List<String> = emptyList(),
)

/** Correção de ponto vinda da descida. `revision` é a guarda de ordenação. */
data class SummusPunchCorrection(
        val uid: String,
        val type: PunchType,
        val timestamp: Long,
        val editedBy: String? = null,
        val editedAt: Long? = null,
        val editReason: String? = null,
        val deleted: Boolean = false,
        val revision: Int,
)

/**
 * Batida CRIADA no backoffice (não existe no aparelho). `employeeUids` são os candidatos a dono,
 * em ordem de preferência (cadastros locais vinculados primeiro, uid do RH por último): vale o
 * primeiro com linha VISÍVEL no aparelho. O `uid` da batida é a idempotência.
 */
data class SummusNewPunch(
        val uid: String,
        val employeeUids: List<String>,
        val type: PunchType,
        val timestamp: Long,
        val editedBy: String? = null,
        val editedAt: Long? = null,
        val editReason: String? = null,
        val deleted: Boolean = false,
        val revision: Int = 1,
)

object SyncRules {

    // Teto de lote por rodada (SyncManager repete até esvaziar): um aparelho semanas offline não
    // pode montar um JSONObject único em memória contra o ReadTimeout.
    const val LOTE_METADADOS = 50
    const val LOTE_FOTOS = 20

    /** Backoff p/ retry: 1ª -> 30s, 2ª -> 60s, demais -> 5min. */
    fun backoff(tentativas: Int): Long =
            when (tentativas) {
                0 -> 0L
                1 -> 30_000L
                2 -> 60_000L
                else -> 300_000L
            }

    /**
     * Item elegível p/ nova tentativa. Base = lastAttemptAt (createdAt só p/ item nunca tentado, e
     * p/ linha gravada antes da v5): medir de createdAt não freava nada com mais de 5 min de idade.
     */
    fun elegivel(agora: Long, item: OutboxItem): Boolean =
            agora - (item.lastAttemptAt ?: item.createdAt) >= backoff(item.tentativas)

    /**
     * Guarda de ordenação da correção de ponto: só aplica revisão MAIOR que a local. Idempotente
     * sob reenvio (mesma revisão duas vezes não reaplica) e imune a pull fora de ordem.
     */
    fun aplicaRevisao(local: Int?, recebida: Int): Boolean = recebida > (local ?: 0)

    /** Funcionário que o aparelho ainda não tem: nasce SUMMUS. */
    fun novoEmployee(s: SummusEmployee): Employee =
            Employee(
                    uid = s.uid,
                    name = s.name,
                    role = s.role,
                    active = s.active && !s.deleted,
                    origin = ORIGIN_SUMMUS,
            )

    /**
     * Funcionário existente: o Summus manda em nome/cargo/ativo e assume a posse do cadastro (um
     * LOCAL que volta com o mesmo uid virou vinculado). `deleted`/`active=false` DESATIVA — o campo
     * `deleted` local (lixeira, purgável) nunca é ligado por ordem do servidor.
     */
    fun mergeEmployee(local: Employee, s: SummusEmployee): Employee =
            local.copy(
                    name = s.name,
                    role = s.role,
                    active = s.active && !s.deleted,
                    origin = ORIGIN_SUMMUS,
            )

    /**
     * Quem fica na tela quando o vínculo junta cadastros da mesma pessoa. Só linhas VISÍVEIS (fora da
     * lixeira) podem sobreviver — absorver para dentro de uma linha da lixeira esconderia o
     * funcionário (bug da v2.2.2). Entre as visíveis vence o cadastro LOCAL da loja (uid diferente do
     * do RH), que é o que ela já usa e tem o histórico; a linha criada pela descida do RH só sobra
     * quando não há local visível. Null = tudo na lixeira.
     */
    fun escolherSobrevivente(grupo: List<Employee>, uidSummus: String): Employee? {
        val vivos = grupo.filter { !it.deleted }
        return vivos.firstOrNull { it.uid != uidSummus } ?: vivos.firstOrNull()
    }

    /**
     * Correção de ponto: sobrescreve horário/tipo/motivo e carimba a revisão aplicada. `deleted`
     * vira soft-delete (a linha fica; é o mesmo que a exclusão pela tela do app já faz).
     */
    fun mergePunch(local: Punch, c: SummusPunchCorrection): Punch =
            local.copy(
                    type = c.type,
                    timestamp = c.timestamp,
                    editedBy = c.editedBy,
                    editedAt = c.editedAt,
                    editReason = c.editReason,
                    deleted = c.deleted,
                    serverRevision = c.revision,
            )
}
