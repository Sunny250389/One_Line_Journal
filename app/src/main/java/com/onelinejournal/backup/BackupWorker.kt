package com.onelinejournal.backup

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.onelinejournal.auth.DriveAuthRequiredException
import com.onelinejournal.auth.GoogleAccountSession
import com.onelinejournal.data.JournalDatabase
import com.onelinejournal.data.JournalRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * Retries a failed Drive backup in the background once the device is online.
 * Never logs journal text: only the exception type and its (status-code) message.
 */
class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = applicationContext.getSharedPreferences(BackupPrefs.SETTINGS_FILE, Context.MODE_PRIVATE)
        val owner = applicationContext.getSharedPreferences(BackupPrefs.OWNER_FILE, Context.MODE_PRIVATE)

        val email = settings.getString(GoogleAccountSession.EMAIL_KEY, null)
        // Signed out since the failure: nothing to back up.
        if (email.isNullOrBlank()) return Result.success()
        // The signed-in account doesn't own this journal; the sign-in flow resolves that,
        // so never push local entries into the wrong account's Drive from here.
        val ownerEmail = owner.getString(BackupPrefs.OWNER_EMAIL_KEY, null)
        if (ownerEmail != null && ownerEmail != email) return Result.success()

        return try {
            val token = GoogleAccountSession.silentAccessToken(applicationContext, email)
            val database = JournalDatabase.getInstance(applicationContext)
            val repository = JournalBackupRepository(
                journalRepository = JournalRepository(database.journalEntryDao()),
                driveClient = DriveAppDataClient(),
                accessToken = { token }
            )
            repository.pullMergePush()
            settings.edit()
                .putLong(BackupPrefs.LAST_BACKUP_KEY, System.currentTimeMillis())
                .putBoolean(BackupPrefs.PENDING_KEY, false)
                .apply()
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: DriveAuthRequiredException) {
            // Retrying can't help until the user signs in again.
            Log.w(TAG, "Background backup needs sign-in")
            Result.failure()
        } catch (error: Exception) {
            Log.w(TAG, "Background backup failed, will retry: ${error.javaClass.simpleName} ${error.message}")
            Result.retry()
        }
    }

    private companion object {
        const val TAG = "OneLineJournal"
    }
}
