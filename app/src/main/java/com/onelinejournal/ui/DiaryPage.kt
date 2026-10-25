package com.onelinejournal.ui

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.onelinejournal.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val LINE_HEIGHT_RATIO = 1.8f
private const val TEXT_TOP_RATIO = 0.25f
private val DIARY_DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.US)
private val DIARY_TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

/**
 * Diary-style Home (Page view). Date and name sit in the top margin with the streak on the
 * right, ruled lines fill the page, and Save stays bottom-right with the calendar bottom-left.
 * Unlike the classic view there is no 120-character limit.
 */
@Composable
internal fun DiaryPage(
    state: JournalUiState,
    onInputChanged: (String) -> Unit,
    onSave: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenDayInHistory: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val todaysEntry = state.todaysEntry
    var showCalendar by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DiaryHeader(
                userName = state.userName,
                streakCount = state.streakCount,
                hasEntry = todaysEntry != null,
                isFavorite = todaysEntry?.isFavorite == true,
                onToggleFavorite = onToggleFavorite
            )
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(6.dp))
            DiaryLines(
                text = state.input,
                textSize = state.journalTextSize,
                fontFamily = state.journalFont.toFontFamily(),
                onTextChanged = onInputChanged,
                modifier = Modifier.weight(1f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showCalendar = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_calendar),
                        contentDescription = "Open calendar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSave()
                    },
                    enabled = state.canSave,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (todaysEntry == null) "SAVE ENTRY" else "UPDATE ENTRY")
                }
            }
        }
    }

    if (showCalendar) {
        Dialog(
            onDismissRequest = { showCalendar = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                JournalCalendar(
                    entries = state.entries,
                    onDayClick = { date, hasEntry ->
                        if (hasEntry) {
                            showCalendar = false
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
}

@Composable
private fun DiaryHeader(
    userName: String,
    streakCount: Int,
    hasEntry: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            Spacer(modifier = Modifier.weight(1f))
            DiaryActions(
                hasEntry = hasEntry,
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            DiaryTitle(userName = userName, modifier = Modifier.weight(1f))
            DiaryStreak(streakCount = streakCount)
        }
    }
}

@Composable
private fun DiaryTitle(userName: String, modifier: Modifier) {
    val now by rememberCurrentTime()
    Column(modifier = modifier) {
        val date = LocalDate.now().format(DIARY_DATE_FORMAT).uppercase(Locale.US)
        Text(
            text = "$date · ${now.format(DIARY_TIME_FORMAT)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (userName.isBlank()) "My Journal" else "$userName's Journal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DiaryActions(
    hasEntry: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onToggleFavorite, enabled = hasEntry) {
            FavoriteHeart(
                isFavorite = isFavorite,
                activeTint = MaterialTheme.colorScheme.primary,
                inactiveTint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DiaryStreak(streakCount: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "🔥",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = streakCount.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Text area drawn over ruled paper. The rules live inside the scrolling content, so they stay
 * aligned with the text as the page grows. The page is never shorter than the visible area.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiaryLines(
    text: String,
    textSize: Int,
    fontFamily: FontFamily,
    onTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val lineHeight = (textSize * LINE_HEIGHT_RATIO).sp
    val lineHeightPx = with(density) { lineHeight.toPx() }
    val ruleWidth = with(density) { 1.dp.toPx() }
    // Text sits a quarter line below its rule so the baseline almost touches it.
    val textTopPadding = with(density) { (lineHeightPx * TEXT_TOP_RATIO).toDp() }
    val ruleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val textStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurface,
        fontFamily = fontFamily,
        fontSize = textSize.sp,
        lineHeight = lineHeight
    )
    val scroll = rememberScrollState()

    // The field keeps its own selection so the caret position is known for auto-scrolling.
    var fieldValue by remember { mutableStateOf(TextFieldValue(text)) }
    if (fieldValue.text != text) {
        fieldValue = TextFieldValue(text, TextRange(text.length))
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val bringIntoView = remember { BringIntoViewRequester() }
    val topPaddingPx = with(density) { textTopPadding.toPx() }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val minHeight = maxHeight
        // Scroll the page so the caret stays visible as you type, and when the keyboard
        // shrinks the visible area.
        LaunchedEffect(fieldValue.selection, layout, minHeight) {
            val result = layout ?: return@LaunchedEffect
            val caret = result.getCursorRect(fieldValue.selection.end.coerceIn(0, text.length))
            bringIntoView.bringIntoView(caret.translate(0f, topPaddingPx))
        }
        Box(modifier = Modifier.fillMaxSize().verticalScroll(scroll)) {
            BasicTextField(
                value = fieldValue,
                onValueChange = { next ->
                    fieldValue = next
                    onTextChanged(next.text)
                },
                onTextLayout = { layout = it },
                textStyle = textStyle,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .bringIntoViewRequester(bringIntoView)
                    .drawBehind {
                        drawRuledPage(
                            lineHeightPx = lineHeightPx,
                            ruleWidth = ruleWidth,
                            ruleColor = ruleColor
                        )
                    }
                    .padding(top = textTopPadding),
                decorationBox = { inner ->
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                text = "Write about your day...",
                                style = textStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        inner()
                    }
                }
            )
        }
    }
}

private fun DrawScope.drawRuledPage(
    lineHeightPx: Float,
    ruleWidth: Float,
    ruleColor: Color
) {
    var y = lineHeightPx
    while (y < size.height) {
        drawLine(
            color = ruleColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = ruleWidth
        )
        y += lineHeightPx
    }
}
