package com.chronopass.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.chronopass.app.data.entities.Employee
import com.chronopass.app.data.entities.Punch
import com.chronopass.app.data.entities.PunchType
import com.chronopass.app.reports.TimeUtil
import com.chronopass.app.ui.ChronoViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private enum class Period(val label: String) { TODAY("Hoje"), YESTERDAY("Ontem"), WEEK("7 dias"), ALL("Tudo") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(vm: ChronoViewModel, nav: NavController) {
    val activeEmployees by vm.allEmployees.collectAsState()
    // Resolve names for deleted employees too, and know which are deleted.
    val everyone by vm.employeesWithDeleted.collectAsState()
    val names = remember(everyone) { everyone.associate { it.id to it.name } }
    val deletedIds = remember(everyone) { everyone.filter { it.deleted }.map { it.id }.toSet() }
    var period by remember { mutableStateOf(Period.TODAY) }
    var employeeFilter by remember { mutableStateOf<Long?>(null) }
    var selected by remember { mutableStateOf<Punch?>(null) }
    var adding by remember { mutableStateOf(false) }

    val range = remember(period) {
        val now = System.currentTimeMillis()
        when (period) {
            Period.TODAY -> TimeUtil.startOfDay(now) to TimeUtil.endOfDay(now)
            Period.YESTERDAY -> TimeUtil.startOfDay(now - 86_400_000L) to TimeUtil.endOfDay(now - 86_400_000L)
            Period.WEEK -> TimeUtil.startOfDay(now - 6 * 86_400_000L) to TimeUtil.endOfDay(now)
            Period.ALL -> 0L to Long.MAX_VALUE
        }
    }
    val punches by remember(range) { vm.repo.punchesBetween(range.first, range.second) }
        .collectAsState(initial = emptyList())
    val shown = punches.filter { employeeFilter == null || it.employeeId == employeeFilter }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Marcações") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }) { Icon(Icons.Default.Add, "Adicionar marcação") }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(8.dp)) {
                Period.entries.forEach {
                    FilterChip(selected = period == it, onClick = { period = it }, label = { Text(it.label) })
                    Spacer(Modifier.width(6.dp))
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
                FilterChip(selected = employeeFilter == null, onClick = { employeeFilter = null }, label = { Text("Todos") })
                activeEmployees.forEach { e ->
                    Spacer(Modifier.width(6.dp))
                    FilterChip(selected = employeeFilter == e.id, onClick = { employeeFilter = e.id }, label = { Text(e.name) })
                }
            }
            HorizontalDivider()
            if (shown.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Nenhuma marcação no período.") }
            } else {
                LazyColumn {
                    items(shown, key = { it.id }) { p ->
                        val gone = p.employeeId in deletedIds || p.employeeId !in names
                        ListItem(
                            headlineContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(names[p.employeeId] ?: "Funcionário excluído")
                                    if (gone) {
                                        Spacer(Modifier.width(6.dp))
                                        Icon(Icons.Default.Warning, "Funcionário excluído",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            supportingContent = {
                                Column {
                                    Text("${if (p.type == PunchType.IN) "Entrada" else "Saída"} — ${TimeUtil.date(p.timestamp)} ${TimeUtil.time(p.timestamp)}")
                                    if (gone) Text("Funcionário não existe mais",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            },
                            trailingContent = { if (p.editedAt != null) AssistChip(onClick = {}, label = { Text("editado") }) },
                            modifier = Modifier.clickable { selected = p }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    selected?.let { p ->
        PunchDetailDialog(p, names[p.employeeId] ?: "?",
            onDismiss = { selected = null },
            onDelete = { vm.deletePunch(p); selected = null })
    }
    if (adding) {
        AddPunchDialog(activeEmployees, onDismiss = { adding = false }) { p -> vm.savePunch(p) { adding = false } }
    }
}

@Composable
private fun PunchDetailDialog(punch: Punch, name: String, onDismiss: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(name) },
        text = {
            Column {
                Text(if (punch.type == PunchType.IN) "Entrada" else "Saída")
                Text("${TimeUtil.date(punch.timestamp)} ${TimeUtil.time(punch.timestamp)}")
                Spacer(Modifier.height(8.dp))
                if (punch.latitude != null && punch.longitude != null) {
                    Text("Localização: %.5f, %.5f".format(punch.latitude, punch.longitude))
                    punch.accuracy?.let { Text("Precisão: ${it.toInt()} metros") }
                    TextButton(onClick = {
                        val uri = Uri.parse("geo:${punch.latitude},${punch.longitude}?q=${punch.latitude},${punch.longitude}")
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }) { Text("VER NO MAPA") }
                } else Text("Sem localização")
                punch.photoPath?.let { path ->
                    if (File(path).exists()) {
                        Spacer(Modifier.height(8.dp))
                        AsyncImage(model = File(path), contentDescription = "Foto",
                            modifier = Modifier.fillMaxWidth().height(220.dp))
                    }
                }
                punch.editReason?.let {
                    Spacer(Modifier.height(8.dp))
                    Text("Alterado por ${punch.editedBy}: $it", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDelete) { Text("EXCLUIR") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

@Composable
private fun AddPunchDialog(employees: List<Employee>, onDismiss: () -> Unit, onSave: (Punch) -> Unit) {
    var employeeId by remember { mutableStateOf(employees.firstOrNull()?.id) }
    var type by remember { mutableStateOf(PunchType.IN) }
    var dateTime by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())) }
    var reason by remember { mutableStateOf("") }
    var picking by remember { mutableStateOf(false) }
    val parser = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
    val parsed = runCatching { parser.parse(dateTime)?.time }.getOrNull()

    if (picking) {
        com.chronopass.app.ui.components.EmployeePickerDialog(
            title = "Selecionar funcionário",
            employees = employees,
            onDismiss = { picking = false },
            onPick = { employeeId = it.id; picking = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar marcação") },
        text = {
            Column {
                OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(employees.firstOrNull { it.id == employeeId }?.name ?: "Selecionar funcionário")
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    FilterChip(type == PunchType.IN, { type = PunchType.IN }, { Text("Entrada") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(type == PunchType.OUT, { type = PunchType.OUT }, { Text("Saída") })
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(dateTime, { dateTime = it }, label = { Text("Data e hora (dd/MM/aaaa HH:mm)") },
                    isError = parsed == null, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(reason, { reason = it }, label = { Text("Motivo da correção") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(
                enabled = employeeId != null && parsed != null && reason.isNotBlank(),
                onClick = {
                    onSave(Punch(
                        employeeId = employeeId!!, timestamp = parsed!!, type = type,
                        editedBy = "admin", editedAt = System.currentTimeMillis(), editReason = reason
                    ))
                }
            ) { Text("SALVAR") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
