package com.onelinejournal.ui

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onelinejournal.data.JournalEntry
import com.onelinejournal.data.JournalRepository
import com.onelinejournal.ui.theme.AccentTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val MAX_ENTRY_LENGTH = 120
private const val DATE_PATTERN = "yyyy-MM-dd"
private const val THEME_KEY = "accent_theme"
private const val JOURNAL_FONT_KEY = "journal_font"
private const val JOURNAL_TEXT_SIZE_KEY = "journal_text_size"
private const val REMINDER_TIME_KEY = "reminder_time"

enum class JournalFont(val label: String) {
    Sans("Sans"),
    Serif("Serif"),
    Mono("Mono"),
    Casual("Casual");

    companion object {
        fun fromName(name: String?): JournalFont {
            return values().firstOrNull { it.name == name } ?: Sans
        }
    }
}

data class JournalUiState(
    val today: String = todayKey(),
    val input: String = "",
    val streakCount: Int = 0,
    val todaysEntry: JournalEntry? = null,
    val entries: List<JournalEntry> = emptyList(),
    val isSaving: Boolean = false,
    val accentTheme: AccentTheme = AccentTheme.Green,
    val journalFont: JournalFont = JournalFont.Sans,
    val journalTextSize: Int = 16,
    val reminderTime: String? = null
) {
    val charactersRemaining: Int = MAX_ENTRY_LENGTH - input.length
    val canSave: Boolean = input.isNotBlank() && input.length <= MAX_ENTRY_LENGTH && !isSaving
}

private data class JournalSettings(
    val accentTheme: AccentTheme,
    val journalFont: JournalFont,
    val journalTextSize: Int,
    val reminderTime: String?
)

class JournalViewModel(
    private val repository: JournalRepository,
    private val preferences: SharedPreferences
) : ViewModel() {

    private val draft = MutableStateFlow<String?>(null)
    private val isSaving = MutableStateFlow(false)
    private val accentTheme = MutableStateFlow(
        AccentTheme.fromName(preferences.getString(THEME_KEY, AccentTheme.Green.name))
    )
    private val journalFont = MutableStateFlow(
        JournalFont.fromName(preferences.getString(JOURNAL_FONT_KEY, JournalFont.Sans.name))
    )
    private val journalTextSize = MutableStateFlow(
        preferences.getInt(JOURNAL_TEXT_SIZE_KEY, 16)
    )
    private val reminderTime = MutableStateFlow(
        preferences.getString(REMINDER_TIME_KEY, null)
    )
    private val settingsState = combine(
        accentTheme,
        journalFont,
        journalTextSize,
        reminderTime
    ) { theme, font, textSize, reminder ->
        JournalSettings(
            accentTheme = theme,
            journalFont = font,
            journalTextSize = textSize,
            reminderTime = reminder
        )
    }

    val uiState: StateFlow<JournalUiState> = combine(
        repository.observeEntries(),
        draft,
        isSaving,
        settingsState
    ) { entries, input, saving, settings ->
        val today = todayKey()
        val todaysEntry = entries.firstOrNull { it.date == today }
        val displayInput = input ?: todaysEntry?.content.orEmpty()

        JournalUiState(
            today = today,
            input = displayInput,
            streakCount = calculateStreak(entries),
            todaysEntry = todaysEntry,
            entries = entries,
            isSaving = saving,
            accentTheme = settings.accentTheme,
            journalFont = settings.journalFont,
            journalTextSize = settings.journalTextSize,
            reminderTime = settings.reminderTime
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = JournalUiState()
    )

    fun onInputChanged(value: String) {
        draft.value = value.take(MAX_ENTRY_LENGTH)
    }

    fun saveTodayEntry() {
        val current = uiState.value
        val content = current.input.trim()
        if (content.isBlank() || content.length > MAX_ENTRY_LENGTH) return

        viewModelScope.launch {
            isSaving.value = true
            repository.saveEntry(
                JournalEntry(
                    date = current.today,
                    content = content,
                    isFavorite = current.todaysEntry?.isFavorite ?: false
                )
            )
            draft.value = null
            isSaving.value = false
        }
    }

    fun toggleFavorite(entry: JournalEntry) {
        viewModelScope.launch {
            repository.updateFavorite(entry.date, !entry.isFavorite)
        }
    }

    fun setAccentTheme(theme: AccentTheme) {
        accentTheme.value = theme
        preferences.edit().putString(THEME_KEY, theme.name).apply()
    }

    fun setJournalFont(font: JournalFont) {
        journalFont.value = font
        preferences.edit().putString(JOURNAL_FONT_KEY, font.name).apply()
    }

    fun setJournalTextSize(size: Int) {
        val safeSize = size.coerceIn(14, 24)
        journalTextSize.value = safeSize
        preferences.edit().putInt(JOURNAL_TEXT_SIZE_KEY, safeSize).apply()
    }

    fun setReminderTime(time: String) {
        reminderTime.value = time
        preferences.edit().putString(REMINDER_TIME_KEY, time).apply()
    }

    private fun calculateStreak(entries: List<JournalEntry>): Int {
        if (entries.isEmpty()) return 0

        val entryDates = entries
            .map { it.date }
            .toSet()

        var streak = 0
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        var cursor = when {
            entryDates.contains(formatDate(today)) -> today
            entryDates.contains(formatDate(yesterday)) -> yesterday
            else -> return 0
        }

        while (entryDates.contains(formatDate(cursor))) {
            streak++
            cursor.add(Calendar.DAY_OF_YEAR, -1)
        }

        return streak
    }
}

private fun todayKey(): String = formatDate(Calendar.getInstance())

private fun formatDate(calendar: Calendar): String {
    return SimpleDateFormat(DATE_PATTERN, Locale.US).format(calendar.time)
}

class JournalViewModelFactory(
    private val repository: JournalRepository,
    private val preferences: SharedPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JournalViewModel::class.java)) {
            return JournalViewModel(repository, preferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
