package com.onelinejournal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    viewModel: JournalViewModel,
    onOpenHistory: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val todaysEntry = state.todaysEntry

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "One Line Journal",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = state.today.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onOpenHistory) {
                    Text("History")
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Current streak",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "${state.streakCount} day${if (state.streakCount == 1) "" else "s"}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    val helper = if (todaysEntry == null) {
                        "Write a line for today."
                    } else {
                        "You can update today's line anytime."
                    }
                    Text(
                        text = helper,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Text(
                text = "Today's entry",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = state.input,
                onValueChange = viewModel::onInputChanged,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 3,
                shape = RoundedCornerShape(20.dp),
                placeholder = {
                    Text("Write one sentence about today")
                },
                supportingText = {
                    Text("${state.charactersRemaining} characters left")
                }
            )

            Button(
                onClick = viewModel::saveTodayEntry,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(if (todaysEntry == null) "Save today's line" else "Update today's line")
            }

            if (todaysEntry != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Saved today",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = todaysEntry.content,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
