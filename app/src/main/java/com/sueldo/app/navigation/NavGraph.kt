package com.sueldo.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sueldo.app.MainViewModel
import com.sueldo.app.ui.ajustes.AjustesScreen
import com.sueldo.app.ui.diario.DiarioScreen
import com.sueldo.app.ui.gastos.GastosScreen
import com.sueldo.app.ui.resumen.ResumenScreen
import androidx.compose.runtime.collectAsState

sealed class Screen(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Resumen : Screen("resumen", "Resumen", Icons.Filled.Home, Icons.Outlined.Home)
    data object Gastos : Screen("gastos", "Gastos", Icons.Filled.Payments, Icons.Outlined.Payments)
    data object Diario : Screen("diario", "Diario", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    data object Ajustes : Screen("ajustes", "Ajustes", Icons.Filled.Settings, Icons.Outlined.Settings)
}

val items = listOf(Screen.Resumen, Screen.Gastos, Screen.Diario, Screen.Ajustes)

@Composable
fun AppNav(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val resumen by viewModel.resumen.collectAsState()
    val gastos by viewModel.gastos.collectAsState()
    val dias by viewModel.dias.collectAsState()
    val config by viewModel.config.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.label
                            )
                        },
                        label = { Text(screen.label) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Resumen.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Resumen.route) {
                ResumenScreen(resumen = resumen)
            }
            composable(Screen.Gastos.route) {
                GastosScreen(
                    gastos = gastos,
                    onTogglePagado = viewModel::togglePagado,
                    onAddClick = { /* TODO: dialog add */ }
                )
            }
            composable(Screen.Diario.route) {
                DiarioScreen(
                    dias = dias,
                    onSaveDia = viewModel::saveDia
                )
            }
            composable(Screen.Ajustes.route) {
                AjustesScreen(
                    config = config,
                    onSave = viewModel::saveConfig
                )
            }
        }
    }
}
