package com.onelinejournal.backup

/** SharedPreferences file names and keys shared by the UI and the background retry worker. */
object BackupPrefs {
    const val SETTINGS_FILE = "journal_settings"
    const val OWNER_FILE = "journal_owner"
    const val OWNER_EMAIL_KEY = "journal_owner_email"
    const val LAST_BACKUP_KEY = "google_backup_last_sync"
    const val PENDING_KEY = "backup_pending"
}
