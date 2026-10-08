package com.onelinejournal.ui

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.onelinejournal.ui.theme.WarningAmber
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.sin
import kotlinx.coroutines.delay
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
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: JournalViewModel,
    bottomBar: @Composable () -> Unit,
    onOpenDayInHistory: (LocalDate) -> Unit
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

            GreetingRow(userName = state.userName)

            JournalEditorCard(
                userName = state.userName,
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

            JournalCalendar(
                entries = state.entries,
                onDayClick = { date, hasEntry ->
                    if (hasEntry) {
                        onOpenDayInHistory(date)
                    } else {
                        Toast.makeText(
                            context,
                            "No entry for ${date.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
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
    val animatedStreak by animateIntAsState(
        targetValue = streakCount,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "streak"
    )
    val glowing = streakCount >= 7
    val glowAlpha by animateFloatAsState(
        targetValue = if (glowing) 0.35f else 0f,
        animationSpec = tween(600),
        label = "streakGlow"
    )
    val message = when {
        streakCount <= 0 -> "Start your streak today"
        streakCount == 1 -> "One day — great start!"
        streakCount >= 30 -> "🔥 $streakCount days"
        else -> "Keep it going!"
    }

    Card(
        modifier = Modifier.shadow(
            elevation = if (glowing) 12.dp else 0.dp,
            shape = RoundedCornerShape(10.dp),
            ambientColor = WarningAmber,
            spotColor = WarningAmber
        ),
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
                .background(WarningAmber.copy(alpha = glowAlpha))
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    space = 48.dp,
                    alignment = Alignment.CenterHorizontally
                )
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "DAILY STREAK",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.82f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$animatedStreak Day${if (streakCount == 1) "" else "s"}",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.82f)
                    )
                }
                StreakFlame(streak = streakCount)
            }
        }
    }
}

@Composable
private fun GreetingRow(userName: String) {
    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.US)
    val greeting = when (now.hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (userName.isBlank()) greeting else "$greeting, $userName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatter.format(now).uppercase(Locale.US),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun JournalEditorCard(
    userName: String,
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
    // Grow the box with the text size so a full 120-character line shows without inner scrolling.
    val visibleLines = ceil(120f * textSize * 0.55f / 300f).toInt().coerceIn(3, 6)
    val haptic = LocalHapticFeedback.current
    var justSaved by remember { mutableStateOf(false) }
    LaunchedEffect(justSaved) {
        if (justSaved) {
            delay(700)
            justSaved = false
        }
    }
    val cardColor by animateColorAsState(
        targetValue = if (justSaved) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                .compositeOver(MaterialTheme.colorScheme.surface)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(400),
        label = "saveTint"
    )
    val countColor by animateColorAsState(
        targetValue = when {
            charactersRemaining <= 10 -> MaterialTheme.colorScheme.error
            charactersRemaining <= 30 -> WarningAmber
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "charCountColor"
    )
    val shake = remember { Animatable(0f) }
    val nearLimit = charactersRemaining <= 10
    val atLimit = charactersRemaining <= 0
    LaunchedEffect(nearLimit, atLimit) {
        if (nearLimit) {
            shake.snapTo(0f)
            shake.animateTo(1f, tween(300, easing = LinearEasing))
        }
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        label = "savePress"
    )

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (userName.isBlank()) "My Journal" else "$userName's Journal",
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
                    FavoriteHeart(
                        isFavorite = todaysEntry?.isFavorite == true,
                        activeTint = MaterialTheme.colorScheme.primary,
                        inactiveTint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            OutlinedTextField(
                value = input,
                onValueChange = onInputChanged,
                modifier = Modifier.fillMaxWidth(),
                minLines = visibleLines,
                maxLines = visibleLines,
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
                    Text(
                        text = "${120 - charactersRemaining}/120",
                        color = countColor,
                        modifier = Modifier.graphicsLayer {
                            translationX = sin(shake.value * PI.toFloat() * 4f) *
                                6.dp.toPx() * (1f - shake.value)
                        }
                    )
                }
            )
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    justSaved = true
                    onSave()
                },
                enabled = canSave,
                interactionSource = interactionSource,
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(pressScale),
                shape = RoundedCornerShape(8.dp)
            ) {
                Crossfade(targetState = todaysEntry == null, label = "saveLabel") { isNew ->
                    Text(if (isNew) "SAVE ENTRY" else "UPDATE ENTRY")
                }
            }
        }
    }
}

@Composable
private fun JournalCalendar(
    entries: List<JournalEntry>,
    onDayClick: (LocalDate, Boolean) -> Unit
) {
    val today = LocalDate.now()
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    val writtenDates = entries.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSet()
    val weekDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

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
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous month"
                    )
                }
                AnimatedContent(
                    targetState = month,
                    modifier = Modifier.weight(1f),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "monthTitle"
                ) { m ->
                    Text(
                        text = m.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = { month = month.plusMonths(1) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next month"
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
            AnimatedContent(
                targetState = month,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally { dir * it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -dir * it } + fadeOut())
                },
                label = "monthTransition"
            ) { m ->
                CalendarGrid(month = m, today = today, writtenDates = writtenDates, onDayClick = onDayClick)
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    today: LocalDate,
    writtenDates: Set<LocalDate>,
    onDayClick: (LocalDate, Boolean) -> Unit
) {
    val firstDayOffset = month.atDay(1).dayOfWeek.value % 7
    val cells = buildList<LocalDate?> {
        repeat(firstDayOffset) { add(null) }
        for (day in 1..month.lengthOfMonth()) {
            add(month.atDay(day))
        }
        while (size % 7 != 0) {
            add(null)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            CalendarDay(
                                date = date,
                                isToday = date == today,
                                hasEntry = writtenDates.contains(date),
                                isPastOrToday = !date.isAfter(today),
                                onClick = { onDayClick(date, writtenDates.contains(date)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate,
    isToday: Boolean,
    hasEntry: Boolean,
    isPastOrToday: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val background = when {
        hasEntry -> colors.primary
        isPastOrToday -> colors.errorContainer
        else -> colors.onSurfaceVariant.copy(alpha = 0.12f)
    }
    val textColor = when {
        hasEntry -> colors.onPrimary
        isPastOrToday -> colors.onErrorContainer
        else -> colors.onSurfaceVariant
    }
    val animatedBackground by animateColorAsState(background, label = "dayBackground")

    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .then(if (isPastOrToday) Modifier.clickable(onClick = onClick) else Modifier)
            .background(animatedBackground)
            .then(
                if (isToday) {
                    Modifier.border(2.dp, colors.onSurface.copy(alpha = 0.7f), CircleShape)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

internal fun JournalFont.toFontFamily(): FontFamily {
    return when (this) {
        JournalFont.Sans -> FontFamily.SansSerif
        JournalFont.Serif -> FontFamily.Serif
        JournalFont.Mono -> FontFamily.Monospace
        JournalFont.Casual -> FontFamily.Cursive
        JournalFont.Condensed -> FontFamily(
            androidx.compose.ui.text.font.Font(
                familyName = androidx.compose.ui.text.font.DeviceFontFamilyName("sans-serif-condensed")
            )
        )
    }
}
