package com.chronopass.app.data.repo

import android.content.Context
import com.chronopass.app.data.dao.*
import com.chronopass.app.data.database.ChronoDatabase
import com.chronopass.app.data.entities.*
import com.chronopass.app.sync.OutboxPayloads
import com.chronopass.app.sync.Pull
import com.chronopass.app.sync.SummusEmployee
import com.chronopass.app.sync.SummusPunchCorrection
import com.chronopass.app.sync.SyncRules
import com.chronopass.app.ui.SUMUS_PULL_SINCE_KEY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Hash da última foto de funcionário aplicada, em app_settings.
// ponytail: chave em app_settings em vez de coluna nova em employee — evita migration v6 para
// um dado que nem é do domínio (é estado de sync). Coluna quando alguma query precisar filtrar.
private fun photoHashKey(uid: String) = "summus_photo_hash.$uid"

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
    ) =
            withContext(Dispatchers.IO) {
                for (s in funcionarios) {
                    val local = employees.employeeByUid(s.uid)
                    val canonica =
                            when {
                                local != null ->
                                        SyncRules.mergeEmployee(local, s).also { employees.update(it) }
                                // Baixa desconhecida: nada a criar (não inventa linha na lixeira).
                                s.deleted -> continue
                                else ->
                                        SyncRules.novoEmployee(s).let {
                                            it.copy(id = employees.insert(it))
                                        }
                            }
                    absorver(canonica, s.mergeUids)
                }
                for (c in correcoes) {
                    // Ponto que este aparelho não tem (outro aparelho da loja): ignora.
                    val local = punches.punchByUid(c.uid) ?: continue
                    if (!SyncRules.aplicaRevisao(local.serverRevision, c.revision)) continue
                    punches.update(SyncRules.mergePunch(local, c))
                }
            }

    // Vínculo confirmado no Summus: as batidas da linha duplicada passam para a canônica e a
    // duplicada vai para a lixeira (nunca DELETE — vínculo errado continua recuperável).
    // Idempotente: `dup.deleted` corta a segunda passada (o merge não sobe, então o uid segue
    // vindo em mergeUids a cada pull).
    private suspend fun absorver(canonica: Employee, uids: List<String>) {
        var foto = canonica.photoPath
        for (uid in uids) {
            val dup = employees.employeeByUid(uid) ?: continue
            if (dup.id == canonica.id || dup.deleted) continue
            punches.repointEmployee(dup.id, canonica.id)
            if (foto == null) foto = dup.photoPath
            employees.softDelete(dup.id)
        }
        if (foto != canonica.photoPath) employees.update(canonica.copy(photoPath = foto))
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
                applyFromSummus(p.employees, p.correcoes)
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
                val local = employees.employeeByUid(uid) ?: return@withContext
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
