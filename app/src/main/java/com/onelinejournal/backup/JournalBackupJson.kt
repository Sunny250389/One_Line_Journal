package com.onelinejournal.backup

import com.onelinejournal.data.JournalEntry
import java.io.IOException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * The Drive backup can't be parsed. The message is fixed because org.json's own messages quote
 * the input, which would put journal text in logs and on screen.
 */
class BackupCorruptException : IOException("The backup in Google Drive can't be read.")

object JournalBackupJson {
    private const val VERSION = 1

    fun toJson(entries: List<JournalEntry>, exportedAt: Long = System.currentTimeMillis()): String {
        val root = JSONObject()
        root.put("version", VERSION)
        root.put("exportedAt", exportedAt)
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("date", entry.date)
                    .put("content", entry.content)
                    .put("updatedAt", entry.updatedAt)
                    .put("isFavorite", entry.isFavorite)
            )
        }
        root.put("entries", array)
        return root.toString()
    }

    fun fromJson(text: String): List<JournalEntry> {
        if (text.isBlank()) return emptyList()
        val root = try {
            JSONObject(text)
        } catch (_: JSONException) {
            throw BackupCorruptException()
        }
        val array = root.optJSONArray("entries") ?: throw BackupCorruptException()
        val entries = ArrayList<JournalEntry>(array.length())
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val date = item.optString("date")
            val content = item.optString("content")
            if (date.isBlank() || content.isBlank()) continue
            entries += JournalEntry(
                date = date,
                content = content,
                updatedAt = item.optLong("updatedAt", 0L),
                isFavorite = item.optBoolean("isFavorite", false)
            )
        }
        return entries
    }

    fun merge(local: List<JournalEntry>, cloud: List<JournalEntry>): List<JournalEntry> {
        val byDate = LinkedHashMap<String, JournalEntry>()
        (local + cloud).forEach { entry ->
            val existing = byDate[entry.date]
            if (existing == null || entry.updatedAt > existing.updatedAt) {
                byDate[entry.date] = entry
            }
        }
        return byDate.values.toList()
    }
}
