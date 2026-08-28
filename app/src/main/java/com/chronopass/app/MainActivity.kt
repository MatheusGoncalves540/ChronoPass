package com.chronopass.app

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chronopass.app.ui.ChronoViewModel
import com.chronopass.app.ui.screens.*
import com.chronopass.app.ui.theme.ChronoTheme
import com.chronopass.app.update.UpdateAvailableDialog
import com.chronopass.app.update.UpdateChecker
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChronoTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    App()
                }
            }
        }
    }
}

@Composable
fun App() {
    val nav = rememberNavController()
    val vm: ChronoViewModel = viewModel()
    val context = LocalContext.current
    var updateInfo by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    var dismissed by rememberSaveable { mutableStateOf(false) }
    var installLaunched by rememberSaveable { mutableStateOf(false) }
    var lastCheckMs by remember { mutableStateOf(0L) }
    val scope = rememberCoroutineScope()

    suspend fun checkUpdate() {
        val now = SystemClock.elapsedRealtime()
        if (installLaunched || dismissed || now - lastCheckMs < 60_000L) return
        lastCheckMs = now
        val versionName =
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
        val info = UpdateChecker.checkForUpdate(versionName) ?: return
        // Retomada pós-permissão: APK já baixado? Instala direto, sem re-baixar.
        val file = UpdateChecker.downloadedApk(context, info)
        if (file != null && UpdateChecker.canInstallUnknownApps(context)) {
            installLaunched = true
            updateInfo = null
            UpdateChecker.installApk(context, file)
        } else {
            updateInfo = info
        }
    }

    LaunchedEffect(Unit) { checkUpdate() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) scope.launch { checkUpdate() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    NavHost(nav, startDestination = "home") {
        composable("home") { HomeScreen(vm, nav) }
        composable(
                "punch/{employeeId}",
                arguments = listOf(navArgument("employeeId") { type = NavType.LongType })
        ) { back -> PunchScreen(vm, nav, back.arguments!!.getLong("employeeId")) }
        composable("admin") { AdminScreen(vm, nav) }
        composable("employees") { EmployeesScreen(vm, nav) }
        composable("records") { RecordsScreen(vm, nav) }
        composable("reports") { ReportsScreen(vm, nav) }
        composable("settings") { SettingsScreen(vm, nav) }
    }

    updateInfo?.let { info ->
        UpdateAvailableDialog(
                info = info,
                onDismiss = {
                    updateInfo = null
                    dismissed = true
                },
                onInstallLaunched = {
                    installLaunched = true
                    updateInfo = null
                }
        )
    }
}
