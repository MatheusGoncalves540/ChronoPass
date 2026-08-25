package com.chronopass.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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

    val locPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) scope.launch {
            getCurrentFix(context)?.let { lat = it.latitude.toString(); lon = it.longitude.toString() }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Configurações") }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

            msg?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    }
}
