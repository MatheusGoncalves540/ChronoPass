package com.chronopass.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.chronopass.app.camera.CameraCapture
import com.chronopass.app.camera.PhotoStore
import com.chronopass.app.data.entities.Employee
import com.chronopass.app.ui.ChronoViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeesScreen(vm: ChronoViewModel, nav: NavController) {
    val employees by vm.allEmployees.collectAsState()
    var editing by remember { mutableStateOf<Employee?>(null) }
    var creating by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Employee?>(null) }

    Scaffold(
            topBar = { TopAppBar(title = { Text("Funcionários") }) },
            floatingActionButton = {
                FloatingActionButton(onClick = { creating = true }) {
                    Icon(Icons.Default.Add, "Adicionar")
                }
            }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize()) {
            items(employees, key = { it.id }) { e ->
                ListItem(
                        headlineContent = { Text(e.name) },
                        supportingContent = { Text(if (e.active) "Ativo" else "Inativo") },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { editing = e }) {
                                    Icon(Icons.Default.Edit, "Editar")
                                }
                                IconButton(onClick = { confirmDelete = e }) {
                                    Icon(Icons.Default.Delete, "Excluir")
                                }
                            }
                        }
                )
                HorizontalDivider()
            }
        }
    }

    if (creating)
            EmployeeDialog(null, onDismiss = { creating = false }) { name, code, _, photoPath ->
                vm.addEmployee(name, code, photoPath)
                creating = false
            }
    editing?.let { e ->
        EmployeeDialog(e, onDismiss = { editing = null }) { name, code, active, photoPath ->
            vm.updateEmployee(
                    e.copy(
                            name = name.trim(),
                            code = code.trim(),
                            active = active,
                            photoPath = photoPath
                    )
            )
            editing = null
        }
    }
    confirmDelete?.let { e ->
        AlertDialog(
                onDismissRequest = { confirmDelete = null },
                title = { Text("Excluir funcionário") },
                text = {
                    Text(
                            "Excluir ${e.name}? As marcações dele serão mantidas no histórico (marcadas como funcionário excluído). Vai para a lixeira."
                    )
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                vm.deleteEmployee(e)
                                confirmDelete = null
                            }
                    ) { Text("EXCLUIR") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDelete = null }) { Text("Cancelar") }
                }
        )
    }
}

@Composable
private fun EmployeeDialog(
        employee: Employee?,
        onDismiss: () -> Unit,
        onSave: (name: String, code: String, active: Boolean, photoPath: String?) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(employee?.name ?: "") }
    var code by remember { mutableStateOf(employee?.code ?: "") }
    var active by remember { mutableStateOf(employee?.active ?: true) }
    var photoPath by remember { mutableStateOf(employee?.photoPath) }
    var capturing by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    val cameraPerm =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted
                ->
                hasCameraPermission = granted
            }

    if (capturing) {
        CameraDialog(
                hasCameraPermission = hasCameraPermission,
                onRequestPermission = { cameraPerm.launch(Manifest.permission.CAMERA) },
                onPhoto = { file ->
                    photoPath = PhotoStore.saveEmployeePhoto(context, file).absolutePath
                    capturing = false
                },
                onDismiss = { capturing = false }
        )
    } else {

        AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Text(if (employee == null) "Novo funcionário" else "Editar funcionário")
                },
                text = {
                    Column {
                        OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Nome") },
                                singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                                value = code,
                                onValueChange = { code = it },
                                label = { Text("Código (opcional)") },
                                singleLine = true
                        )
                        if (employee != null) {
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(checked = active, onCheckedChange = { active = it })
                                Spacer(Modifier.width(8.dp))
                                Text("Ativo")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        val existing =
                                photoPath?.let { path -> if (File(path).exists()) path else null }
                        if (existing != null) {
                            AsyncImage(
                                    model = File(existing),
                                    contentDescription = "Foto do colaborador",
                                    modifier =
                                            Modifier.fillMaxWidth()
                                                    .height(120.dp)
                                                    .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                            )
                            Row {
                                TextButton(onClick = { capturing = true }) { Text("TROCAR FOTO") }
                                TextButton(onClick = { photoPath = null }) { Text("REMOVER FOTO") }
                            }
                        } else {
                            OutlinedButton(
                                    onClick = { capturing = true },
                                    Modifier.fillMaxWidth()
                            ) { Text("TIRAR FOTO") }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                            enabled = name.isNotBlank(),
                            onClick = { onSave(name, code, active, photoPath) }
                    ) { Text("SALVAR") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun CameraDialog(
        hasCameraPermission: Boolean,
        onRequestPermission: () -> Unit,
        onPhoto: (File) -> Unit,
        onDismiss: () -> Unit,
) {
    LaunchedEffect(Unit) { if (!hasCameraPermission) onRequestPermission() }
    Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
                modifier = Modifier.fillMaxWidth(0.95f).height(520.dp),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp
        ) {
            if (hasCameraPermission) {
                Column {
                    CameraCapture(Modifier.weight(1f).fillMaxWidth(), onPhoto = onPhoto)
                    TextButton(onClick = onDismiss, Modifier.fillMaxWidth()) { Text("Cancelar") }
                }
            } else {
                Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                ) {
                    Text(
                            "Permissão de câmera necessária para registrar a foto.",
                            textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onRequestPermission) { Text("CONCEDER PERMISSÃO") }
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                }
            }
        }
    }
}
