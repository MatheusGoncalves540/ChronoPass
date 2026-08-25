package com.chronopass.app.data.repo

import android.content.Context
import com.chronopass.app.data.database.ChronoDatabase
import com.chronopass.app.data.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ponytail: one repository over all DAOs; the app is small enough that
// four repositories would just be four passthrough files.
class ChronoRepository(context: Context) {
    private val db = ChronoDatabase.get(context)
    private val employees = db.employeeDao()
    private val punches = db.punchDao()
    private val stores = db.storeDao()
    private val settings = db.settingsDao()

    // Employees
    fun employeesFlow() = employees.all()
    fun employeesWithDeletedFlow() = employees.allIncludingDeleted()
    fun activeEmployeesFlow() = employees.activeList()
    suspend fun employee(id: Long) = employees.byId(id)
    suspend fun allEmployees() = withContext(Dispatchers.IO) { employees.allOnce() }
    suspend fun addEmployee(e: Employee) = withContext(Dispatchers.IO) { employees.insert(e) }
    suspend fun updateEmployee(e: Employee) = employees.update(e)
    // Soft delete: keeps the row (and never touches the employee's punches).
    suspend fun deleteEmployee(e: Employee) = withContext(Dispatchers.IO) { employees.softDelete(e.id) }

    // Punches
    suspend fun nextType(employeeId: Long): PunchType = withContext(Dispatchers.IO) {
        com.chronopass.app.data.PunchRules.next(punches.lastFor(employeeId)?.type)
    }
    suspend fun addPunch(p: Punch) = withContext(Dispatchers.IO) { punches.insert(p) }
    suspend fun updatePunch(p: Punch) = punches.update(p)
    suspend fun deletePunch(p: Punch) = withContext(Dispatchers.IO) { punches.softDelete(p.id) }
    fun punchesBetween(from: Long, to: Long) = punches.between(from, to)
    suspend fun forEmployeeBetween(id: Long, from: Long, to: Long) =
        withContext(Dispatchers.IO) { punches.forEmployeeBetween(id, from, to) }
    suspend fun allPunches() = withContext(Dispatchers.IO) { punches.allOnce() }
    suspend fun activePunches() = withContext(Dispatchers.IO) { punches.allActiveOnce() }

    // Trash (soft-deleted employees + punches). Empty-trash purges them for good.
    suspend fun trashCount(): Int = withContext(Dispatchers.IO) {
        employees.trashCount() + punches.trashCount()
    }
    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        punches.purgeDeleted()
        employees.purgeDeleted()
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
