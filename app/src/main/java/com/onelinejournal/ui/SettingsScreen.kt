package com.onelinejournal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    viewModel: JournalViewModel,
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            SettingsCard(title = "Theme") {
                ThemeColorMenu(
                    selectedTheme = state.accentTheme,
                    onThemeSelected = viewModel::setAccentTheme
                )
            }

            SettingsCard(title = "Journal font") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        fontFamily = state.journalFont.previewFontFamily(),
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

private fun JournalFont.previewFontFamily(): FontFamily {
    return when (this) {
        JournalFont.Sans -> FontFamily.SansSerif
        JournalFont.Serif -> FontFamily.Serif
        JournalFont.Mono -> FontFamily.Monospace
        JournalFont.Casual -> FontFamily.Cursive
    }
}
