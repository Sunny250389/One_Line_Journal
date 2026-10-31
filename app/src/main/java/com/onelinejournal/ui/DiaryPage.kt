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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
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
import com.onelinejournal.data.JournalEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private const val LINE_HEIGHT_RATIO = 1.8f
private const val TEXT_TOP_RATIO = 0.25f
private const val PAGE_SNAP_THRESHOLD = 0.3f
private const val PAGE_TURN_DEGREES = 25f
private const val PAGE_CAMERA_DISTANCE = 12f
private const val TODAY_PLACEHOLDER = "Write about your day..."
private const val PAST_PLACEHOLDER = "No entry on this day."
private val DIARY_DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.US)
private val DIARY_TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

/**
 * Diary-style Home (Page view). Each day is one ruled page. Today's page is editable, and
 * swiping right turns back to earlier days, which are read-only. Each page shows its own date,
 * entry time and streak. Unlike the classic view there is no 120-character limit.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DiaryPage(
    state: JournalUiState,
    onInputChanged: (String) -> Unit,
    onSave: () -> Unit,
    onToggleFavorite: (JournalEntry) -> Unit,
    onOpenDayInHistory: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val today = remember(state.today) { LocalDate.parse(state.today) }
    val entriesByDate = remember(state.entries) { state.entries.associateBy { it.date } }
    val oldest = remember(state.entries, today) { oldestDate(state.entries, today) }
    // Page 0 is today; each later page is one calendar day earlier, back to the oldest entry.
    // Indexing from today keeps today's page at 0 even as older entries arrive.
    val pageCount = ChronoUnit.DAYS.between(oldest, today).toInt() + 1
    val pagerState = rememberPagerState(pageCount = { pageCount })
    var showCalendar by remember { mutableStateOf(false) }

    HorizontalPager(
        state = pagerState,
        // Page 0 sits at the right edge, so swiping right reveals the day before.
        reverseLayout = true,
        // Commit to the next day once a drag passes this fraction of a page (default 0.5), so
        // short flicks that stopped about a third of the way no longer snap back.
        flingBehavior = PagerDefaults.flingBehavior(
            state = pagerState,
            snapPositionalThreshold = PAGE_SNAP_THRESHOLD
        ),
        modifier = modifier
    ) { page ->
        val date = today.minusDays(page.toLong())
        val entry = entriesByDate[date.toString()]
        DiarySheet(
            date = date,
            isToday = page == 0,
            entry = entry,
            streakCount = if (page == 0) {
                state.streakCount
            } else {
                streakEndingOn(entriesByDate.keys, date)
            },
            state = state,
            onInputChanged = onInputChanged,
            onSave = onSave,
            onToggleFavorite = { entry?.let(onToggleFavorite) },
            onOpenCalendar = { showCalendar = true },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Pages turn about the edge they leave, so a swipe reads as a page flip.
                    val turn = (page - (pagerState.currentPage + pagerState.currentPageOffsetFraction))
                        .coerceIn(-1f, 1f)
                    rotationY = turn * PAGE_TURN_DEGREES
                    cameraDistance = PAGE_CAMERA_DISTANCE * density
                    transformOrigin = TransformOrigin(if (turn > 0f) 0f else 1f, 0.5f)
                }
        )
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
private fun DiarySheet(
    date: LocalDate,
    isToday: Boolean,
    entry: JournalEntry?,
    streakCount: Int,
    state: JournalUiState,
    onInputChanged: (String) -> Unit,
    onSave: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier.imePadding(),
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
                date = date,
                isToday = isToday,
                entryTime = entry?.updatedAt,
                streakCount = streakCount,
                hasEntry = entry != null,
                isFavorite = entry?.isFavorite == true,
                onToggleFavorite = onToggleFavorite
            )
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(6.dp))
            if (isToday) {
                DiaryLines(
                    text = state.input,
                    textSize = state.journalTextSize,
                    fontFamily = state.journalFont.toFontFamily(),
                    placeholder = TODAY_PLACEHOLDER,
                    onTextChanged = onInputChanged,
                    modifier = Modifier.weight(1f)
                )
            } else {
                ReadOnlyDiaryLines(
                    text = entry?.content.orEmpty(),
                    textSize = state.journalTextSize,
                    fontFamily = state.journalFont.toFontFamily(),
                    placeholder = PAST_PLACEHOLDER,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenCalendar) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_calendar),
                        contentDescription = "Open calendar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (isToday) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSave()
                        },
                        enabled = state.canSave,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (entry == null) "SAVE ENTRY" else "UPDATE ENTRY")
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaryHeader(
    userName: String,
    date: LocalDate,
    isToday: Boolean,
    entryTime: Long?,
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
            DiaryTitle(
                userName = userName,
                date = date,
                isToday = isToday,
                entryTime = entryTime,
                modifier = Modifier.weight(1f)
            )
            DiaryStreak(streakCount = streakCount)
        }
    }
}

@Composable
private fun DiaryTitle(
    userName: String,
    date: LocalDate,
    isToday: Boolean,
    entryTime: Long?,
    modifier: Modifier
) {
    Column(modifier = modifier) {
        val dateLabel = date.format(DIARY_DATE_FORMAT).uppercase(Locale.US)
        // Only today's page ticks with the live clock. A past page shows when it was saved.
        if (isToday) {
            LiveDateLabel(dateLabel)
        } else {
            DiaryDateText(
                if (entryTime == null) dateLabel else "$dateLabel · ${formatEntryTime(entryTime)}"
            )
        }
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
private fun LiveDateLabel(dateLabel: String) {
    val now by rememberCurrentTime()
    DiaryDateText("$dateLabel · ${now.format(DIARY_TIME_FORMAT)}")
}

@Composable
private fun DiaryDateText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold
    )
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
    placeholder: String,
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
                                text = placeholder,
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

/** Read-only ruled page for past days. Plain text is much cheaper to compose than an editor. */
@Composable
private fun ReadOnlyDiaryLines(
    text: String,
    textSize: Int,
    fontFamily: FontFamily,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val lineHeight = (textSize * LINE_HEIGHT_RATIO).sp
    val lineHeightPx = with(density) { lineHeight.toPx() }
    val ruleWidth = with(density) { 1.dp.toPx() }
    val textTopPadding = with(density) { (lineHeightPx * TEXT_TOP_RATIO).toDp() }
    val ruleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val textStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurface,
        fontFamily = fontFamily,
        fontSize = textSize.sp,
        lineHeight = lineHeight
    )
    val scroll = rememberScrollState()

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val minHeight = maxHeight
        Box(modifier = Modifier.fillMaxSize().verticalScroll(scroll)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .drawBehind {
                        drawRuledPage(
                            lineHeightPx = lineHeightPx,
                            ruleWidth = ruleWidth,
                            ruleColor = ruleColor
                        )
                    }
                    .padding(top = textTopPadding)
            ) {
                Text(
                    text = text.ifEmpty { placeholder },
                    style = textStyle,
                    color = if (text.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
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

/** Earliest entry date, or today when there are no entries (or none before today). */
private fun oldestDate(entries: List<JournalEntry>, today: LocalDate): LocalDate {
    val earliest = entries
        .mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
        .minOrNull()
    return if (earliest != null && earliest.isBefore(today)) earliest else today
}

/** Consecutive days with an entry, counting back from [endDate]; the streak as of that day. */
private fun streakEndingOn(entryDates: Set<String>, endDate: LocalDate): Int {
    var streak = 0
    var cursor = endDate
    while (entryDates.contains(cursor.toString())) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}
