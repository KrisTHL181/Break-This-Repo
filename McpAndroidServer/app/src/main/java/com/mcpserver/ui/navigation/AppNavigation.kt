package com.mcpserver.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mcpserver.ui.screens.*

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "主页", Icons.Default.Home)
    object Settings : Screen("settings", "设置", Icons.Default.Settings)
    object Logs : Screen("logs", "日志", Icons.Default.List)
}

val screens = listOf(Screen.Home, Screen.Settings, Screen.Logs)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        topBar = { TopAppBar(title = { Text("MCP Android Server") }) },
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, Screen.Home.route, Modifier.padding(padding)) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
            composable(Screen.Logs.route) { LogScreen() }
        }
    }
}
