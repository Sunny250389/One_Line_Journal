package com.onelinejournal.data

import kotlinx.coroutines.flow.Flow

class JournalRepository(
    private val journalEntryDao: JournalEntryDao
) {
    fun observeEntries(): Flow<List<JournalEntry>> = journalEntryDao.observeAllEntries()

    suspend fun saveEntry(entry: JournalEntry) {
        journalEntryDao.upsertEntry(entry)
    }

    suspend fun updateFavorite(date: String, isFavorite: Boolean) {
        journalEntryDao.updateFavorite(date, isFavorite)
    }
}
