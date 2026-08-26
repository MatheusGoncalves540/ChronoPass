package com.chronopass.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.chronopass.app.backup.BackupManager
import com.chronopass.app.data.entities.Employee
import com.chronopass.app.data.entities.Punch
import com.chronopass.app.reports.CsvExport
import com.chronopass.app.reports.PdfExport
import com.chronopass.app.reports.ReportPeriod
import com.chronopass.app.reports.TimeUtil
import com.chronopass.app.ui.ChronoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(vm: ChronoViewModel, nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val employees by vm.allEmployees.collectAsState()
    val everyone by vm.employeesWithDeleted.collectAsState()
    var status by remember { mutableStateOf<String?>(null) }
    var pdfPicker by remember { mutableStateOf(false) }

    // Filtros (período + funcionário) valem para o CSV e para o PDF.
    var period by remember { mutableStateOf(ReportPeriod.THIS_MONTH) }
    var customRange by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    var employeeFilter by remember { mutableStateOf<Long?>(null) }
    var datePicker by remember { mutableStateOf(false) }

    val (from, to) = remember(period, customRange) {
        if (period == ReportPeriod.CUSTOM) customRange ?: period.range() else period.range()
    }
    val periodLabel = "${TimeUtil.date(from)} - ${TimeUtil.date(to)}"

    val punches by remember(from, to) { vm.repo.punchesBetween(from, to) }
        .collectAsState(initial = emptyList())
    val shown = punches.filter { employeeFilter == null || it.employeeId == employeeFilter }
    val selectedEmployee = employees.firstOrNull { it.id == employeeFilter }

    fun exportsDir() = File(context.cacheDir, "exports").apply { mkdirs() }
    fun share(file: File, mime: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar"))
    }

    fun exportPdf(e: Employee) {
        scope.launch {
            val rows: List<Punch> = vm.repo.forEmployeeBetween(e.id, from, to)
            val file = File(exportsDir(), "espelho_${slug(e.name)}_${TimeUtil.fileDate(from)}.pdf")
            val logo = com.chronopass.app.ui.components.LogoAsset.bitmap(context)
            withContext(Dispatchers.IO) { PdfExport.write(file, e.name, periodLabel, rows, logo) }
            share(file, "application/pdf")
        }
    }

    // Backup: write a zip to a location the user chooses (SAF)
    val backupSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) scope.launch {
            val tmp = File(exportsDir(), "backup.zip")
            withContext(Dispatchers.IO) {
                BackupManager.export(context, vm.repo, tmp)
                context.contentResolver.openOutputStream(uri)?.use { out -> tmp.inputStream().use { it.copyTo(out) } }
            }
            status = "Backup salvo."
        }
    }
    val backupRestorer = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) scope.launch {
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { BackupManager.import(context, it) }
            }
            status = "Backup restaurado. Reinicie o aplicativo."
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Relatórios e backup") }) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Período", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                ReportPeriod.entries.forEach { p ->
                    FilterChip(
                        selected = period == p,
                        onClick = { if (p == ReportPeriod.CUSTOM) datePicker = true else period = p },
                        label = { Text(p.label) }
                    )
                    Spacer(Modifier.width(6.dp))
                }
            }

            Text("Funcionário", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                FilterChip(employeeFilter == null, { employeeFilter = null }, { Text("Todos") })
                employees.forEach { e ->
                    Spacer(Modifier.width(6.dp))
                    FilterChip(employeeFilter == e.id, { employeeFilter = e.id }, { Text(e.name) })
                }
            }

            Text(
                "$periodLabel  •  ${shown.size} marcações",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()

            Button(
                onClick = {
                    if (shown.isEmpty()) {
                        status = "Nenhuma marcação no período."
                    } else scope.launch {
                        val names = everyone.associate { it.id to it.name }
                        val who = selectedEmployee?.let { slug(it.name) } ?: "todos"
                        val file = File(
                            exportsDir(),
                            "chronopass_${who}_${TimeUtil.fileDate(from)}_${TimeUtil.fileDate(to)}.csv"
                        )
                        val rows = shown.sortedBy { it.timestamp }
                        withContext(Dispatchers.IO) { CsvExport.write(file, rows, names) }
                        share(file, "text/csv")
                    }
                },
                Modifier.fillMaxWidth()
            ) { Text("EXPORTAR CSV") }

            Button(
                onClick = { selectedEmployee.let { if (it != null) exportPdf(it) else pdfPicker = true } },
                Modifier.fillMaxWidth()
            ) {
                Text(selectedEmployee?.let { "GERAR PDF — ${it.name}" } ?: "GERAR PDF (por funcionário)")
            }

            HorizontalDivider()

            Button(onClick = { backupSaver.launch("chronopass-backup-${today()}.zip") }, Modifier.fillMaxWidth()) {
                Text("EXPORTAR BACKUP")
            }
            OutlinedButton(onClick = { backupRestorer.launch(arrayOf("application/zip")) }, Modifier.fillMaxWidth()) {
                Text("RESTAURAR BACKUP")
            }
            status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    }

    if (pdfPicker) {
        com.chronopass.app.ui.components.EmployeePickerDialog(
            title = "PDF — selecionar funcionário",
            employees = employees,
            onDismiss = { pdfPicker = false },
            onPick = { e -> pdfPicker = false; exportPdf(e) }
        )
    }

    if (datePicker) {
        val state = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { datePicker = false },
            confirmButton = {
                TextButton(
                    enabled = state.selectedStartDateMillis != null,
                    onClick = {
                        val s = state.selectedStartDateMillis!!
                        val e = state.selectedEndDateMillis ?: s
                        customRange = TimeUtil.startOfDay(TimeUtil.fromPickerUtc(s)) to
                            TimeUtil.endOfDay(TimeUtil.fromPickerUtc(e))
                        period = ReportPeriod.CUSTOM
                        datePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { datePicker = false }) { Text("Cancelar") } }
        ) {
            DateRangePicker(
                state = state,
                modifier = Modifier.height(480.dp),
                title = { Text("Escolher período", Modifier.padding(start = 16.dp, top = 16.dp)) }
            )
        }
    }
}

private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

// Nome do funcionário dentro do nome do arquivo: sem espaço nem barra.
private fun slug(name: String) =
    name.trim().replace(Regex("[^A-Za-z0-9]+"), "-").trim('-').ifEmpty { "funcionario" }
