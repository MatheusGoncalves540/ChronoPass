package com.chronopass.app.data.dao

import androidx.room.*
import com.chronopass.app.data.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employee ORDER BY name")
    fun all(): Flow<List<Employee>>

    @Query("SELECT * FROM employee WHERE active = 1 ORDER BY name")
    fun activeList(): Flow<List<Employee>>

    @Query("SELECT * FROM employee WHERE id = :id")
    suspend fun byId(id: Long): Employee?

    @Query("SELECT * FROM employee")
    suspend fun allOnce(): List<Employee>

    @Insert fun insert(e: Employee): Long
    @Update suspend fun update(e: Employee)
    @Delete suspend fun delete(e: Employee)
}

@Dao
interface PunchDao {
    @Query("SELECT * FROM punch WHERE employeeId = :employeeId ORDER BY timestamp DESC LIMIT 1")
    suspend fun lastFor(employeeId: Long): Punch?

    @Query("SELECT * FROM punch WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp DESC")
    fun between(from: Long, to: Long): Flow<List<Punch>>

    @Query("SELECT * FROM punch WHERE employeeId = :employeeId AND timestamp BETWEEN :from AND :to ORDER BY timestamp")
    suspend fun forEmployeeBetween(employeeId: Long, from: Long, to: Long): List<Punch>

    @Query("SELECT * FROM punch ORDER BY timestamp")
    suspend fun allOnce(): List<Punch>

    @Insert fun insert(p: Punch): Long
    @Update suspend fun update(p: Punch)
    @Delete suspend fun delete(p: Punch)
    @Query("DELETE FROM punch WHERE employeeId = :employeeId")
    suspend fun deleteForEmployee(employeeId: Long)
}

@Dao
interface StoreDao {
    @Query("SELECT * FROM store LIMIT 1")
    fun get(): Flow<Store?>
    @Query("SELECT * FROM store LIMIT 1")
    suspend fun getOnce(): Store?
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insert(s: Store): Long
    @Update suspend fun update(s: Store)
}

@Dao
interface SettingsDao {
    @Query("SELECT value FROM app_settings WHERE key = :key")
    suspend fun get(key: String): String?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(setting: AppSetting)
    @Query("SELECT * FROM app_settings")
    suspend fun allOnce(): List<AppSetting>
}
