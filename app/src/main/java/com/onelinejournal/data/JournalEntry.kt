package com.onelinejournal.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey val date: String,
    val content: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
