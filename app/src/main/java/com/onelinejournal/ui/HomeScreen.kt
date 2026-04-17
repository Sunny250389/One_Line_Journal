package com.onelinejournal.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onelinejournal.R
import com.onelinejournal.ReminderScheduler
import com.onelinejournal.data.JournalEntry
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: JournalViewModel,
    bottomBar: @Composable () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val todaysEntry = state.todaysEntry
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

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
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppHeader(
                reminderTime = state.reminderTime,
                onReminderClick = {
                    val now = java.util.Calendar.getInstance()
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            val time = "%02d:%02d".format(hour, minute)
                            viewModel.setReminderTime(time)
                            ReminderScheduler.scheduleDaily(context, time)
                        },
                        now.get(java.util.Calendar.HOUR_OF_DAY),
                        now.get(java.util.Calendar.MINUTE),
                        false
                    ).show()
                },
            )

            Text(
                text = "Home",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            StreakCard(streakCount = state.streakCount)

            TodayDateRow()

            JournalEditorCard(
                input = state.input,
                todaysEntry = todaysEntry,
                textSize = state.journalTextSize,
                fontFamily = state.journalFont.toFontFamily(),
                charactersRemaining = state.charactersRemaining,
                canSave = state.canSave,
                onInputChanged = viewModel::onInputChanged,
                onSave = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    viewModel.saveTodayEntry()
                },
                onToggleFavorite = {
                    todaysEntry?.let(viewModel::toggleFavorite)
                }
            )

            JournalCalendar(entries = state.entries)
        }
    }
}

@Composable
private fun AppHeader(
    reminderTime: String?,
    onReminderClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "One Line Journal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (reminderTime != null) {
                Text(
                    text = reminderTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onReminderClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_notifications),
                    contentDescription = "Set reminder"
                )
            }
        }
    }
}

@Composable
private fun StreakCard(streakCount: Int) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
                        )
                    )
                )
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "DAILY STREAK",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.82f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$streakCount Day${if (streakCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Keep it going!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.82f)
                )
            }
        }
    }
}

@Composable
private fun TodayDateRow() {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.US)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Today's Date",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatter.format(today).uppercase(Locale.US),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_calendar),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun JournalEditorCard(
    input: String,
    todaysEntry: JournalEntry?,
    textSize: Int,
    fontFamily: FontFamily,
    charactersRemaining: Int,
    canSave: Boolean,
    onInputChanged: (String) -> Unit,
    onSave: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "One-Line Journal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Write your only line for today... (max 120 characters)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onToggleFavorite,
                    enabled = todaysEntry != null
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (todaysEntry?.isFavorite == true) {
                                R.drawable.ic_favorite
                            } else {
                                R.drawable.ic_favorite_border
                            }
                        ),
                        contentDescription = "Mark as favorite",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            OutlinedTextField(
                value = input,
                onValueChange = onInputChanged,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 3,
                shape = RoundedCornerShape(8.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = textSize.sp,
                    fontFamily = fontFamily
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedSupportingTextColor = MaterialTheme.colorScheme.primary
                ),
                placeholder = {
                    Text("Write one sentence about today")
                },
                supportingText = {
                    Text("${120 - charactersRemaining}/120")
                }
            )
            Button(
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (todaysEntry == null) "SAVE ENTRY" else "UPDATE ENTRY")
            }
        }
    }
}

@Composable
private fun JournalCalendar(entries: List<JournalEntry>) {
    val today = LocalDate.now()
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    val writtenDates = entries.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSet()
    val firstDayOffset = month.atDay(1).dayOfWeek.value % 7
    val monthName = month.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
    val weekDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val cells = buildList<LocalDate?> {
        repeat(firstDayOffset) { add(null) }
        for (day in 1..month.lengthOfMonth()) {
            add(month.atDay(day))
        }
        while (size % 7 != 0) {
            add(null)
        }
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { month = month.minusMonths(1) }) {
                    Text(
                        text = "<",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = monthName,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { month = month.plusMonths(1) }) {
                    Text(
                        text = ">",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                weekDays.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            cells.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    week.forEach { date ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (date != null) {
                                val isPastOrToday = !date.isAfter(today)
                                val hasEntry = writtenDates.contains(date)
                                val color = when {
                                    hasEntry -> Color(0xFF2E7D32)
                                    isPastOrToday -> Color(0xFFC62828)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(color),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun JournalFont.toFontFamily(): FontFamily {
    return when (this) {
        JournalFont.Sans -> FontFamily.SansSerif
        JournalFont.Serif -> FontFamily.Serif
        JournalFont.Mono -> FontFamily.Monospace
        JournalFont.Casual -> FontFamily.Cursive
    }
}
