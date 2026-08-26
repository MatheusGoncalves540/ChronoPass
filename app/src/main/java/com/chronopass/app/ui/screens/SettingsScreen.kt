package com.chronopass.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.navigation.NavController
import com.chronopass.app.location.getCurrentFix
import com.chronopass.app.ui.ChronoViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: ChronoViewModel, nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store by vm.store.collectAsState()

    var name by remember(store) { mutableStateOf(store?.name ?: "Loja Principal") }
    var lat by remember(store) { mutableStateOf(store?.latitude?.toString() ?: "") }
    var lon by remember(store) { mutableStateOf(store?.longitude?.toString() ?: "") }
    var radius by remember(store) { mutableStateOf(store?.radius?.toString() ?: "100") }
    var msg by remember { mutableStateOf<String?>(null) }

    var newPw by remember { mutableStateOf("") }

    var trashCount by remember { mutableStateOf(0) }
    var confirmTrash by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.loadTrashCount { trashCount = it } }

    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "—"
    }

    val locPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) scope.launch {
            msg = "Obtendo localização…"
            val fix = getCurrentFix(context)
            if (fix != null) {
                lat = fix.latitude.toString(); lon = fix.longitude.toString()
                msg = "Localização preenchida (precisão ${fix.accuracy.toInt()}m)."
            } else msg = "Não foi possível obter a localização agora."
        } else msg = "Permissão de localização negada."
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Configurações") }) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Localização da loja", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(lat, { lat = it }, label = { Text("Latitude") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(lon, { lon = it }, label = { Text("Longitude") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(radius, { radius = it }, label = { Text("Raio permitido (metros)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            Row {
                OutlinedButton(onClick = {
                    locPerm.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }) { Text("Usar local atual") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val la = lat.toDoubleOrNull(); val lo = lon.toDoubleOrNull(); val r = radius.toFloatOrNull()
                    if (la != null && lo != null && r != null) {
                        vm.saveStore(name.trim(), la, lo, r); msg = "Loja salva."
                    } else msg = "Preencha latitude, longitude e raio."
                }) { Text("SALVAR LOJA") }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("Senha do administrador", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(newPw, { newPw = it }, label = { Text("Nova senha") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Button(enabled = newPw.length >= 4, onClick = {
                vm.setAdminPassword(newPw); newPw = ""; msg = "Senha atualizada."
            }, modifier = Modifier.fillMaxWidth()) { Text("ALTERAR SENHA") }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("Lixeira", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Funcionários e marcações excluídos ficam aqui. Limpar apaga em definitivo.",
                style = MaterialTheme.typography.bodySmall)
            OutlinedButton(
                enabled = trashCount > 0,
                onClick = { confirmTrash = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text(if (trashCount > 0) "LIMPAR LIXEIRA ($trashCount)" else "LIXEIRA VAZIA") }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("Sobre o app", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("ChronoPass", style = MaterialTheme.typography.titleLarge)
            Text("Ponto eletrônico simples: registro de entrada/saída por funcionário, com localização, foto e relatórios em PDF.",
                style = MaterialTheme.typography.bodySmall)
            Text("Versão $versionName", style = MaterialTheme.typography.bodySmall)

            msg?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    }

    if (confirmTrash) {
        AlertDialog(
            onDismissRequest = { confirmTrash = false },
            title = { Text("Limpar lixeira") },
            text = { Text("Apagar em definitivo $trashCount item(ns)? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmTrash = false
                    vm.emptyTrash { vm.loadTrashCount { trashCount = it }; msg = "Lixeira esvaziada." }
                }) { Text("APAGAR TUDO") }
            },
            dismissButton = { TextButton(onClick = { confirmTrash = false }) { Text("Cancelar") } }
        )
    }
}
