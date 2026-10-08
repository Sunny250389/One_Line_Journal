package com.onelinejournal.ui

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onelinejournal.auth.GoogleAccountSession
import com.onelinejournal.backup.JournalBackupRepository
import com.onelinejournal.data.JournalEntry
import com.onelinejournal.data.JournalRepository
import com.onelinejournal.ui.theme.AccentTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
private const val LAST_BACKUP_KEY = "google_backup_last_sync"
private const val OWNER_EMAIL_KEY = "journal_owner_email"
private const val USER_NAME_KEY = "user_name"
private const val MAX_NAME_LENGTH = 20
private const val PUSH_DEBOUNCE_MS = 1_500L

enum class JournalFont(val label: String) {
    Sans("Sans"),
    Serif("Serif"),
    Mono("Mono"),
    Casual("Casual"),
    Condensed("Condensed");

    companion object {
        fun fromName(name: String?): JournalFont {
            return values().firstOrNull { it.name == name } ?: Sans
        }
    }
}

enum class BackupSyncState {
    Idle,
    Syncing,
    Success,
    Error
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
    val reminderTime: String? = null,
    val userName: String = "",
    val signedInEmail: String? = null,
    val lastBackupAt: Long? = null,
    val backupSyncState: BackupSyncState = BackupSyncState.Idle,
    val backupError: String? = null
) {
    val charactersRemaining: Int = MAX_ENTRY_LENGTH - input.length
    val canSave: Boolean = input.isNotBlank() && input.length <= MAX_ENTRY_LENGTH && !isSaving
}

private data class JournalSettings(
    val accentTheme: AccentTheme,
    val journalFont: JournalFont,
    val journalTextSize: Int,
    val reminderTime: String?,
    val userName: String
)

private data class BackupUi(
    val signedInEmail: String?,
    val lastBackupAt: Long?,
    val backupSyncState: BackupSyncState,
    val backupError: String?
)

class JournalViewModel(
    private val repository: JournalRepository,
    private val preferences: SharedPreferences,
    private val ownerPreferences: SharedPreferences,
    private val backupRepository: JournalBackupRepository
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
    private val userName = MutableStateFlow(
        preferences.getString(USER_NAME_KEY, null).orEmpty()
    )
    private val signedInEmail = MutableStateFlow(
        preferences.getString(GoogleAccountSession.EMAIL_KEY, null)
    )
    private val lastBackupAt = MutableStateFlow(
        preferences.getLong(LAST_BACKUP_KEY, 0L).takeIf { it > 0L }
    )
    private val backupSyncState = MutableStateFlow(BackupSyncState.Idle)
    private val backupError = MutableStateFlow<String?>(null)
    private var pushJob: Job? = null
    private var completedStartupSync = false

    private val settingsState = combine(
        accentTheme,
        journalFont,
        journalTextSize,
        reminderTime,
        userName
    ) { theme, font, textSize, reminder, name ->
        JournalSettings(
            accentTheme = theme,
            journalFont = font,
            journalTextSize = textSize,
            reminderTime = reminder,
            userName = name
        )
    }

    private val backupState = combine(
        signedInEmail,
        lastBackupAt,
        backupSyncState,
        backupError
    ) { email, lastBackup, syncState, error ->
        BackupUi(
            signedInEmail = email,
            lastBackupAt = lastBackup,
            backupSyncState = syncState,
            backupError = error
        )
    }

    val uiState: StateFlow<JournalUiState> = combine(
        repository.observeEntries(),
        draft,
        isSaving,
        settingsState,
        backupState
    ) { entries, input, saving, settings, backup ->
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
            reminderTime = settings.reminderTime,
            userName = settings.userName,
            signedInEmail = backup.signedInEmail,
            lastBackupAt = backup.lastBackupAt,
            backupSyncState = backup.backupSyncState,
            backupError = backup.backupError
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
            schedulePush()
        }
    }

    fun toggleFavorite(entry: JournalEntry) {
        viewModelScope.launch {
            repository.saveEntry(
                entry.copy(
                    isFavorite = !entry.isFavorite,
                    updatedAt = System.currentTimeMillis()
                )
            )
            schedulePush()
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

    fun setUserName(name: String) {
        val clean = name.trim().take(MAX_NAME_LENGTH)
        userName.value = clean
        preferences.edit().putString(USER_NAME_KEY, clean).apply()
    }

    fun setReminderTime(time: String) {
        reminderTime.value = time
        preferences.edit().putString(REMINDER_TIME_KEY, time).apply()
    }

    fun onGoogleSignedIn(email: String) {
        signedInEmail.value = email
        backupError.value = null

        val previousOwner = currentOwnerEmail()
        val switchedAccount = previousOwner != null && previousOwner != email
        ownerPreferences.edit().putString(OWNER_EMAIL_KEY, email).apply()

        viewModelScope.launch {
            runBackup {
                if (switchedAccount) {
                    backupRepository.pullReplace(allowUi = false)
                } else {
                    backupRepository.pullMergePush(allowUi = false)
                }
            }
        }
    }

    // The owner marker lives in its own prefs file so Android backup restores it together
    // with the database; otherwise a restored journal could be merged into another account.
    private fun currentOwnerEmail(): String? {
        ownerPreferences.getString(OWNER_EMAIL_KEY, null)?.let { return it }
        val legacy = preferences.getString(OWNER_EMAIL_KEY, null) ?: return null
        ownerPreferences.edit().putString(OWNER_EMAIL_KEY, legacy).apply()
        preferences.edit().remove(OWNER_EMAIL_KEY).apply()
        return legacy
    }

    fun onGoogleSignedOut() {
        pushJob?.cancel()
        backupRepository.forgetAccount()
        signedInEmail.value = null
        lastBackupAt.value = null
        backupSyncState.value = BackupSyncState.Idle
        backupError.value = null
        preferences.edit().remove(LAST_BACKUP_KEY).apply()
    }

    fun reportBackupError(message: String?) {
        backupSyncState.value = BackupSyncState.Error
        backupError.value = message ?: "Backup failed"
    }

    fun syncOnStart() {
        if (completedStartupSync) return
        completedStartupSync = true

        // Adopt the signed-in account as owner for sessions that predate ownership
        // tracking, so a later account switch is still detected.
        val currentEmail = signedInEmail.value
        if (!currentEmail.isNullOrBlank() && currentOwnerEmail() == null) {
            ownerPreferences.edit().putString(OWNER_EMAIL_KEY, currentEmail).apply()
        }
        syncFromCloud()
    }

    fun syncFromCloud() {
        if (signedInEmail.value.isNullOrBlank()) return
        viewModelScope.launch {
            runBackup { backupRepository.pullMergePush(allowUi = false) }
        }
    }

    fun backupNow() {
        if (signedInEmail.value.isNullOrBlank()) return
        pushJob?.cancel()
        viewModelScope.launch {
            runBackup { backupRepository.pullMergePush(allowUi = true) }
        }
    }

    private fun schedulePush() {
        if (signedInEmail.value.isNullOrBlank()) return
        pushJob?.cancel()
        pushJob = viewModelScope.launch {
            delay(PUSH_DEBOUNCE_MS)
            runBackup { backupRepository.pullMergePush(allowUi = false) }
        }
    }

    private suspend fun runBackup(block: suspend () -> Unit) {
        backupSyncState.value = BackupSyncState.Syncing
        backupError.value = null
        try {
            block()
            val syncedAt = System.currentTimeMillis()
            lastBackupAt.value = syncedAt
            preferences.edit().putLong(LAST_BACKUP_KEY, syncedAt).apply()
            backupSyncState.value = BackupSyncState.Success
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            backupSyncState.value = BackupSyncState.Error
            backupError.value = error.message ?: "Backup failed"
        }
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
    private val preferences: SharedPreferences,
    private val ownerPreferences: SharedPreferences,
    private val backupRepository: JournalBackupRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JournalViewModel::class.java)) {
            return JournalViewModel(repository, preferences, ownerPreferences, backupRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
