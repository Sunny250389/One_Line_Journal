package com.onelinejournal

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onelinejournal.auth.GoogleAccountSession
import com.onelinejournal.backup.DriveAppDataClient
import com.onelinejournal.backup.JournalBackupRepository
import com.onelinejournal.data.JournalDatabase
import com.onelinejournal.data.JournalRepository
import com.onelinejournal.ui.JournalApp
import com.onelinejournal.ui.JournalViewModel
import com.onelinejournal.ui.JournalViewModelFactory
import com.onelinejournal.ui.theme.OneLineJournalTheme
import kotlin.coroutines.cancellation.CancellationException
import com.onelinejournal.ui.purgeSharedCards
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = JournalDatabase.getInstance(applicationContext)
        val repository = JournalRepository(database.journalEntryDao())
        val preferences = getSharedPreferences("journal_settings", MODE_PRIVATE)
        val ownerPreferences = getSharedPreferences("journal_owner", MODE_PRIVATE)
        val googleSession = GoogleAccountSession(this, preferences)
        val backupRepository = JournalBackupRepository(
            journalRepository = repository,
            driveClient = DriveAppDataClient(),
            accessToken = { allowUi -> googleSession.getAccessToken(allowUi) }
        )
        val viewModelFactory = JournalViewModelFactory(repository, preferences, ownerPreferences, backupRepository)
        lifecycleScope.launch(Dispatchers.IO) { purgeSharedCards(applicationContext) }
        createReminderChannel(this)
        preferences.getString("reminder_time", null)?.let {
            ReminderScheduler.scheduleDaily(this, it)
        }
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        }

        setContent {
            val viewModel: JournalViewModel = viewModel(factory = viewModelFactory)
            val state by viewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                if (googleSession.isSignedIn()) {
                    viewModel.syncOnStart()
                }
            }

            OneLineJournalTheme(accentTheme = state.accentTheme) {
                JournalApp(
                    viewModel = viewModel,
                    onGoogleSignIn = {
                        lifecycleScope.launch {
                            try {
                                val email = googleSession.signIn()
                                viewModel.onGoogleSignedIn(email)
                            } catch (_: CancellationException) {
                            } catch (error: Exception) {
                                viewModel.reportBackupError(error.message)
                            }
                        }
                    },
                    onGoogleSignOut = {
                        lifecycleScope.launch {
                            googleSession.signOut()
                            viewModel.onGoogleSignedOut()
                        }
                    }
                )
            }
        }
    }
}
