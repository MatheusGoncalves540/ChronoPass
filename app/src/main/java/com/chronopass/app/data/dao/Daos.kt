package com.chronopass.app.data.dao

import androidx.room.*
import com.chronopass.app.data.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {
    // Management/name lists exclude soft-deleted employees.
    @Query("SELECT * FROM employee WHERE deleted = 0 ORDER BY name") fun all(): Flow<List<Employee>>

    @Query("SELECT * FROM employee WHERE active = 1 AND deleted = 0 ORDER BY name")
    fun activeList(): Flow<List<Employee>>

    // Records need names of deleted employees too (to show the "excluído" marker).
    @Query("SELECT * FROM employee ORDER BY name") fun allIncludingDeleted(): Flow<List<Employee>>

    @Query("SELECT * FROM employee WHERE id = :id") suspend fun byId(id: Long): Employee?

    // Descida do Summus: casa pelo uid (único desde a v5).
    @Query("SELECT * FROM employee WHERE uid = :uid")
    suspend fun employeeByUid(uid: String): Employee?

    @Query("SELECT * FROM employee") suspend fun allOnce(): List<Employee>

    @Query("SELECT COUNT(*) FROM employee WHERE deleted = 1") suspend fun trashCount(): Int

    @Query("UPDATE employee SET deleted = 1 WHERE id = :id") suspend fun softDelete(id: Long)

    @Query("DELETE FROM employee WHERE deleted = 1") suspend fun purgeDeleted()

    @Insert fun insert(e: Employee): Long
    @Update suspend fun update(e: Employee)
}

@Dao
interface PunchDao {
    @Query(
            "SELECT * FROM punch WHERE employeeId = :employeeId AND deleted = 0 ORDER BY timestamp DESC LIMIT 1"
    )
    suspend fun lastFor(employeeId: Long): Punch?

    @Query(
            "SELECT * FROM punch WHERE deleted = 0 AND timestamp BETWEEN :from AND :to ORDER BY timestamp DESC"
    )
    fun between(from: Long, to: Long): Flow<List<Punch>>

    @Query(
            "SELECT * FROM punch WHERE employeeId = :employeeId AND deleted = 0 AND timestamp BETWEEN :from AND :to ORDER BY timestamp"
    )
    suspend fun forEmployeeBetween(employeeId: Long, from: Long, to: Long): List<Punch>

    // Merge de funcionário duplicado (vínculo no Summus): sem FK no schema, o UPDATE é direto.
    @Query("UPDATE punch SET employeeId = :para WHERE employeeId = :de")
    suspend fun repointEmployee(de: Long, para: Long)

    // Backup keeps everything (deleted included) so restore is faithful.
    @Query("SELECT * FROM punch ORDER BY timestamp") suspend fun allOnce(): List<Punch>

    @Query("SELECT * FROM punch WHERE deleted = 0 ORDER BY timestamp")
    suspend fun allActiveOnce(): List<Punch>

    // Descida do Summus: casa a correção pelo uid (único desde a v5).
    @Query("SELECT * FROM punch WHERE uid = :uid") suspend fun punchByUid(uid: String): Punch?

    @Query("SELECT COUNT(*) FROM punch WHERE deleted = 1") suspend fun trashCount(): Int

    @Query("UPDATE punch SET deleted = 1 WHERE id = :id") suspend fun softDelete(id: Long)

    @Query("DELETE FROM punch WHERE deleted = 1") suspend fun purgeDeleted()

    @Insert fun insert(p: Punch): Long
    @Update suspend fun update(p: Punch)
}

@Dao
interface StoreDao {
    @Query("SELECT * FROM store LIMIT 1") fun get(): Flow<Store?>
    @Query("SELECT * FROM store LIMIT 1") suspend fun getOnce(): Store?
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insert(s: Store): Long
    @Update suspend fun update(s: Store)
}

@Dao
interface SettingsDao {
    @Query("SELECT value FROM app_settings WHERE key = :key") suspend fun get(key: String): String?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun set(setting: AppSetting)
    @Query("SELECT * FROM app_settings") suspend fun allOnce(): List<AppSetting>
}

// Fila de sync com o Summus (§7). Só acesso a dados; enfileirar/compactar/drenar é fase posterior.
@Dao
abstract class OutboxDao {
    @Insert abstract suspend fun insert(item: OutboxItem): Long

    @Query("DELETE FROM sync_outbox WHERE id = :id") abstract suspend fun delete(id: Long)

    // Re-enfileirar a mesma entidade ainda PENDING substitui o snapshot antigo (nunca 2 PENDING).
    @Query("DELETE FROM sync_outbox WHERE tipo = :tipo AND refUid = :refUid AND status = 'PENDING'")
    abstract suspend fun deletePending(tipo: String, refUid: String)

    @Query("DELETE FROM sync_outbox WHERE id IN (:ids)")
    abstract suspend fun deleteAll(ids: List<Long>)

    // lastAttemptAt é a base do backoff (v5): sem ela o intervalo media de createdAt e um item
    // velho voltava a cada sync.
    @Query(
            "UPDATE sync_outbox SET status='FAILED', tentativas=tentativas+1, ultimoErro=:erro, lastAttemptAt=:agora WHERE id IN (:ids)"
    )
    abstract suspend fun markFailed(ids: List<Long>, erro: String, agora: Long)

    // Lote 1 (metadados): funcionários + pontos pendentes ou falhos (backoff filtra em memória no
    // dreno).
    @Query(
            "SELECT * FROM sync_outbox WHERE status IN ('PENDING', 'FAILED') AND tipo IN ('EMPLOYEE', 'PUNCH') ORDER BY createdAt"
    )
    abstract suspend fun pendingMetadata(): List<OutboxItem>

    // Lote 2: fotos pendentes ou falhas (backoff filtra em memória no dreno).
    @Query(
            "SELECT * FROM sync_outbox WHERE status IN ('PENDING', 'FAILED') AND tipo = 'PHOTO' ORDER BY createdAt"
    )
    abstract suspend fun pendingPhotos(): List<OutboxItem>

    @Query(
            "UPDATE sync_outbox SET status = :status, tentativas = :tentativas, ultimoErro = :ultimoErro WHERE id = :id"
    )
    abstract suspend fun updateStatus(
            id: Long,
            status: String,
            tentativas: Int,
            ultimoErro: String?
    )

    // Enfileira o snapshot atual (tipo, refUid): 1) apaga PENDING duplicado do mesmo refUid,
    // 2) insere o novo. Tudo numa transação — nunca sobram 2 PENDING da mesma entidade.
    @Transaction
    open suspend fun enqueue(tipo: String, refUid: String, payload: String): Long {
        deletePending(tipo, refUid)
        return insert(OutboxItem(tipo = tipo, refUid = refUid, payload = payload))
    }
}
