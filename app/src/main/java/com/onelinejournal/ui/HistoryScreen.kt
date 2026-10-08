package com.onelinejournal.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onelinejournal.R
import com.onelinejournal.data.JournalEntry

@Composable
fun HistoryScreen(
    viewModel: JournalViewModel,
    onNavigateBack: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val focusDate by viewModel.historyFocusDate.collectAsState()
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = "History",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            JournalEntryList(
                entries = state.entries,
                journalFont = state.journalFont,
                journalTextSize = state.journalTextSize,
                emptyMessage = "No entries yet. Your journal will appear here after your first line.",
                emptyIcon = R.drawable.ic_calendar,
                focusDate = focusDate,
                onFocusConsumed = viewModel::consumeHistoryFocus,
                onToggleFavorite = viewModel::toggleFavorite,
                onShareEntry = { shareJournalEntryCard(context, it, state.journalFont) }
            )
        }
    }
}

@Composable
fun FavoritesScreen(
    viewModel: JournalViewModel,
    onNavigateBack: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val favoriteEntries = state.entries.filter { it.isFavorite }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = "Favorites",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            JournalEntryList(
                entries = favoriteEntries,
                journalFont = state.journalFont,
                journalTextSize = state.journalTextSize,
                emptyMessage = "No favorites yet. Tap a heart in History to save one here.",
                emptyIcon = R.drawable.ic_favorite_border,
                onToggleFavorite = viewModel::toggleFavorite,
                onShareEntry = { shareJournalEntryCard(context, it, state.journalFont) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun JournalEntryList(
    entries: List<JournalEntry>,
    journalFont: JournalFont,
    journalTextSize: Int,
    emptyMessage: String,
    emptyIcon: Int,
    onToggleFavorite: (JournalEntry) -> Unit,
    onShareEntry: (JournalEntry) -> Unit,
    focusDate: String? = null,
    onFocusConsumed: () -> Unit = {}
) {
    if (entries.isEmpty()) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(id = emptyIcon),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        val grouped = entries.groupBy { entry ->
            runCatching { YearMonth.from(LocalDate.parse(entry.date)) }.getOrNull()
        }
        val listState = rememberLazyListState()
        val headerClearance = with(LocalDensity.current) { 44.dp.roundToPx() }
        var highlightedDate by remember { mutableStateOf<String?>(null) }

        // Jump to the day tapped on the Home calendar, then flash its card.
        LaunchedEffect(focusDate, entries) {
            val target = focusDate ?: return@LaunchedEffect
            var index = 0
            var found = -1
            grouped.forEach { (_, monthEntries) ->
                index += 1 // sticky header
                monthEntries.forEach { entry ->
                    if (entry.date == target) found = index
                    index += 1
                }
            }
            if (found >= 0) {
                listState.animateScrollToItem(found, -headerClearance)
                highlightedDate = target
            }
            onFocusConsumed()
        }
        LaunchedEffect(highlightedDate) {
            if (highlightedDate != null) {
                delay(1600)
                highlightedDate = null
            }
        }

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            grouped.forEach { (month, monthEntries) ->
                stickyHeader(key = "header-${month ?: "unknown"}") {
                    Text(
                        text = month?.format(MONTH_HEADER_FORMAT) ?: "Other",
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(vertical = 6.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(
                    items = monthEntries,
                    key = { it.date }
                ) { entry ->
                    JournalEntryCard(
                        entry = entry,
                        journalFont = journalFont,
                        journalTextSize = journalTextSize,
                        onToggleFavorite = onToggleFavorite,
                        onShareEntry = onShareEntry,
                        highlighted = entry.date == highlightedDate,
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(200),
                            placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
                    )
                }
            }
        }
    }
}

private val MONTH_HEADER_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
private val ENTRY_DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)

private fun formatEntryDate(date: String): String {
    return runCatching { LocalDate.parse(date).format(ENTRY_DATE_FORMAT) }.getOrDefault(date)
}

@Composable
internal fun FavoriteHeart(
    isFavorite: Boolean,
    activeTint: Color,
    inactiveTint: Color,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1f) }
    var firstRun by remember { mutableStateOf(true) }
    LaunchedEffect(isFavorite) {
        if (firstRun) {
            firstRun = false
        } else if (isFavorite) {
            scale.animateTo(1.3f, tween(90))
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    Icon(
        painter = painterResource(
            id = if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
        ),
        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
        tint = if (isFavorite) activeTint else inactiveTint
    )
}

@Composable
private fun JournalEntryCard(
    entry: JournalEntry,
    journalFont: JournalFont,
    journalTextSize: Int,
    onToggleFavorite: (JournalEntry) -> Unit,
    onShareEntry: (JournalEntry) -> Unit,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val baseColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val cardColor by animateColorAsState(
        targetValue = if (highlighted) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f).compositeOver(baseColor)
        } else {
            baseColor
        },
        animationSpec = tween(400),
        label = "entryHighlight"
    )
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatEntryDate(entry.date),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = { onShareEntry(entry) }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_share),
                        contentDescription = "Share as card",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onToggleFavorite(entry) }) {
                    FavoriteHeart(
                        isFavorite = entry.isFavorite,
                        activeTint = MaterialTheme.colorScheme.primary,
                        inactiveTint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = entry.content,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = journalFont.toFontFamily(),
                    fontSize = journalTextSize.sp
                )
            )
        }
    }
}
