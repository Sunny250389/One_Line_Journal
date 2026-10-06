package com.onelinejournal.backup

import com.onelinejournal.data.JournalEntry
import org.json.JSONArray
import org.json.JSONObject

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
        val root = JSONObject(text)
        val array = root.optJSONArray("entries") ?: return emptyList()
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
