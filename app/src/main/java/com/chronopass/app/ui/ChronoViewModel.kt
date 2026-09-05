package com.chronopass.app.ui

import android.app.Application
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chronopass.app.data.entities.*
import com.chronopass.app.data.repo.ChronoRepository
import com.chronopass.app.sync.SyncManager
import com.chronopass.app.sync.SyncOutcome
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val ADMIN_PASSWORD_KEY = "admin_password"
const val DEFAULT_ADMIN_PASSWORD = "1234"
const val SUMUS_URL_KEY = "summus_url"
const val SUMUS_API_KEY = "summus_api_key"

class ChronoViewModel(app: Application) : AndroidViewModel(app) {
    val repo = ChronoRepository(app)

    val activeEmployees: StateFlow<List<Employee>> =
            repo.activeEmployeesFlow()
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allEmployees: StateFlow<List<Employee>> =
            repo.employeesFlow()
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    // Includes soft-deleted employees, so Records can resolve names + show the "excluído" marker.
    val employeesWithDeleted: StateFlow<List<Employee>> =
            repo.employeesWithDeletedFlow()
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val store: StateFlow<Store?> =
            repo.storeFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Admin stays unlocked while navigating inside the admin area; HomeScreen relocks on exit.
    var adminUnlocked by mutableStateOf(false)

    suspend fun nextType(employeeId: Long): PunchType = repo.nextType(employeeId)

    fun savePunch(p: Punch, done: () -> Unit) =
            viewModelScope.launch {
                repo.addPunch(p)
                done()
                syncNow {} // fire-and-forget: ponto registrado -> drena já (não bloqueia a UI)
            }

    // Drena a fila de sync (SyncManager). onResult recebe a mensagem de status p/ a UI.
    fun syncNow(onResult: (String) -> Unit) =
            viewModelScope.launch {
                val o = SyncManager.sync(getApplication(), repo)
                val msg = mensagem(o)
                when (o) {
                    is SyncOutcome.Ok -> toast(msg) // sucesso: toast curto no rodapé
                    is SyncOutcome.Falha ->
                            toast(msg, longo = true) // erro: toast longo em destaque
                    else -> Unit // Inativo/Ocioso/JaRodando: silencioso no toast
                }
                onResult(msg)
            }

    private fun mensagem(o: SyncOutcome): String =
            when (o) {
                SyncOutcome.Inativo -> "Configure a URL e api-key do SummusBackoffice."
                SyncOutcome.Ocioso -> "Nada a sincronizar."
                SyncOutcome.JaRodando -> "Sincronização já em andamento."
                is SyncOutcome.Ok ->
                        "Sincronizado: ${o.funcionarios} funcionários, ${o.pontos} pontos, ${o.fotos} fotos."
                is SyncOutcome.Falha -> "Falha na sincronização: ${o.mensagem}"
            }

    // Toast nativo: feedback de sincronização visível no rodapé da tela (sucesso/falha).
    private fun toast(texto: String, longo: Boolean = false) {
        Toast.makeText(
                        getApplication(),
                        texto,
                        if (longo) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                )
                .show()
    }

    fun addEmployee(name: String, code: String, photoPath: String?) =
            viewModelScope.launch {
                repo.addEmployee(
                        Employee(name = name.trim(), code = code.trim(), photoPath = photoPath)
                )
            }
    fun updateEmployee(e: Employee) = viewModelScope.launch { repo.updateEmployee(e) }
    fun deleteEmployee(e: Employee) = viewModelScope.launch { repo.deleteEmployee(e) }

    fun updatePunch(p: Punch) = viewModelScope.launch { repo.updatePunch(p) }
    fun deletePunch(p: Punch) = viewModelScope.launch { repo.deletePunch(p) }

    fun saveStore(name: String, lat: Double, lon: Double, radius: Float) =
            viewModelScope.launch {
                val existing = repo.store()
                repo.saveStore(Store(existing?.id ?: 0, name, lat, lon, radius))
            }

    suspend fun adminPassword(): String = repo.setting(ADMIN_PASSWORD_KEY, DEFAULT_ADMIN_PASSWORD)
    fun setAdminPassword(pw: String) =
            viewModelScope.launch { repo.setSetting(ADMIN_PASSWORD_KEY, pw) }

    fun loadSumusConfig(result: (String, Boolean) -> Unit) =
            viewModelScope.launch {
                result(repo.setting(SUMUS_URL_KEY), repo.setting(SUMUS_API_KEY).isNotEmpty())
            }

    fun saveSumusConfig(url: String, apiKey: String) =
            viewModelScope.launch {
                repo.setSetting(SUMUS_URL_KEY, url.trim())
                repo.setSetting(SUMUS_API_KEY, apiKey.trim())
            }

    fun checkAdminPassword(input: String, result: (Boolean) -> Unit) =
            viewModelScope.launch { result(input == adminPassword()) }

    // Trash
    fun loadTrashCount(result: (Int) -> Unit) = viewModelScope.launch { result(repo.trashCount()) }
    fun emptyTrash(done: () -> Unit) =
            viewModelScope.launch {
                repo.emptyTrash()
                done()
            }
}
