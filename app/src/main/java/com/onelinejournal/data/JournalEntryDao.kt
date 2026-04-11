package com.onelinejournal.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalEntryDao {
    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    fun observeAllEntries(): Flow<List<JournalEntry>>

    @Upsert
    suspend fun upsertEntry(entry: JournalEntry)
}
