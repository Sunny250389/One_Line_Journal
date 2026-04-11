package com.onelinejournal.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private const val HOME_ROUTE = "home"
private const val HISTORY_ROUTE = "history"
private const val FAVORITES_ROUTE = "favorites"

@Composable
fun JournalApp(
    viewModel: JournalViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = HOME_ROUTE,
        modifier = modifier
    ) {
        composable(HOME_ROUTE) {
            HomeScreen(
                viewModel = viewModel,
                onOpenHistory = { navController.navigate(HISTORY_ROUTE) },
                onOpenFavorites = { navController.navigate(FAVORITES_ROUTE) }
            )
        }
        composable(HISTORY_ROUTE) {
            HistoryScreen(
                viewModel = viewModel,
                onOpenFavorites = { navController.navigate(FAVORITES_ROUTE) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(FAVORITES_ROUTE) {
            FavoritesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
