package com.chronopass.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.chronopass.app.backup.BackupManager
import com.chronopass.app.data.entities.Punch
import com.chronopass.app.reports.CsvExport
import com.chronopass.app.reports.PdfExport
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
        Column(Modifier.padding(pad).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                scope.launch {
                    val all = vm.repo.activePunches()
                    val names = everyone.associate { it.id to it.name }
                    val file = File(exportsDir(), "chronopass_${today()}.csv")
                    withContext(Dispatchers.IO) { CsvExport.write(file, all, names) }
                    share(file, "text/csv")
                }
            }, Modifier.fillMaxWidth()) { Text("EXPORTAR CSV") }

            Button(onClick = { pdfPicker = true }, Modifier.fillMaxWidth()) { Text("GERAR PDF (por funcionário)") }

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
            onPick = { e ->
                pdfPicker = false
                scope.launch {
                    val now = System.currentTimeMillis()
                    val from = TimeUtil.startOfDay(now - 30L * 86_400_000L)
                    val to = TimeUtil.endOfDay(now)
                    val punches: List<Punch> = vm.repo.forEmployeeBetween(e.id, from, to)
                    val file = File(exportsDir(), "espelho_${e.name}_${today()}.pdf")
                    val period = "${TimeUtil.date(from)} - ${TimeUtil.date(to)}"
                    withContext(Dispatchers.IO) { PdfExport.write(file, e.name, period, punches) }
                    share(file, "application/pdf")
                }
            }
        )
    }
}

private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
