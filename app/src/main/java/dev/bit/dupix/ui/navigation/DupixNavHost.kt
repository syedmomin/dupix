package dev.bit.dupix.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.bit.dupix.domain.model.FileCategory
import dev.bit.dupix.ui.ScanViewModel
import dev.bit.dupix.ui.screens.GroupListScreen
import dev.bit.dupix.ui.screens.HomeScreen
import dev.bit.dupix.ui.screens.LargeFilesScreen
import dev.bit.dupix.ui.screens.RecoverScreen
import dev.bit.dupix.ui.screens.RecycleBinScreen
import dev.bit.dupix.ui.screens.ResultsScreen
import dev.bit.dupix.ui.screens.ScanProgressScreen
import dev.bit.dupix.ui.screens.SettingsScreen
import dev.bit.dupix.ui.screens.SplashScreen

@Composable
fun DupixNavHost(vm: ScanViewModel) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
            SplashScreen(onDone = {
                nav.navigate(Screen.Home.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Home.route) {
            HomeScreen(
                vm = vm,
                onScanStarted = { nav.navigate(Screen.Scan.route) },
                onOpenSettings = { nav.navigate(Screen.Settings.route) },
            )
        }

        composable(Screen.Scan.route) {
            ScanProgressScreen(
                vm = vm,
                onComplete = {
                    nav.navigate(Screen.Results.route) {
                        popUpTo(Screen.Scan.route) { inclusive = true }
                    }
                },
                onBack = { nav.popBackStack() },
            )
        }

        composable(Screen.Results.route) {
            ResultsScreen(
                vm = vm,
                onOpenCategory = { cat -> nav.navigate(Screen.Group.route(cat.name)) },
                onOpenLargeFiles = { nav.navigate(Screen.Large.route) },
                onBack = { nav.popBackStack() },
            )
        }

        composable(
            Screen.Group.route,
            arguments = listOf(navArgument("category") { type = NavType.StringType }),
        ) { entry ->
            val categoryName = entry.arguments?.getString("category") ?: FileCategory.PHOTO.name
            val category = runCatching { FileCategory.valueOf(categoryName) }.getOrDefault(FileCategory.PHOTO)
            GroupListScreen(
                vm = vm,
                category = category,
                onBack = { nav.popBackStack() },
                onDeleteComplete = { nav.popBackStack(Screen.Home.route, inclusive = false) },
            )
        }

        composable(Screen.Large.route) {
            LargeFilesScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onDeleteComplete = { nav.popBackStack(Screen.Home.route, inclusive = false) },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onOpenRecycleBin = { nav.navigate(Screen.RecycleBin.route) },
                onOpenRecover = { nav.navigate(Screen.Recover.route) },
            )
        }

        composable(Screen.RecycleBin.route) {
            RecycleBinScreen(vm = vm, onBack = { nav.popBackStack() })
        }

        composable(Screen.Recover.route) {
            RecoverScreen(vm = vm, onBack = { nav.popBackStack() })
        }
    }
}
