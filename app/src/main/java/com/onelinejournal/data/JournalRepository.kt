package com.onelinejournal.data

import kotlinx.coroutines.flow.Flow

class JournalRepository(
    private val journalEntryDao: JournalEntryDao
) {
    fun observeEntries(): Flow<List<JournalEntry>> = journalEntryDao.observeAllEntries()

    suspend fun getEntries(): List<JournalEntry> = journalEntryDao.getAllEntries()

    suspend fun saveEntry(entry: JournalEntry) {
        journalEntryDao.upsertEntry(entry)
    }

    suspend fun upsertEntries(entries: List<JournalEntry>) {
        if (entries.isEmpty()) return
        journalEntryDao.upsertEntries(entries)
    }

    suspend fun updateFavorite(date: String, isFavorite: Boolean) {
        journalEntryDao.updateFavorite(date, isFavorite)
    }
}
