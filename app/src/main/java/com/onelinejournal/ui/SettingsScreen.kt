package com.onelinejournal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_NAME_INPUT = 20

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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            SettingsCard(title = "Appearance") {
                ThemeColorMenu(
                    selectedTheme = state.accentTheme,
                    onThemeSelected = viewModel::setAccentTheme
                )
                Spacer(modifier = Modifier.height(8.dp))
                JournalFontPicker(
                    selectedFont = state.journalFont,
                    onFontSelected = viewModel::setJournalFont
                )
                Text(
                    text = "A line from today's journal",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = state.journalFont.toFontFamily(),
                        fontSize = state.journalTextSize.sp
                    ),
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Text size",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = state.journalTextSize.toFloat(),
                        onValueChange = { viewModel.setJournalTextSize(it.toInt()) },
                        valueRange = 14f..24f,
                        steps = 9,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    Text(
                        text = "${state.journalTextSize}sp",
                        modifier = Modifier.width(36.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            SettingsCard(title = "Reminder") {
                Text(
                    text = state.reminderTime?.let { "Daily reminder at $it" }
                        ?: "Tap the bell on Home to set a daily reminder.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LoginCard(
                state = state,
                onSetName = viewModel::setUserName,
                onGoogleSignIn = onGoogleSignIn,
                onGoogleSignOut = onGoogleSignOut,
                onBackupNow = viewModel::backupNow
            )
        }
    }
}

@Composable
private fun LoginCard(
    state: JournalUiState,
    onSetName: (String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onGoogleSignOut: () -> Unit,
    onBackupNow: () -> Unit
) {
    val googleSignedIn = !state.signedInEmail.isNullOrBlank()
    val hasName = state.userName.isNotBlank()
    val syncing = state.backupSyncState == BackupSyncState.Syncing
    var nameInput by remember(state.userName) { mutableStateOf(state.userName) }
    val nameReady = nameInput.isNotBlank()

    SettingsCard(title = "Login") {
        when {
            // Google session without a name (e.g. signed in before names existed).
            googleSignedIn && !hasName -> {
                NameField(value = nameInput, onValueChange = { nameInput = it })
                Button(
                    onClick = { onSetName(nameInput) },
                    enabled = nameReady,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save name")
                }
            }

            googleSignedIn -> {
                Text(
                    text = "${state.userName} · ${state.signedInEmail.orEmpty()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = when {
                        syncing -> "Syncing…"
                        state.lastBackupAt != null -> "Last synced ${formatBackupTime(state.lastBackupAt)}"
                        else -> "Not synced yet"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.backupSyncState == BackupSyncState.Error) {
                        Button(
                            onClick = onBackupNow,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Retry backup")
                        }
                    }
                    OutlinedButton(
                        onClick = onGoogleSignOut,
                        enabled = !syncing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Sign out")
                    }
                }
            }

            hasName -> {
                Text(
                    text = "Hi, ${state.userName}! Your journal is saved on this device.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onGoogleSignIn,
                        enabled = !syncing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Login with Google")
                    }
                    OutlinedButton(
                        onClick = { onSetName("") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Log out")
                    }
                }
            }

            else -> {
                NameField(value = nameInput, onValueChange = { nameInput = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onSetName(nameInput) },
                        enabled = nameReady,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Login with name")
                    }
                    Button(
                        onClick = {
                            onSetName(nameInput)
                            onGoogleSignIn()
                        },
                        enabled = nameReady && !syncing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Login with Google")
                    }
                }
            }
        }
        if (!state.backupError.isNullOrBlank()) {
            Text(
                text = state.backupError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun NameField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(MAX_NAME_INPUT)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = {
            Text(
                buildAnnotatedString {
                    append("Your name ")
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.error)) { append("*") }
                }
            )
        },
        shape = RoundedCornerShape(8.dp),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done
        )
    )
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
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

private fun formatBackupTime(millis: Long): String {
    return SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(millis))
}
