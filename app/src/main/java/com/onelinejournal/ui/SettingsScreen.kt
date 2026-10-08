package com.onelinejournal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: JournalViewModel,
    onGoogleSignIn: () -> Unit,
    onGoogleSignOut: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = bottomBar,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            GoogleBackupCard(
                state = state,
                onGoogleSignIn = onGoogleSignIn,
                onGoogleSignOut = onGoogleSignOut,
                onBackupNow = viewModel::backupNow
            )

            SettingsCard(title = "Theme") {
                ThemeColorMenu(
                    selectedTheme = state.accentTheme,
                    onThemeSelected = viewModel::setAccentTheme
                )
            }

            SettingsCard(title = "Journal font") {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    JournalFont.values().forEach { font ->
                        FilterChip(
                            selected = state.journalFont == font,
                            onClick = { viewModel.setJournalFont(font) },
                            label = { Text(font.label) }
                        )
                    }
                }
                Text(
                    text = "A line from today's journal",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = state.journalFont.toFontFamily(),
                        fontSize = state.journalTextSize.sp
                    )
                )
            }

            SettingsCard(title = "Journal text size") {
                Text(
                    text = "${state.journalTextSize}sp",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = state.journalTextSize.toFloat(),
                    onValueChange = { viewModel.setJournalTextSize(it.toInt()) },
                    valueRange = 14f..24f,
                    steps = 9
                )
            }

            SettingsCard(title = "Reminder") {
                Text(
                    text = state.reminderTime?.let { "Daily reminder at $it" }
                        ?: "Tap the bell on Home to set a daily journal reminder.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GoogleBackupCard(
    state: JournalUiState,
    onGoogleSignIn: () -> Unit,
    onGoogleSignOut: () -> Unit,
    onBackupNow: () -> Unit
) {
    val signedIn = !state.signedInEmail.isNullOrBlank()
    val syncing = state.backupSyncState == BackupSyncState.Syncing

    SettingsCard(title = "Google backup") {
        if (!signedIn) {
            Text(
                text = "Sign in to back up your journal to your Google account.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onGoogleSignIn,
                enabled = !syncing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign in with Google")
            }
        } else {
            Text(
                text = state.signedInEmail.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = when {
                    syncing -> "Syncing…"
                    state.lastBackupAt != null -> "Last synced ${formatBackupTime(state.lastBackupAt)}"
                    else -> "Not synced yet"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (state.backupSyncState == BackupSyncState.Error) {
                Button(
                    onClick = onBackupNow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Retry backup")
                }
            }
            OutlinedButton(
                onClick = onGoogleSignOut,
                enabled = !syncing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign out")
            }
        }
        if (!state.backupError.isNullOrBlank()) {
            Text(
                text = state.backupError,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

private fun formatBackupTime(millis: Long): String {
    return SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(millis))
}
