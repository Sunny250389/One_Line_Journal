package com.onelinejournal.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import com.onelinejournal.R

private const val HOME_ROUTE = "home"
private const val HISTORY_ROUTE = "history"
private const val FAVORITES_ROUTE = "favorites"
private const val SETTINGS_ROUTE = "settings"
private const val ENTRY_ARG = "date"
private const val ENTRY_ROUTE_PREFIX = "entry/"
private const val ENTRY_ROUTE = "$ENTRY_ROUTE_PREFIX{$ENTRY_ARG}"

@Composable
fun JournalApp(
    viewModel: JournalViewModel,
    onGoogleSignIn: () -> Unit,
    onGoogleSignOut: () -> Unit,
    onToggleAppLock: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val openHistory = {
        navController.navigate(HISTORY_ROUTE) {
            popUpTo(HOME_ROUTE) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
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
                    label = { Text(destination.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    )
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
                bottomBar = bottomBar,
                onOpenDayInHistory = { date ->
                    viewModel.focusHistoryEntry(date.toString())
                    openHistory()
                }
            )
        }
        composable(HISTORY_ROUTE) {
            HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenEntry = { date -> navController.navigate("$ENTRY_ROUTE_PREFIX$date") },
                bottomBar = bottomBar
            )
        }
        composable(FAVORITES_ROUTE) {
            FavoritesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenEntry = { date -> navController.navigate("$ENTRY_ROUTE_PREFIX$date") },
                bottomBar = bottomBar
            )
        }
        composable(
            route = ENTRY_ROUTE,
            arguments = listOf(navArgument(ENTRY_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            EntryDetailScreen(
                viewModel = viewModel,
                date = backStackEntry.arguments?.getString(ENTRY_ARG).orEmpty(),
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(SETTINGS_ROUTE) {
            SettingsScreen(
                viewModel = viewModel,
                onGoogleSignIn = onGoogleSignIn,
                onGoogleSignOut = onGoogleSignOut,
                onToggleAppLock = onToggleAppLock,
                bottomBar = bottomBar
            )
        }
    }

    val state by viewModel.uiState.collectAsState()
    state.pendingSwitchFrom?.let { previousAccount ->
        AccountSwitchDialog(
            previousAccount = previousAccount,
            newAccount = state.signedInEmail.orEmpty(),
            hasLocalEntries = state.entries.isNotEmpty(),
            isWorking = state.backupSyncState == BackupSyncState.Syncing,
            error = state.backupError,
            onConfirm = viewModel::confirmAccountSwitch,
            onCancel = onGoogleSignOut
        )
    }
}

/** Blocks the app until the user decides, so nothing is written while the journal may be replaced. */
@Composable
private fun AccountSwitchDialog(
    previousAccount: String,
    newAccount: String,
    hasLocalEntries: Boolean,
    isWorking: Boolean,
    error: String?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val message = when {
        isWorking -> "Loading the journal for $newAccount..."
        hasLocalEntries ->
            "This device holds the journal from $previousAccount. Continuing replaces it with the " +
                "journal backed up to $newAccount, and anything not yet backed up to " +
                "$previousAccount is permanently deleted.\n\nTo keep those entries, cancel and " +
                "log in with $previousAccount so they can back up first."
        else -> "The journal for $newAccount hasn't been loaded yet."
    }
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Switch Google account?") },
        text = { Text(if (error != null && !isWorking) "$error\n\n$message" else message) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isWorking) {
                Text(if (hasLocalEntries) "Replace journal" else "Try again")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !isWorking) { Text("Cancel") }
        }
    )
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
