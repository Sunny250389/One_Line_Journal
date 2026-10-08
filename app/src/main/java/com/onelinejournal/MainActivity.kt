package com.onelinejournal

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.onelinejournal.auth.GoogleAccountSession
import com.onelinejournal.backup.BackupPrefs
import com.onelinejournal.backup.BackupScheduler
import com.onelinejournal.backup.DriveAppDataClient
import com.onelinejournal.backup.JournalBackupRepository
import com.onelinejournal.data.JournalDatabase
import com.onelinejournal.data.JournalRepository
import com.onelinejournal.ui.JournalApp
import com.onelinejournal.ui.JournalViewModel
import com.onelinejournal.ui.JournalViewModelFactory
import com.onelinejournal.ui.LockScreen
import com.onelinejournal.ui.purgeSharedCards
import com.onelinejournal.ui.theme.OneLineJournalTheme
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// FragmentActivity (a ComponentActivity) is required by BiometricPrompt.
class MainActivity : FragmentActivity() {
    private lateinit var viewModel: JournalViewModel
    private var promptShowing = false
    private var backgroundedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = JournalDatabase.getInstance(applicationContext)
        val repository = JournalRepository(database.journalEntryDao())
        val preferences = getSharedPreferences(BackupPrefs.SETTINGS_FILE, MODE_PRIVATE)
        val ownerPreferences = getSharedPreferences(BackupPrefs.OWNER_FILE, MODE_PRIVATE)
        val googleSession = GoogleAccountSession(this, preferences)
        val backupRepository = JournalBackupRepository(
            journalRepository = repository,
            driveClient = DriveAppDataClient(),
            accessToken = { allowUi -> googleSession.getAccessToken(allowUi) }
        )
        val viewModelFactory = JournalViewModelFactory(
            repository = repository,
            preferences = preferences,
            ownerPreferences = ownerPreferences,
            backupRepository = backupRepository,
            scheduleBackupRetry = { BackupScheduler.scheduleRetry(applicationContext) },
            cancelBackupRetry = { BackupScheduler.cancelRetry(applicationContext) }
        )
        viewModel = ViewModelProvider(this, viewModelFactory)[JournalViewModel::class.java]

        // Hide the journal from screenshots and the recent-apps preview while the lock is on.
        applyScreenProtection(viewModel.isAppLockEnabled)
        observeLifecycleForLock()

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
            val state by viewModel.uiState.collectAsState()
            val locked by viewModel.isLocked.collectAsState()

            LaunchedEffect(Unit) {
                if (googleSession.isSignedIn()) {
                    viewModel.syncOnStart()
                }
            }
            LaunchedEffect(state.appLockEnabled) {
                applyScreenProtection(state.appLockEnabled)
            }

            OneLineJournalTheme(accentTheme = state.accentTheme) {
                if (locked) {
                    LockScreen(onUnlock = ::requestUnlock)
                } else {
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
                        },
                        onToggleAppLock = ::toggleAppLock
                    )
                }
            }
        }
    }

    /** Lock after the app has been in the background longer than a short grace period. */
    private fun observeLifecycleForLock() {
        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> backgroundedAt = SystemClock.elapsedRealtime()
                    Lifecycle.Event.ON_START -> {
                        val away = SystemClock.elapsedRealtime() - backgroundedAt
                        if (backgroundedAt != 0L && away > LOCK_GRACE_MS) viewModel.lockApp()
                        if (viewModel.isLocked.value) requestUnlock()
                    }
                    else -> Unit
                }
            }
        )
    }

    private fun canAuthenticate(): Boolean {
        return BiometricManager.from(this).canAuthenticate(AUTHENTICATORS) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun requestUnlock() {
        when (BiometricManager.from(this).canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS ->
                authenticate("Unlock One Line Journal") { viewModel.unlockApp() }
            // The phone's screen lock was removed, so the lock can never be passed; switch it
            // off rather than locking the journal forever.
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> viewModel.setAppLockEnabled(false)
            // Anything else (e.g. sensor temporarily unavailable) may pass: stay locked.
            else -> Unit
        }
    }

    private fun toggleAppLock(enable: Boolean) {
        if (!canAuthenticate()) {
            Toast.makeText(
                this,
                "Set up a screen lock (PIN, pattern or fingerprint) in your phone settings first.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        authenticate(if (enable) "Turn on app lock" else "Turn off app lock") {
            viewModel.setAppLockEnabled(enable)
        }
    }

    private fun authenticate(title: String, onSuccess: () -> Unit) {
        if (promptShowing) return
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
        promptShowing = true
        BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    promptShowing = false
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // Cancelled or locked out: stay locked; the lock screen offers a retry button.
                    promptShowing = false
                }
            }
        ).authenticate(info)
    }

    private fun applyScreenProtection(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private companion object {
        const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        const val LOCK_GRACE_MS = 30_000L
    }
}
