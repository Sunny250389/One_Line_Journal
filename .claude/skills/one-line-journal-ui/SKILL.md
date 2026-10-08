---
name: one-line-journal-ui
description: >
  Design and implement UI for the One Line Journal Android app — animations,
  micro-interactions, visual polish, layout improvements, and delight. Use this
  skill whenever the user asks to improve a screen's look or feel, add animations,
  make interactions more satisfying, tweak layout or spacing, change typography or
  colour usage, or make any part of the UI more fun, polished, or minimalist.
  Covers HomeScreen, HistoryScreen, FavoritesScreen, SettingsScreen, JournalApp
  navigation, and the ShareCard. Also use it for character-count feedback, streak
  display, calendar polish, entry cards, bottom navigation, and empty states.
---

# One Line Journal — UI Skill

## Philosophy

The app is about brevity and mindfulness — one sentence per day. The UI should
mirror that: **uncluttered, purposeful, with small moments of delight that don't
distract**. Every animation should feel earned. Every visual flourish should serve
the user's mood, not compete with their writing.

The three-word test: **calm, warm, alive.** If an interaction feels frantic,
cold, or flat, reconsider it.

## Tech reference

- **Language:** Kotlin, Jetpack Compose, Material3
- **Package:** `com.onelinejournal`
- **Key files:**
  - `ui/HomeScreen.kt` — streak card, journal editor, mini calendar
  - `ui/HistoryScreen.kt` — entry list, `FavoritesScreen` (same file)
  - `ui/JournalApp.kt` — `NavHost` + `NavigationBar`
  - `ui/JournalViewModel.kt` — `JournalUiState`, `JournalFont` enum, `BackupSyncState`
  - `ui/SettingsScreen.kt` — theme, font, reminder, backup
  - `ui/ShareCard.kt` — bitmap export of a journal entry card
  - `ui/ThemeColorMenu.kt` — accent theme picker using `FlowRow`
  - `ui/theme/AccentTheme.kt` — 9 accent colours
  - `ui/theme/Theme.kt` — `MaterialTheme` wrapper
- **Font families:** `JournalFont` enum (Sans/Serif/Mono/Casual/Condensed);
  `toFontFamily()` is `internal` in `HomeScreen.kt` and imported by other screens.
- **State:** all UI state flows through `JournalUiState` in `JournalViewModel`.

---

## Animation toolkit — what to reach for

### Simple value animations
```kotlin
// Streak counter counting up on first composition
val animatedStreak by animateIntAsState(
    targetValue = streakCount,
    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
    label = "streak"
)

// Character count colour shifting towards warning
// AccentTheme.Amber.color is the project's amber — no hard-coded hex needed
val countColor by animateColorAsState(
    targetValue = when {
        remaining <= 10 -> MaterialTheme.colorScheme.error
        remaining <= 30 -> AccentTheme.Amber.color // from ui/theme/AccentTheme.kt
        else            -> MaterialTheme.colorScheme.onSurfaceVariant
    },
    label = "charCountColor"
)

// Favourite heart scale bounce
val heartScale by animateFloatAsState(
    targetValue = if (isFavorite) 1.2f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "heartScale",
    finishedListener = { /* reset to 1f */ }
)
```

### Content transitions
```kotlin
// Month change in calendar — slide direction matches forward/back
AnimatedContent(
    targetState = month,
    transitionSpec = {
        val dir = if (targetState > initialState) 1 else -1
        slideInHorizontally { dir * it } + fadeIn() togetherWith
        slideOutHorizontally { -dir * it } + fadeOut()
    },
    label = "monthTransition"
) { m -> CalendarGrid(month = m, ...) }

// Crossfade for save button label (SAVE ↔ UPDATE)
Crossfade(targetState = todaysEntry == null, label = "saveLabel") { isNew ->
    Text(if (isNew) "SAVE ENTRY" else "UPDATE ENTRY")
}
```

### List item animations (Compose 1.7+)
```kotlin
LazyColumn {
    items(entries, key = { it.date }) { entry ->
        JournalEntryCard(
            modifier = Modifier.animateItem(
                fadeInSpec = tween(200),
                placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ),
            ...
        )
    }
}
```

### Haptic feedback
```kotlin
val haptic = LocalHapticFeedback.current
// On successful save:
haptic.performHapticFeedback(HapticFeedbackType.LongPress)
```

### Scale press effect (for buttons that feel physical)
```kotlin
val interactionSource = remember { MutableInteractionSource() }
val isPressed by interactionSource.collectIsPressedAsState()
val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "press")

Button(
    interactionSource = interactionSource,
    modifier = Modifier.scale(scale),
    ...
)
```

---

## Screen-by-screen guidance

### HomeScreen

**Streak card**
- Animate the number counting up with `animateIntAsState` (tween ~600 ms).
- When streak is ≥ 7, add a subtle golden glow — use `shadow()` or a second
  `background` layer with low-alpha amber drawn behind the card.
  Use `AccentTheme.Amber.color` (already in `ui/theme/AccentTheme.kt`) for the
  amber tint — don't hard-code a hex like `0xFFFFB300`.
- "Keep it going!" copy can vary: if streak is 0, show "Start your streak today";
  if streak is 1, "One day — great start!"; if ≥ 30, "🔥 {n} days". Keep it warm.

**Journal editor card**
- Character count (`${120 - remaining}/120`) should animate its colour:
  grey → amber at ≤ 30 remaining → red at ≤ 10.
- At ≤ 10 remaining, gently shake the counter with a `keyframes` animation
  to signal urgency without alarming.
- Save button: scale-press effect + haptic on successful save + `Crossfade`
  between "SAVE ENTRY" / "UPDATE ENTRY".
- After a successful save, briefly show a subtle success tint on the card
  (`animateColorAsState` on `containerColor`) that fades back to surface.

**Calendar (JournalCalendar)**
- Wrap the month grid body in `AnimatedContent` so changing months slides
  left/right (direction follows earlier/later).
- Use `< ` / `>` text buttons or replace with `Icon(Icons.AutoMirrored.Filled.
  KeyboardArrowLeft/Right)` for cleaner look.
- Today's date dot: use `MaterialTheme.colorScheme.primary` instead of the
  hard-coded green, so it respects the accent theme.
- Missed days (red): soften to `MaterialTheme.colorScheme.errorContainer` so it
  reads as a gentle nudge, not an alarm.

**TodayDateRow**
- Remove the calendar icon — the JournalCalendar card below already provides
  that context. Simpler.

---

### HistoryScreen & FavoritesScreen

**Entry cards**
- Add `animateItem()` to `LazyColumn` items for smooth insertion/removal.
- Favourite heart icon: `graphicsLayer { scaleX = heartScale; scaleY = heartScale }`
  using a spring-animated float that pulses to 1.25× then settles.
- Date label: format as "Wed, Oct 8" (human-readable) rather than raw "2026-10-08".
  Use `LocalDate.parse(entry.date).format(DateTimeFormatter.ofPattern("EEE, MMM d"))`.

**Empty state**
- Replace the plain `Text` with a centred column: a subtle icon
  (`Icons.Outlined.AutoAwesome` or similar) + the message text. Add a gentle
  fade-in `AnimatedVisibility`.

**Month section headers**
- Group entries by year-month in `HistoryScreen` and add a sticky header
  (`stickyHeader {}` in `LazyColumn`) showing "October 2026". This replaces the
  flat chronological dump and makes the history scannable.
- `stickyHeader` requires `@OptIn(ExperimentalFoundationApi::class)` on the
  enclosing composable — add it, don't avoid `stickyHeader` to dodge the annotation.

---

### Navigation bar (JournalApp)

- The bottom `NavigationBar` already uses Material3 defaults — it's fine.
- Consider adding `NavigationBarItemDefaults.colors()` to tint the selected
  icon with the accent colour so it changes with `AccentTheme`.

---

### SettingsScreen

- Group settings into visual sections using surface-tinted cards
  (`MaterialTheme.colorScheme.surfaceContainerLow`) with a section title above
  each group: **Appearance**, **Daily reminder**, **Account & backup**.
- Font picker and theme picker are the most visual settings — consider a preview
  strip (a sample sentence in the current font) instead of a plain dropdown.

---

## What to avoid

| Pattern | Why |
|---|---|
| `infiniteTransition` looping animations | Distracting for a calm journalling context |
| Skeleton loaders / shimmer effects | The data loads from Room — it's instant |
| Full-screen shared element transitions | Adds complexity for little gain here |
| Confetti / particle effects on save | Too loud for the minimalist tone |
| Heavy blur overlays | Compose `BlurMaskFilter` is expensive on older APIs |
| Hard-coded colour literals in new UI code | Always use `MaterialTheme.colorScheme.*` tokens so accent themes and dark mode work |

---

## Import cheat-sheet

```kotlin
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.animateIntAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
```

All animation APIs are in the `androidx.compose.animation` and
`androidx.compose.animation.core` packages which are already on the classpath
via the Compose BOM — no new Gradle dependencies needed.
