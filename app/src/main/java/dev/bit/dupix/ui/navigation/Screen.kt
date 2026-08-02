package dev.bit.dupix.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Home : Screen("home")
    data object Scan : Screen("scan")
    data object Results : Screen("results")
    data object Large : Screen("large")
    data object Settings : Screen("settings")
    data object RecycleBin : Screen("recyclebin")
    data object Group : Screen("group/{category}") {
        fun route(category: String) = "group/$category"
    }
}
