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

data class JournalUiState(
    val today: String = todayKey(),
    val input: String = "",
    val streakCount: Int = 0,
    val todaysEntry: JournalEntry? = null,
    val entries: List<JournalEntry> = emptyList(),
    val isSaving: Boolean = false,
    val accentTheme: AccentTheme = AccentTheme.Green
) {
    val charactersRemaining: Int = MAX_ENTRY_LENGTH - input.length
    val canSave: Boolean = input.isNotBlank() && input.length <= MAX_ENTRY_LENGTH && !isSaving
}

class JournalViewModel(
    private val repository: JournalRepository,
    private val preferences: SharedPreferences
) : ViewModel() {

    private val draft = MutableStateFlow<String?>(null)
    private val isSaving = MutableStateFlow(false)
    private val accentTheme = MutableStateFlow(
        AccentTheme.fromName(preferences.getString(THEME_KEY, AccentTheme.Green.name))
    )

    val uiState: StateFlow<JournalUiState> = combine(
        repository.observeEntries(),
        draft,
        isSaving,
        accentTheme
    ) { entries, input, saving, theme ->
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
            accentTheme = theme
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
