package com.chronopass.app.data.repo

import android.content.Context
import com.chronopass.app.data.dao.*
import com.chronopass.app.data.database.ChronoDatabase
import com.chronopass.app.data.entities.*
import com.chronopass.app.sync.OutboxPayloads
import com.chronopass.app.sync.Pull
import com.chronopass.app.sync.SummusEmployee
import com.chronopass.app.sync.SummusNewPunch
import com.chronopass.app.sync.SummusPunchCorrection
import com.chronopass.app.sync.SyncRules
import com.chronopass.app.ui.SUMUS_PULL_SINCE_KEY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Hash da última foto de funcionário aplicada, em app_settings.
// ponytail: chave em app_settings em vez de coluna nova em employee — evita migration v6 para
// um dado que nem é do domínio (é estado de sync). Coluna quando alguma query precisar filtrar.
private fun photoHashKey(uid: String) = "summus_photo_hash.$uid"
private fun aliasKey(uidSummus: String) = "summus_alias.$uidSummus"

// ponytail: one repository over all DAOs; the app is small enough that
// four repositories would just be four passthrough files.
// Os DAOs entram pelo construtor (o de Context é o usado pelo app) para o teste JUnit puro de
// applyFromSummus poder passar fakes — nada de Robolectric.
class ChronoRepository
internal constructor(
        private val employees: EmployeeDao,
        private val punches: PunchDao,
        private val stores: StoreDao,
        private val settings: SettingsDao,
        private val outbox: OutboxDao,
) {
    constructor(context: Context) : this(ChronoDatabase.get(context))

    private constructor(
            db: ChronoDatabase
    ) : this(db.employeeDao(), db.punchDao(), db.storeDao(), db.settingsDao(), db.outboxDao())

    // Fila de sync (§7): snapshot da entidade no momento da escrita. Escrita + enqueue são
    // chamadas sequenciais (Room já transaciona cada operação; enqueue tem @Transaction próprio).
    private suspend fun enfileirar(tipo: String, uid: String, payloadJson: String) {
        outbox.enqueue(tipo, uid, payloadJson)
    }

    // Employees
    fun employeesFlow() = employees.all()
    fun employeesWithDeletedFlow() = employees.allIncludingDeleted()
    fun activeEmployeesFlow() = employees.activeList()
    suspend fun employee(id: Long) = employees.byId(id)
    suspend fun allEmployees() = withContext(Dispatchers.IO) { employees.allOnce() }
    suspend fun addEmployee(e: Employee) =
            withContext(Dispatchers.IO) {
                employees.insert(e)
                e.uid?.let {
                    enfileirar("EMPLOYEE", it, OutboxPayloads.employeeJson(e).toString())
                    enfileirarFoto(e)
                }
            }
    suspend fun updateEmployee(e: Employee) =
            withContext(Dispatchers.IO) {
                val anterior = employees.byId(e.id)
                employees.update(e)
                e.uid?.let {
                    enfileirar("EMPLOYEE", it, OutboxPayloads.employeeJson(e).toString())
                    // Só quando a foto mudou: editar o nome não reenvia a imagem.
                    if (e.photoPath != anterior?.photoPath) enfileirarFoto(e)
                }
            }

    // Foto de funcionário no lote 2 já existente, com a chave "employee.<uid>" documentada em
    // Entities.kt (o servidor roteia employee.* p/ rh_employees.photo).
    private suspend fun enfileirarFoto(e: Employee) {
        val uid = e.uid ?: return
        val caminho = e.photoPath ?: return
        val key = "employee.$uid"
        enfileirar("PHOTO", key, OutboxPayloads.photoJson(OutboxPayloads.PhotoRef(key, caminho)).toString())
    }
    // Soft delete: keeps the row (and never touches the employee's punches).
    suspend fun deleteEmployee(e: Employee) =
            withContext(Dispatchers.IO) {
                employees.softDelete(e.id)
                e.uid?.let {
                    enfileirar(
                            "EMPLOYEE",
                            it,
                            OutboxPayloads.employeeJson(e.copy(deleted = true)).toString()
                    )
                }
            }

    // Punches
    suspend fun nextType(employeeId: Long): PunchType =
            withContext(Dispatchers.IO) {
                com.chronopass.app.data.PunchRules.next(punches.lastFor(employeeId)?.type)
            }
    suspend fun addPunch(p: Punch) =
            withContext(Dispatchers.IO) {
                punches.insert(p)
                p.uid?.let { uid ->
                    enfileirar("PUNCH", uid, OutboxPayloads.punchJson(p).toString())
                    // Foto só na criação — update/delete não duplicam PHOTO (§7).
                    if (p.photoPath != null) {
                        enfileirar(
                                "PHOTO",
                                uid,
                                OutboxPayloads.photoJson(
                                                OutboxPayloads.PhotoRef(uid, p.photoPath)
                                        )
                                        .toString()
                        )
                    }
                }
            }
    suspend fun updatePunch(p: Punch) =
            withContext(Dispatchers.IO) {
                punches.update(p)
                p.uid?.let { enfileirar("PUNCH", it, OutboxPayloads.punchJson(p).toString()) }
            }
    suspend fun deletePunch(p: Punch) =
            withContext(Dispatchers.IO) {
                punches.softDelete(p.id)
                p.uid?.let {
                    enfileirar(
                            "PUNCH",
                            it,
                            OutboxPayloads.punchJson(p.copy(deleted = true)).toString()
                    )
                }
            }
    fun punchesBetween(from: Long, to: Long) = punches.between(from, to)
    suspend fun forEmployeeBetween(id: Long, from: Long, to: Long) =
            withContext(Dispatchers.IO) { punches.forEmployeeBetween(id, from, to) }
    suspend fun allPunches() = withContext(Dispatchers.IO) { punches.allOnce() }
    suspend fun activePunches() = withContext(Dispatchers.IO) { punches.allActiveOnce() }

    // Trash (soft-deleted employees + punches). Empty-trash purges them for good.
    suspend fun trashCount(): Int =
            withContext(Dispatchers.IO) { employees.trashCount() + punches.trashCount() }
    suspend fun emptyTrash() =
            withContext(Dispatchers.IO) {
                punches.purgeDeleted()
                employees.purgeDeleted()
            }

    // --- Descida do Summus (plano Fase 3): seam anti-eco ---
    // Escreve nos DAOs SEM chamar enfileirar(): aplicar aqui uma mudança que veio do servidor
    // não a devolve para cima. Todo caminho de descida passa por aqui, e só por aqui.
    // Nada é apagado: funcionário some -> desativa; correção de ponto -> update na mesma linha.
    suspend fun applyFromSummus(
            funcionarios: List<SummusEmployee> = emptyList(),
            correcoes: List<SummusPunchCorrection> = emptyList(),
            novasBatidas: List<SummusNewPunch> = emptyList(),
    ) =
            withContext(Dispatchers.IO) {
                for (s in funcionarios) {
                    val porUid = employees.employeeByUid(s.uid)
                    // Duplicatas que o vínculo declarou serem a mesma pessoa (cadastro local do app).
                    val dups =
                            s.mergeUids
                                    .mapNotNull { employees.employeeByUid(it) }
                                    .filter { it.id != porUid?.id }
                                    .distinctBy { it.id }
                    val grupo = listOfNotNull(porUid) + dups
                    if (grupo.isEmpty()) {
                        // Baixa desconhecida: nada a criar (não inventa linha na lixeira).
                        if (!s.deleted) employees.insert(SyncRules.novoEmployee(s))
                        continue
                    }
                    val sobrevivente = SyncRules.escolherSobrevivente(grupo, s.uid)
                    if (sobrevivente == null) {
                        // Tudo na lixeira: a lixeira é escolha do usuário — não ressuscita nem cria
                        // linha nova. (Comportamento de sempre para a linha casada por uid.)
                        porUid?.let { employees.update(SyncRules.mergeEmployee(it, s)) }
                        continue
                    }
                    val atual = SyncRules.mergeEmployee(sobrevivente, s)
                    employees.update(atual)
                    absorver(atual, grupo.filter { it.id != sobrevivente.id })
                    registrarAlias(s.uid, atual.uid)
                }
                // Batidas criadas no backoffice: DEPOIS do cadastro (o dono precisa existir) e
                // ANTES das correções (uma correção da mesma batida no mesmo pull cai na guarda de
                // revisão). Idempotente pelo uid.
                for (n in novasBatidas) inserirBatidaDoBackoffice(n)
                for (c in correcoes) {
                    // Ponto que este aparelho não tem (outro aparelho da loja): ignora.
                    val local = punches.punchByUid(c.uid) ?: continue
                    if (!SyncRules.aplicaRevisao(local.serverRevision, c.revision)) continue
                    punches.update(SyncRules.mergePunch(local, c))
                }
            }

    // Batida lançada no backoffice: entra na tela do aparelho como qualquer outra (a próxima
    // Entrada/Saída passa a contar com ela). Dono = primeiro candidato com linha VISÍVEL; se todas
    // estão na lixeira, a primeira que existir (não perde a batida); sem linha nenhuma o aparelho
    // não conhece a pessoa — ignora (o servidor só aceita funcionário da loja). Não enfileira
    // (seam anti-eco): o Summus já a tem.
    private suspend fun inserirBatidaDoBackoffice(n: SummusNewPunch) {
        if (punches.punchByUid(n.uid) != null) return
        val donos = n.employeeUids.mapNotNull { employees.employeeByUid(it) }
        val dono = donos.firstOrNull { !it.deleted } ?: donos.firstOrNull() ?: return
        punches.insert(
                Punch(
                        uid = n.uid,
                        employeeId = dono.id,
                        timestamp = n.timestamp,
                        type = n.type,
                        editedBy = n.editedBy,
                        editedAt = n.editedAt,
                        editReason = n.editReason,
                        deleted = n.deleted,
                        serverRevision = n.revision,
                )
        )
    }

    // Vínculo confirmado no Summus: as batidas das linhas duplicadas passam para o SOBREVIVENTE e as
    // duplicadas ainda ativas vão para a lixeira (nunca DELETE — vínculo errado continua
    // recuperável). O sobrevivente é sempre uma linha VISÍVEL (SyncRules.escolherSobrevivente), então
    // absorver nunca esconde o funcionário. Idempotente: repetir não muda nada (o merge não sobe —
    // seam anti-eco — e o uid segue vindo em mergeUids a cada pull).
    private suspend fun absorver(sobrevivente: Employee, outras: List<Employee>) {
        var foto = sobrevivente.photoPath
        for (dup in outras) {
            punches.repointEmployee(dup.id, sobrevivente.id)
            if (foto == null) foto = dup.photoPath
            if (!dup.deleted) employees.softDelete(dup.id)
        }
        if (foto != sobrevivente.photoPath) employees.update(sobrevivente.copy(photoPath = foto))
    }

    // A foto/hash do Summus vêm pelo uid do RH; se quem sobrou é o cadastro local (outro uid), o
    // alias diz onde aplicar a foto. Só grava quando muda (não gera escrita a cada pull).
    private suspend fun registrarAlias(uidSummus: String, uidLocal: String?) {
        if (uidLocal == null || uidLocal == uidSummus) return
        if (settings.get(aliasKey(uidSummus)) != uidLocal)
                settings.set(AppSetting(aliasKey(uidSummus), uidLocal))
    }

    /**
     * Aplica um pull inteiro e SÓ ENTÃO avança o cursor.
     *
     * A ordem é a garantia: se qualquer escrita estourar no meio, o cursor fica onde estava e o
     * servidor reenvia a mesma janela no próximo pull. Reprocessar é idempotente (guarda de
     * revisão); perder uma correção não é.
     */
    suspend fun applyPull(p: Pull) =
            withContext(Dispatchers.IO) {
                applyFromSummus(p.employees, p.correcoes, p.novasBatidas)
                p.store?.let { s ->
                    val atual = stores.getOnce()
                    // insert é REPLACE: mantém o id da linha única de loja em vez de criar outra.
                    stores.insert(
                            Store(
                                    id = atual?.id ?: 0,
                                    name = s.name,
                                    latitude = s.latitude,
                                    longitude = s.longitude,
                                    radius = s.radiusMeters.toFloat(),
                                    uid = s.uid,
                                    managedBySummus = true,
                            )
                    )
                }
                settings.set(AppSetting(SUMUS_PULL_SINCE_KEY, p.serverTime))
            }

    /** Último photoHash aplicado a este funcionário (null = nunca baixou foto do Summus). */
    suspend fun photoHashSummus(uid: String): String? =
            withContext(Dispatchers.IO) { settings.get(photoHashKey(uid)) }

    /**
     * Descida da foto de funcionário: grava o caminho e carimba o hash. Como applyFromSummus, NÃO
     * enfileira — a imagem veio de cima, devolvê-la seria o eco.
     */
    suspend fun applyEmployeePhotoFromSummus(uid: String, photoPath: String, hash: String) =
            withContext(Dispatchers.IO) {
                val alvo = settings.get(aliasKey(uid)) ?: uid
                val local =
                        employees.employeeByUid(alvo)
                                ?: employees.employeeByUid(uid)
                                ?: return@withContext
                employees.update(local.copy(photoPath = photoPath))
                settings.set(AppSetting(photoHashKey(uid), hash))
            }

    // Store
    fun storeFlow() = stores.get()
    suspend fun store() = withContext(Dispatchers.IO) { stores.getOnce() }
    suspend fun saveStore(s: Store) = withContext(Dispatchers.IO) { stores.insert(s) }

    // Settings
    suspend fun setting(key: String, default: String = "") =
            withContext(Dispatchers.IO) { settings.get(key) ?: default }
    suspend fun setSetting(key: String, value: String) =
            withContext(Dispatchers.IO) { settings.set(AppSetting(key, value)) }
    suspend fun allSettings() = withContext(Dispatchers.IO) { settings.allOnce() }
}
