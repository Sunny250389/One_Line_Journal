package com.onelinejournal.backup

import android.util.Log
import com.onelinejournal.data.JournalEntry
import com.onelinejournal.data.JournalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JournalBackupRepository(
    private val journalRepository: JournalRepository,
    private val driveClient: DriveAppDataClient,
    private val accessToken: suspend (allowUi: Boolean) -> String
) {
    suspend fun pullMergePush(allowUi: Boolean = false) {
        withContext(Dispatchers.IO) {
            val token = accessToken(allowUi)
            val cloudJson = driveClient.downloadBackup(token)
            val cloudEntries = readCloudOrSetAside(token, cloudJson)
            val localEntries = journalRepository.getEntries()
            val merged = JournalBackupJson.merge(localEntries, cloudEntries)
            journalRepository.upsertEntries(merged)
            driveClient.uploadBackup(token, JournalBackupJson.toJson(merged))
        }
    }

    // A corrupt file would otherwise fail every sync forever. It is kept under another name, in
    // case it can be recovered, and the local journal becomes the new backup.
    private suspend fun readCloudOrSetAside(token: String, cloudJson: String?): List<JournalEntry> {
        return try {
            cloudJson?.let(JournalBackupJson::fromJson).orEmpty()
        } catch (_: BackupCorruptException) {
            Log.w(TAG, "Backup file is corrupt; setting it aside")
            driveClient.setAsideBackup(token)
            emptyList()
        }
    }

    /**
     * A corrupt backup throws here and leaves the local journal untouched.
     *
     * Discards local entries in favour of the new account's cloud copy. Used when
     * the signed-in account changes, so one account's entries are never uploaded
     * into another account's Drive.
     */
    suspend fun pullReplace(allowUi: Boolean = false) {
        withContext(Dispatchers.IO) {
            driveClient.reset()
            val token = accessToken(allowUi)
            val cloudJson = driveClient.downloadBackup(token)
            val cloudEntries = cloudJson?.let(JournalBackupJson::fromJson).orEmpty()
            journalRepository.replaceAllEntries(cloudEntries)
        }
    }

    fun forgetAccount() {
        driveClient.reset()
    }

    private companion object {
        const val TAG = "OneLineJournal"
    }
}
