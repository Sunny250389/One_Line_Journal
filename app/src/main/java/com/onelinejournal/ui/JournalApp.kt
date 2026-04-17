package com.onelinejournal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import com.onelinejournal.R

private const val HOME_ROUTE = "home"
private const val HISTORY_ROUTE = "history"
private const val FAVORITES_ROUTE = "favorites"
private const val SETTINGS_ROUTE = "settings"

@Composable
fun JournalApp(
    viewModel: JournalViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val bottomBar: @Composable () -> Unit = {
        NavigationBar {
            AppDestination.values().forEach { destination ->
                NavigationBarItem(
                    selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                    onClick = {
                        navController.navigate(destination.route) {
                            popUpTo(HOME_ROUTE) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = destination.iconRes),
                            contentDescription = destination.label
                        )
                    },
                    label = { Text(destination.label) }
                )
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = HOME_ROUTE,
        modifier = modifier
    ) {
        composable(HOME_ROUTE) {
            HomeScreen(
                viewModel = viewModel,
                bottomBar = bottomBar
            )
        }
        composable(HISTORY_ROUTE) {
            HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                bottomBar = bottomBar
            )
        }
        composable(FAVORITES_ROUTE) {
            FavoritesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                bottomBar = bottomBar
            )
        }
        composable(SETTINGS_ROUTE) {
            SettingsScreen(
                viewModel = viewModel,
                bottomBar = bottomBar
            )
        }
    }
}

private enum class AppDestination(
    val route: String,
    val label: String,
    val iconRes: Int
) {
    Home(HOME_ROUTE, "Home", R.drawable.ic_home),
    History(HISTORY_ROUTE, "History", R.drawable.ic_history),
    Favorites(FAVORITES_ROUTE, "Favorites", R.drawable.ic_favorite_border),
    Settings(SETTINGS_ROUTE, "Settings", R.drawable.ic_settings)
}
