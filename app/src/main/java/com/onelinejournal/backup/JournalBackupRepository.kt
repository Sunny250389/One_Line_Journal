package com.onelinejournal.backup

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
            val cloudEntries = cloudJson?.let(JournalBackupJson::fromJson).orEmpty()
            val localEntries = journalRepository.getEntries()
            val merged = JournalBackupJson.merge(localEntries, cloudEntries)
            journalRepository.upsertEntries(merged)
            driveClient.uploadBackup(token, JournalBackupJson.toJson(merged))
        }
    }

    /**
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
}
