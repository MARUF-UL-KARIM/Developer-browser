package com.agon.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.agon.app.ui.devtools.DevToolsScreen
import com.agon.app.ui.screens.BrowserScreen
import com.agon.app.ui.screens.ExtensionsScreen
import com.agon.app.ui.screens.LibraryScreen
import com.agon.app.ui.screens.ScriptsScreen
import com.agon.app.ui.screens.SettingsScreen
import com.agon.app.ui.screens.TabSwitcherScreen
import com.agon.app.ui.screens.ToolsScreen
import com.agon.app.ui.screens.WebAppsScreen
import com.agon.app.ui.theme.AgonAppTheme
import com.agon.app.viewmodel.BrowserViewModel

class MainActivity : ComponentActivity() {

    private var pendingUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        pendingUrl = intent?.dataString
        setContent { DevBrowserApp(pendingUrl) { pendingUrl = null } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingUrl = intent.dataString
    }
}

@Composable
fun DevBrowserApp(initialUrl: String?, onUrlConsumed: () -> Unit) {
    val vm: BrowserViewModel = viewModel()
    val systemDark = isSystemInDarkTheme()
    val dark = when (vm.settings.themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    LaunchedEffect(initialUrl, vm.loaded) {
        val url = initialUrl
        if (!url.isNullOrBlank() && vm.loaded) {
            vm.newTab(url, false)
            onUrlConsumed()
        }
    }

    AgonAppTheme(darkTheme = dark) {
        val navController = rememberNavController()

        fun route(target: String) {
            when (target) {
                "browser" -> navController.popBackStack("browser", false)
                else -> navController.navigate(target)
            }
        }

        NavHost(navController = navController, startDestination = "browser") {
            composable("browser") {
                BrowserScreen(vm = vm, onRoute = { route(it) })
            }
            composable("tabs") {
                TabSwitcherScreen(vm = vm, onBack = { navController.popBackStack() })
            }
            composable("devtools") {
                DevToolsScreen(vm = vm, onBack = { navController.popBackStack() })
            }
            composable("tools") {
                ToolsScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onRoute = { navController.navigate(it) },
                )
            }
            composable("settings") {
                SettingsScreen(vm = vm, onBack = { navController.popBackStack() })
            }
            composable("scripts") {
                ScriptsScreen(vm = vm, onBack = { navController.popBackStack() })
            }
            composable("extensions") {
                ExtensionsScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onOpenUrl = {
                        vm.navigate(it)
                        navController.popBackStack("browser", false)
                    },
                )
            }
            composable("webapps") {
                WebAppsScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onOpen = {
                        vm.newTab(it, false)
                        navController.popBackStack("browser", false)
                    },
                )
            }
            composable("library") {
                LibraryScreen(
                    vm = vm,
                    initialTab = 0,
                    onBack = { navController.popBackStack() },
                    onOpen = {
                        vm.navigate(it)
                        navController.popBackStack("browser", false)
                    },
                )
            }
            composable(
                route = "library/{tab}",
                arguments = listOf(navArgument("tab") { type = NavType.IntType }),
            ) { entry ->
                LibraryScreen(
                    vm = vm,
                    initialTab = entry.arguments?.getInt("tab") ?: 0,
                    onBack = { navController.popBackStack() },
                    onOpen = {
                        vm.navigate(it)
                        navController.popBackStack("browser", false)
                    },
                )
            }
        }
    }
}
