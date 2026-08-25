package com.chronopass.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.chronopass.app.camera.CameraCapture
import com.chronopass.app.data.entities.Employee
import com.chronopass.app.data.entities.Punch
import com.chronopass.app.data.entities.PunchType
import com.chronopass.app.location.Fix
import com.chronopass.app.location.distanceMeters
import com.chronopass.app.location.getCurrentFix
import com.chronopass.app.reports.TimeUtil
import com.chronopass.app.ui.ChronoViewModel
import java.io.File

private enum class Step { READY, CAMERA, CONFIRM, DONE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PunchScreen(vm: ChronoViewModel, nav: NavController, employeeId: Long) {
    val context = LocalContext.current
    var employee by remember { mutableStateOf<Employee?>(null) }
    var nextType by remember { mutableStateOf(PunchType.IN) }
    var step by remember { mutableStateOf(Step.READY) }
    var photo by remember { mutableStateOf<File?>(null) }
    var fix by remember { mutableStateOf<Fix?>(null) }
    var locating by remember { mutableStateOf(true) }
    var timestamp by remember { mutableStateOf(0L) }
    val store by vm.store.collectAsState()

    LaunchedEffect(employeeId) {
        employee = vm.repo.employee(employeeId)
        nextType = vm.nextType(employeeId)
    }

    val cameraPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) step = Step.CAMERA
    }
    val locationPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* result handled when we read the fix */ }

    Scaffold(topBar = { TopAppBar(title = { Text("Registrar ponto") }) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val name = employee?.name ?: "…"
            when (step) {
                Step.READY -> {
                    Spacer(Modifier.height(24.dp))
                    Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(24.dp))
                    Text("Próxima marcação:")
                    Text(
                        if (nextType == PunchType.IN) "ENTRADA" else "SAÍDA",
                        style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            locationPerm.launch(arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ))
                            cameraPerm.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.fillMaxWidth().height(64.dp)
                    ) { Text("REGISTRAR PONTO", style = MaterialTheme.typography.titleLarge) }
                }

                Step.CAMERA -> {
                    Text("Tire uma foto para registrar o ponto.")
                    CameraCapture(Modifier.fillMaxSize()) { file ->
                        photo = file
                        timestamp = System.currentTimeMillis()
                        step = Step.CONFIRM
                    }
                }

                Step.CONFIRM -> {
                    LaunchedEffect(photo) { locating = true; fix = getCurrentFix(context); locating = false }
                    Spacer(Modifier.height(16.dp))
                    Text("Confirmar marcação?", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(if (nextType == PunchType.IN) "Entrada" else "Saída")
                    Text(TimeUtil.time(timestamp))
                    Spacer(Modifier.height(12.dp))
                    val f = fix
                    if (locating) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Obtendo localização…")
                        }
                    } else if (f == null) {
                        Text("Localização não obtida", textAlign = TextAlign.Center)
                    } else {
                        Text("Localização encontrada")
                        Text("Precisão: ${f.accuracy.toInt()}m")
                        val s = store
                        if (s != null) {
                            val d = distanceMeters(f.latitude, f.longitude, s.latitude, s.longitude)
                            Text(if (d <= s.radius) "✓ Dentro da loja" else "⚠ Você está a ${d.toInt()} metros da loja")
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            val f2 = fix
                            vm.savePunch(
                                Punch(
                                    employeeId = employeeId, timestamp = timestamp, type = nextType,
                                    latitude = f2?.latitude, longitude = f2?.longitude,
                                    accuracy = f2?.accuracy, photoPath = photo?.absolutePath
                                )
                            ) { step = Step.DONE }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) { Text("CONFIRMAR") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { photo?.delete(); photo = null; step = Step.CAMERA },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("TIRAR NOVA FOTO") }
                }

                Step.DONE -> {
                    Spacer(Modifier.height(48.dp))
                    Text("✓ Ponto registrado", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(24.dp))
                    Text(name, fontWeight = FontWeight.Bold)
                    Text(if (nextType == PunchType.IN) "Entrada" else "Saída")
                    Text(TimeUtil.hm(timestamp))
                    Spacer(Modifier.height(16.dp))
                    Text("Foto salva")
                    Text(if (fix != null) "Localização salva" else "Sem localização")
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { nav.popBackStack("home", false) }, modifier = Modifier.fillMaxWidth()) {
                        Text("CONCLUIR")
                    }
                }
            }
        }
    }
}
