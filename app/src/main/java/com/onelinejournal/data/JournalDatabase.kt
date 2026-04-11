package com.onelinejournal.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [JournalEntry::class],
    version = 1,
    exportSchema = false
)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalEntryDao(): JournalEntryDao

    companion object {
        @Volatile
        private var INSTANCE: JournalDatabase? = null

        fun getInstance(context: Context): JournalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    JournalDatabase::class.java,
                    "one_line_journal.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
