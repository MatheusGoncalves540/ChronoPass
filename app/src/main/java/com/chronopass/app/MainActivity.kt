package com.chronopass.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chronopass.app.ui.ChronoViewModel
import com.chronopass.app.ui.screens.*
import com.chronopass.app.ui.theme.ChronoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ChronoTheme { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { App() } } }
    }
}

@Composable
fun App() {
    val nav = rememberNavController()
    val vm: ChronoViewModel = viewModel()

    NavHost(nav, startDestination = "home") {
        composable("home") { HomeScreen(vm, nav) }
        composable(
            "punch/{employeeId}",
            arguments = listOf(navArgument("employeeId") { type = NavType.LongType })
        ) { back ->
            PunchScreen(vm, nav, back.arguments!!.getLong("employeeId"))
        }
        composable("admin") { AdminScreen(vm, nav) }
        composable("employees") { EmployeesScreen(vm, nav) }
        composable("records") { RecordsScreen(vm, nav) }
        composable("reports") { ReportsScreen(vm, nav) }
        composable("settings") { SettingsScreen(vm, nav) }
    }
}
