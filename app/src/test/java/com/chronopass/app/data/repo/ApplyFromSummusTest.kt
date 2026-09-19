package com.chronopass.app.data.repo

import com.chronopass.app.data.dao.EmployeeDao
import com.chronopass.app.data.dao.OutboxDao
import com.chronopass.app.data.dao.PunchDao
import com.chronopass.app.data.dao.SettingsDao
import com.chronopass.app.data.dao.StoreDao
import com.chronopass.app.data.entities.*
import com.chronopass.app.sync.Pull
import com.chronopass.app.sync.StorePull
import com.chronopass.app.sync.SummusEmployee
import com.chronopass.app.sync.SummusPunchCorrection
import com.chronopass.app.ui.SUMUS_PULL_SINCE_KEY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fase 3: o seam anti-eco. Fakes de DAO em JUnit puro (sem Room/Robolectric) — o que importa aqui é
 * que a descida NÃO toca a outbox, senão o servidor recebe de volta o que acabou de mandar.
 */
class ApplyFromSummusTest {

    @Test
    fun applyFromSummus_naoEnfileiraNaOutbox() = runBlocking {
        val emp = FakeEmployeeDao(mutableListOf(Employee(id = 1, uid = "e-1", name = "Ana")))
        val pun = FakePunchDao(mutableListOf(punch(uid = "p-1", revisao = 1)))
        val out = FakeOutboxDao()
        val repo = ChronoRepository(emp, pun, FakeStoreDao(), FakeSettingsDao(), out)

        repo.applyFromSummus(
                funcionarios =
                        listOf(
                                SummusEmployee(uid = "e-1", name = "Ana Maria", role = "Caixa"),
                                SummusEmployee(uid = "e-2", name = "Novo"),
                        ),
                correcoes = listOf(correcao(uid = "p-1", revisao = 2)),
        )

        assertTrue("descida não pode enfileirar nada", out.itens.isEmpty())
        // ...e escreveu de fato (senão o teste passaria por não fazer nada).
        assertEquals("Ana Maria", emp.rows.first { it.uid == "e-1" }.name)
        assertEquals(ORIGIN_SUMMUS, emp.rows.first { it.uid == "e-1" }.origin)
        assertEquals(ORIGIN_SUMMUS, emp.rows.first { it.uid == "e-2" }.origin)
        assertEquals(2, pun.rows.first().serverRevision)
    }

    @Test
    fun cadastroLocalContinuaSubindo() = runBlocking {
        val emp = FakeEmployeeDao(mutableListOf())
        val out = FakeOutboxDao()
        val repo = ChronoRepository(emp, FakePunchDao(), FakeStoreDao(), FakeSettingsDao(), out)

        repo.addEmployee(Employee(uid = "local-1", name = "Offline", photoPath = "/x/emp.webp"))

        assertEquals(ORIGIN_LOCAL, emp.rows.single().origin)
        // EMPLOYEE + a foto no lote 2 com a chave "employee.<uid>".
        assertEquals(listOf("EMPLOYEE", "PHOTO"), out.itens.map { it.first })
        assertEquals("employee.local-1", out.itens.last().second)
    }

    @Test
    fun revisaoMenorOuIgualNaoAplica() = runBlocking {
        val pun = FakePunchDao(mutableListOf(punch(uid = "p-1", revisao = 3)))
        val repo =
                ChronoRepository(
                        FakeEmployeeDao(),
                        pun,
                        FakeStoreDao(),
                        FakeSettingsDao(),
                        FakeOutboxDao()
                )

        repo.applyFromSummus(correcoes = listOf(correcao(uid = "p-1", revisao = 3)))
        assertEquals(0, pun.updates)
        assertEquals(8 * 3_600_000L, pun.rows.single().timestamp)

        repo.applyFromSummus(correcoes = listOf(correcao(uid = "p-1", revisao = 2)))
        assertEquals(0, pun.updates)

        repo.applyFromSummus(correcoes = listOf(correcao(uid = "p-1", revisao = 4)))
        assertEquals(1, pun.updates)
        assertEquals(9 * 3_600_000L, pun.rows.single().timestamp)
    }

    @Test
    fun desativaNuncaApaga() = runBlocking {
        val emp = FakeEmployeeDao(mutableListOf(Employee(id = 1, uid = "e-1", name = "Ana")))
        val repo =
                ChronoRepository(
                        emp,
                        FakePunchDao(),
                        FakeStoreDao(),
                        FakeSettingsDao(),
                        FakeOutboxDao()
                )

        repo.applyFromSummus(
                funcionarios = listOf(SummusEmployee(uid = "e-1", name = "Ana", deleted = true))
        )

        val row = emp.rows.single()
        assertFalse(row.active)
        assertFalse("deleted local é lixeira purgável — servidor não liga", row.deleted)
    }

    @Test
    fun applyPullAvancaCursorEMarcaLojaGerida() = runBlocking {
        val sto = FakeStoreDao()
        val cfg = FakeSettingsDao()
        val repo =
                ChronoRepository(
                        FakeEmployeeDao(),
                        FakePunchDao(),
                        sto,
                        cfg,
                        FakeOutboxDao(),
                )

        repo.applyPull(
                Pull(
                        serverTime = "2026-09-05T14:03:12.000Z",
                        store = StorePull("s-1", "Loja Centro", -23.5, -46.6, 150.0),
                        employees = listOf(SummusEmployee(uid = "e-1", name = "Ana")),
                        correcoes = emptyList(),
                )
        )

        assertEquals("2026-09-05T14:03:12.000Z", cfg.get(SUMUS_PULL_SINCE_KEY))
        assertEquals("s-1", sto.row?.uid)
        assertTrue(sto.row!!.managedBySummus)
        assertEquals(150f, sto.row!!.radius, 0.001f)
    }

    @Test
    fun cursorNaoAvancaQuandoAplicacaoFalha() = runBlocking {
        val pun = FakePunchDao(mutableListOf(punch(uid = "p-1", revisao = 1)))
        pun.falhaNoUpdate = true // ex.: banco indisponível no meio da aplicação
        val cfg = FakeSettingsDao()
        cfg.set(AppSetting(SUMUS_PULL_SINCE_KEY, "2026-09-01T00:00:00.000Z"))
        val repo =
                ChronoRepository(FakeEmployeeDao(), pun, FakeStoreDao(), cfg, FakeOutboxDao())

        val erro =
                runCatching {
                            repo.applyPull(
                                    Pull(
                                            serverTime = "2026-09-05T14:03:12.000Z",
                                            store = null,
                                            employees = emptyList(),
                                            correcoes = listOf(correcao(uid = "p-1", revisao = 2)),
                                    )
                            )
                        }
                        .exceptionOrNull()

        assertTrue("a falha tem de propagar", erro != null)
        // Cursor parado: o servidor reenvia a mesma janela. Perder correção é irreversível.
        assertEquals("2026-09-01T00:00:00.000Z", cfg.get(SUMUS_PULL_SINCE_KEY))
    }

    @Test
    fun guardaDeRevisaoValeNoCaminhoDoPull() = runBlocking {
        val pun = FakePunchDao(mutableListOf(punch(uid = "p-1", revisao = 5)))
        val cfg = FakeSettingsDao()
        val repo =
                ChronoRepository(FakeEmployeeDao(), pun, FakeStoreDao(), cfg, FakeOutboxDao())

        repo.applyPull(
                Pull(
                        serverTime = "2026-09-05T14:03:12.000Z",
                        store = null,
                        employees = emptyList(),
                        correcoes = listOf(correcao(uid = "p-1", revisao = 5)),
                )
        )

        assertEquals(0, pun.updates)
        assertEquals(8 * 3_600_000L, pun.rows.single().timestamp)
        // Revisão velha não aplica, mas o pull foi processado: o cursor anda.
        assertEquals("2026-09-05T14:03:12.000Z", cfg.get(SUMUS_PULL_SINCE_KEY))
    }

    @Test
    fun fotoDaDescidaNaoEnfileira() = runBlocking {
        val emp = FakeEmployeeDao(mutableListOf(Employee(id = 1, uid = "e-1", name = "Ana")))
        val cfg = FakeSettingsDao()
        val out = FakeOutboxDao()
        val repo = ChronoRepository(emp, FakePunchDao(), FakeStoreDao(), cfg, out)

        assertEquals(null, repo.photoHashSummus("e-1"))
        repo.applyEmployeePhotoFromSummus("e-1", "/files/employees/summus_e-1.webp", "hash-1")

        assertEquals("/files/employees/summus_e-1.webp", emp.rows.single().photoPath)
        assertEquals("hash-1", repo.photoHashSummus("e-1"))
        assertTrue("foto que veio de cima não pode voltar", out.itens.isEmpty())
    }

    @Test
    fun mergeUids_moveBatidasParaCanonicaEMandaDuplicataParaLixeira() = runBlocking {
        val emp =
                FakeEmployeeDao(
                        mutableListOf(
                                Employee(id = 1, uid = "local-1", name = "João", photoPath = "/f/j.webp"),
                                Employee(id = 2, uid = "rh-1", name = "João"),
                        )
                )
        val pun =
                FakePunchDao(
                        mutableListOf(
                                punch(uid = "p-1", revisao = 0, employeeId = 1),
                                punch(uid = "p-2", revisao = 0, employeeId = 2),
                        )
                )
        val out = FakeOutboxDao()
        val repo = ChronoRepository(emp, pun, FakeStoreDao(), FakeSettingsDao(), out)
        val pull = listOf(SummusEmployee(uid = "rh-1", name = "João", mergeUids = listOf("local-1")))

        repo.applyFromSummus(funcionarios = pull)

        assertEquals(listOf(2L, 2L), pun.rows.map { it.employeeId })
        assertTrue(emp.rows.first { it.uid == "local-1" }.deleted)
        assertFalse(emp.rows.first { it.uid == "rh-1" }.deleted)
        // Foto de cadastro da local herdada: a canônica não tinha.
        assertEquals("/f/j.webp", emp.rows.first { it.uid == "rh-1" }.photoPath)
        assertTrue("merge não sobe (anti-eco)", out.itens.isEmpty())

        // Segunda aplicação do mesmo pull: nada muda (idempotente).
        val antes = pun.repoints
        repo.applyFromSummus(funcionarios = pull)
        assertEquals(antes, pun.repoints)
        assertEquals(listOf(2L, 2L), pun.rows.map { it.employeeId })
    }

    @Test
    fun mergeUids_canonicaNovaEhCriadaEDuplicataAbsorvidaNaMesmaPassada() = runBlocking {
        val emp = FakeEmployeeDao(mutableListOf(Employee(id = 1, uid = "local-1", name = "João")))
        val pun = FakePunchDao(mutableListOf(punch(uid = "p-1", revisao = 0, employeeId = 1)))
        val repo = ChronoRepository(emp, pun, FakeStoreDao(), FakeSettingsDao(), FakeOutboxDao())

        repo.applyFromSummus(
                funcionarios =
                        listOf(SummusEmployee(uid = "rh-1", name = "João", mergeUids = listOf("local-1")))
        )

        val canonica = emp.rows.first { it.uid == "rh-1" }
        assertEquals(canonica.id, pun.rows.single().employeeId)
        assertTrue(emp.rows.first { it.uid == "local-1" }.deleted)
    }

    @Test
    fun mergeUids_uidDesconhecidoOuIgualACanonicaEhIgnorado() = runBlocking {
        val emp = FakeEmployeeDao(mutableListOf(Employee(id = 1, uid = "rh-1", name = "João")))
        val pun = FakePunchDao(mutableListOf(punch(uid = "p-1", revisao = 0, employeeId = 1)))
        val repo = ChronoRepository(emp, pun, FakeStoreDao(), FakeSettingsDao(), FakeOutboxDao())

        repo.applyFromSummus(
                funcionarios =
                        listOf(
                                SummusEmployee(
                                        uid = "rh-1",
                                        name = "João",
                                        mergeUids = listOf("rh-1", "de-outro-aparelho"),
                                )
                        )
        )

        assertEquals(0, pun.repoints)
        assertFalse(emp.rows.single().deleted)
    }

    // --- helpers ---

    private fun punch(uid: String, revisao: Int, employeeId: Long = 1) =
            Punch(
                    id = 7,
                    uid = uid,
                    employeeId = employeeId,
                    timestamp = 8 * 3_600_000L,
                    type = PunchType.IN,
                    serverRevision = revisao,
            )

    private fun correcao(uid: String, revisao: Int) =
            SummusPunchCorrection(
                    uid = uid,
                    type = PunchType.IN,
                    timestamp = 9 * 3_600_000L,
                    editedBy = "rh",
                    editedAt = 10 * 3_600_000L,
                    editReason = "esqueceu de bater",
                    revision = revisao,
            )
}

// --- fakes: só o que o seam usa; o resto não é chamado nestes testes ---

private class FakeEmployeeDao(val rows: MutableList<Employee> = mutableListOf()) : EmployeeDao {
    override suspend fun employeeByUid(uid: String) = rows.firstOrNull { it.uid == uid }
    override suspend fun byId(id: Long) = rows.firstOrNull { it.id == id }
    override fun insert(e: Employee): Long {
        val id = if (e.id != 0L) e.id else (rows.maxOfOrNull { it.id } ?: 0L) + 1
        rows += e.copy(id = id)
        return id
    }
    override suspend fun update(e: Employee) {
        val i = rows.indexOfFirst { it.uid == e.uid }
        if (i >= 0) rows[i] = e else rows += e
    }
    override fun all(): Flow<List<Employee>> = flowOf(rows)
    override fun activeList(): Flow<List<Employee>> = flowOf(rows)
    override fun allIncludingDeleted(): Flow<List<Employee>> = flowOf(rows)
    override suspend fun allOnce() = rows.toList()
    override suspend fun trashCount() = 0
    override suspend fun softDelete(id: Long) {
        val i = rows.indexOfFirst { it.id == id }
        if (i >= 0) rows[i] = rows[i].copy(deleted = true)
    }
    override suspend fun purgeDeleted() = Unit
}

private class FakePunchDao(val rows: MutableList<Punch> = mutableListOf()) : PunchDao {
    var updates = 0
    var repoints = 0
    var falhaNoUpdate = false
    override suspend fun repointEmployee(de: Long, para: Long) {
        repoints++
        rows.indices.forEach { if (rows[it].employeeId == de) rows[it] = rows[it].copy(employeeId = para) }
    }
    override suspend fun punchByUid(uid: String) = rows.firstOrNull { it.uid == uid }
    override suspend fun update(p: Punch) {
        if (falhaNoUpdate) throw IllegalStateException("banco indisponível")
        updates++
        val i = rows.indexOfFirst { it.uid == p.uid }
        if (i >= 0) rows[i] = p
    }
    override fun insert(p: Punch): Long {
        rows += p
        return rows.size.toLong()
    }
    override suspend fun lastFor(employeeId: Long) = rows.lastOrNull()
    override fun between(from: Long, to: Long): Flow<List<Punch>> = flowOf(rows)
    override suspend fun forEmployeeBetween(employeeId: Long, from: Long, to: Long) = rows.toList()
    override suspend fun allOnce() = rows.toList()
    override suspend fun allActiveOnce() = rows.toList()
    override suspend fun trashCount() = 0
    override suspend fun softDelete(id: Long) = Unit
    override suspend fun purgeDeleted() = Unit
}

private class FakeStoreDao : StoreDao {
    var row: Store? = null
    override fun get(): Flow<Store?> = flowOf(row)
    override suspend fun getOnce(): Store? = row
    override fun insert(s: Store): Long {
        row = s // REPLACE, como o DAO real
        return 1L
    }
    override suspend fun update(s: Store) {
        row = s
    }
}

private class FakeSettingsDao : SettingsDao {
    private val map = mutableMapOf<String, String>()
    override suspend fun get(key: String) = map[key]
    override suspend fun set(setting: AppSetting) {
        map[setting.key] = setting.value
    }
    override suspend fun allOnce() = map.map { AppSetting(it.key, it.value) }
}

/** Registra (tipo, refUid) de tudo que for enfileirado — a descida tem de deixar isto vazio. */
private class FakeOutboxDao : OutboxDao() {
    val itens = mutableListOf<Pair<String, String?>>()
    override suspend fun insert(item: OutboxItem): Long {
        itens += item.tipo to item.refUid
        return itens.size.toLong()
    }
    override suspend fun delete(id: Long) = Unit
    override suspend fun deletePending(tipo: String, refUid: String) = Unit
    override suspend fun deleteAll(ids: List<Long>) = Unit
    override suspend fun markFailed(ids: List<Long>, erro: String, agora: Long) = Unit
    override suspend fun pendingMetadata(): List<OutboxItem> = emptyList()
    override suspend fun pendingPhotos(): List<OutboxItem> = emptyList()
    override suspend fun updateStatus(
            id: Long,
            status: String,
            tentativas: Int,
            ultimoErro: String?
    ) = Unit
}
