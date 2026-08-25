package com.chronopass.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.chronopass.app.ui.ChronoViewModel
import androidx.compose.runtime.getValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: ChronoViewModel, nav: NavController) {
    val employees by vm.activeEmployees.collectAsState()
    var query by remember { mutableStateOf("") }

    // Leaving the admin area (any return to home) relocks it.
    LaunchedEffect(Unit) { vm.adminUnlocked = false }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ChronoPass", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { nav.navigate("admin") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Administração")
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                label = { Text("Buscar funcionário") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            val filtered = employees.filter { it.name.contains(query, ignoreCase = true) }
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("Nenhum funcionário cadastrado.\nAbra a administração para cadastrar.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(filtered, key = { it.id }) { e ->
                        ListItem(
                            headlineContent = { Text(e.name, style = MaterialTheme.typography.titleMedium) },
                            supportingContent = { if (e.code.isNotBlank()) Text(e.code) },
                            modifier = Modifier.clickable { nav.navigate("punch/${e.id}") }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
