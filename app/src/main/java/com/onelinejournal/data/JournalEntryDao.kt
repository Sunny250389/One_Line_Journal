package com.onelinejournal.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalEntryDao {
    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    fun observeAllEntries(): Flow<List<JournalEntry>>

    @Query("UPDATE journal_entries SET isFavorite = :isFavorite WHERE date = :date")
    suspend fun updateFavorite(date: String, isFavorite: Boolean)

    @Upsert
    suspend fun upsertEntry(entry: JournalEntry)
}
