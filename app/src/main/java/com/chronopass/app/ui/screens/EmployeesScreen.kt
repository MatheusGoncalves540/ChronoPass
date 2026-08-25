package com.chronopass.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.chronopass.app.data.entities.Employee
import com.chronopass.app.ui.ChronoViewModel

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
            FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Default.Add, "Adicionar") }
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize()) {
            items(employees, key = { it.id }) { e ->
                ListItem(
                    headlineContent = { Text(e.name) },
                    supportingContent = { Text(if (e.active) "Ativo" else "Inativo") },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { editing = e }) { Icon(Icons.Default.Edit, "Editar") }
                            IconButton(onClick = { confirmDelete = e }) { Icon(Icons.Default.Delete, "Excluir") }
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }

    if (creating) EmployeeDialog(null, onDismiss = { creating = false }) { name, code, _ ->
        vm.addEmployee(name, code); creating = false
    }
    editing?.let { e ->
        EmployeeDialog(e, onDismiss = { editing = null }) { name, code, active ->
            vm.updateEmployee(e.copy(name = name.trim(), code = code.trim(), active = active))
            editing = null
        }
    }
    confirmDelete?.let { e ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Excluir funcionário") },
            text = { Text("Excluir ${e.name} e todas as suas marcações? Esta ação não pode ser desfeita.") },
            confirmButton = { TextButton(onClick = { vm.deleteEmployee(e); confirmDelete = null }) { Text("EXCLUIR") } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun EmployeeDialog(
    employee: Employee?, onDismiss: () -> Unit,
    onSave: (name: String, code: String, active: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(employee?.name ?: "") }
    var code by remember { mutableStateOf(employee?.code ?: "") }
    var active by remember { mutableStateOf(employee?.active ?: true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (employee == null) "Novo funcionário" else "Editar funcionário") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Código (opcional)") }, singleLine = true)
                if (employee != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Switch(checked = active, onCheckedChange = { active = it })
                        Spacer(Modifier.width(8.dp)); Text("Ativo")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name, code, active) }) { Text("SALVAR") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
