package com.onelinejournal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onelinejournal.data.JournalEntry
import com.onelinejournal.data.JournalRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val MAX_ENTRY_LENGTH = 120

data class JournalUiState(
    val today: LocalDate = LocalDate.now(),
    val input: String = "",
    val streakCount: Int = 0,
    val todaysEntry: JournalEntry? = null,
    val entries: List<JournalEntry> = emptyList(),
    val isSaving: Boolean = false
) {
    val charactersRemaining: Int = MAX_ENTRY_LENGTH - input.length
    val canSave: Boolean = input.isNotBlank() && input.length <= MAX_ENTRY_LENGTH && !isSaving
}

class JournalViewModel(
    private val repository: JournalRepository
) : ViewModel() {

    private val draft = MutableStateFlow<String?>(null)
    private val isSaving = MutableStateFlow(false)

    val uiState: StateFlow<JournalUiState> = combine(
        repository.observeEntries(),
        draft,
        isSaving
    ) { entries, input, saving ->
        val today = LocalDate.now()
        val todayKey = today.toString()
        val todaysEntry = entries.firstOrNull { it.date == todayKey }
        val displayInput = input ?: todaysEntry?.content.orEmpty()

        JournalUiState(
            today = today,
            input = displayInput,
            streakCount = calculateStreak(entries),
            todaysEntry = todaysEntry,
            entries = entries,
            isSaving = saving
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
                    date = current.today.toString(),
                    content = content
                )
            )
            draft.value = null
            isSaving.value = false
        }
    }

    private fun calculateStreak(entries: List<JournalEntry>): Int {
        if (entries.isEmpty()) return 0

        val entryDates = entries
            .map { LocalDate.parse(it.date) }
            .toSet()

        var streak = 0
        var cursor = when {
            entryDates.contains(LocalDate.now()) -> LocalDate.now()
            entryDates.contains(LocalDate.now().minusDays(1)) -> LocalDate.now().minusDays(1)
            else -> return 0
        }

        while (entryDates.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }

        return streak
    }
}

class JournalViewModelFactory(
    private val repository: JournalRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JournalViewModel::class.java)) {
            return JournalViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
