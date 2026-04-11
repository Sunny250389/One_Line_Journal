package com.onelinejournal

import android.os.Bundle
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
