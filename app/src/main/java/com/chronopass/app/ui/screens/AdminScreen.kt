package com.chronopass.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.chronopass.app.ui.ChronoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(vm: ChronoViewModel, nav: NavController) {
    var unlocked by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Administração") }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(16.dp)) {
            if (!unlocked) {
                Spacer(Modifier.height(24.dp))
                Text("Senha do administrador", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input, onValueChange = { input = it; error = false },
                    label = { Text("Senha") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error, modifier = Modifier.fillMaxWidth()
                )
                if (error) Text("Senha incorreta", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        vm.checkAdminPassword(input) { ok -> if (ok) unlocked = true else error = true }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("ENTRAR") }
            } else {
                val items = listOf(
                    "Funcionários" to "employees",
                    "Marcações" to "records",
                    "Relatórios e backup" to "reports",
                    "Configurações" to "settings",
                )
                items.forEach { (label, route) ->
                    Card(
                        onClick = { nav.navigate(route) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    ) { Text(label, Modifier.padding(20.dp), style = MaterialTheme.typography.titleMedium) }
                }
            }
        }
    }
}
