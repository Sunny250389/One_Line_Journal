package com.onelinejournal

import android.Manifest
import android.os.Bundle
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onelinejournal.data.JournalDatabase
import com.onelinejournal.data.JournalRepository
import com.onelinejournal.ui.JournalApp
import com.onelinejournal.ui.JournalViewModel
import com.onelinejournal.ui.JournalViewModelFactory
import com.onelinejournal.ui.theme.OneLineJournalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = JournalDatabase.getInstance(applicationContext)
        val repository = JournalRepository(database.journalEntryDao())
        val preferences = getSharedPreferences("journal_settings", MODE_PRIVATE)
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
            val viewModel: JournalViewModel = viewModel(
                factory = JournalViewModelFactory(repository, preferences)
            )
            val state by viewModel.uiState.collectAsState()

            OneLineJournalTheme(accentTheme = state.accentTheme) {
                JournalApp(viewModel = viewModel)
            }
        }
    }
}
