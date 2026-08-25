package com.chronopass.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.chronopass.app.data.entities.Employee

// Design-system note: ONE scalable picker for every dynamically-growing option
// list. Backed by LazyColumn (renders only visible rows -> scales to thousands),
// height-capped, with search that appears once the list is long. Reused by the
// records "add punch" flow and the PDF report picker so they behave identically.
private val LIST_MAX_HEIGHT = 360.dp
private val SEARCH_THRESHOLD = 8 // show the search box only when it helps

@Composable
fun EmployeePickerDialog(
    title: String,
    employees: List<Employee>,
    onDismiss: () -> Unit,
    onPick: (Employee) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(employees, query) {
        if (query.isBlank()) employees
        else employees.filter { it.name.contains(query, ignoreCase = true) || it.code.contains(query, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                if (employees.size >= SEARCH_THRESHOLD) {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        placeholder = { Text("Buscar…") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (filtered.isEmpty()) {
                    Text("Nenhum funcionário.", Modifier.padding(vertical = 24.dp))
                } else {
                    LazyColumn(Modifier.heightIn(max = LIST_MAX_HEIGHT)) {
                        items(filtered, key = { it.id }) { e ->
                            ListItem(
                                headlineContent = { Text(e.name) },
                                supportingContent = { if (e.code.isNotBlank()) Text(e.code) },
                                modifier = Modifier.clickable { onPick(e) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                }
            }
        }
    }
}
