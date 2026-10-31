package com.onelinejournal.backup

import com.onelinejournal.data.JournalEntry
import com.onelinejournal.data.JournalEntryDao
import com.onelinejournal.data.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

private const val SECRET = "my private journal text"
private const val TRUNCATED = "{\"entries\":[{\"date\":\"2026-01-01\",\"content\":\"$SECRET\""

class CorruptBackupTest {

    @Test
    fun corruptFileThrowsWithoutQuotingTheJournal() {
        val truncated = assertThrows(BackupCorruptException::class.java) {
            JournalBackupJson.fromJson(TRUNCATED)
        }
        assertFalse(truncated.message.orEmpty().contains(SECRET))
    }

    @Test
    fun validJsonWithoutEntriesIsCorruptRatherThanEmpty() {
        assertThrows(BackupCorruptException::class.java) {
            JournalBackupJson.fromJson("{\"version\":1}")
        }
    }

    @Test
    fun validBackupStillRoundTrips() {
        val entry = JournalEntry("2026-01-01", "hello", 5L, true)
        assertEquals(listOf(entry), JournalBackupJson.fromJson(JournalBackupJson.toJson(listOf(entry))))
    }

    @Test
    fun syncSetsCorruptFileAsideAndUploadsLocalJournal() = runBlocking {
        val local = JournalEntry("2026-02-02", "local entry", 5L, false)
        val dao = FakeDao(mutableListOf(local))
        val drive = FakeDrive(backup = TRUNCATED)

        repository(dao, drive).pullMergePush()

        assertEquals(DriveAppDataClient.CORRUPT_FILE_NAME, drive.renamedTo)
        assertEquals(listOf(local), JournalBackupJson.fromJson(drive.created.orEmpty()))
        assertEquals(listOf(local), dao.entries)
    }

    @Test
    fun accountSwitchKeepsLocalEntriesWhenBackupIsCorrupt() {
        val local = JournalEntry("2026-02-02", "local entry", 5L, false)
        val dao = FakeDao(mutableListOf(local))
        val drive = FakeDrive(backup = TRUNCATED)

        assertThrows(BackupCorruptException::class.java) {
            runBlocking { repository(dao, drive).pullReplace() }
        }

        assertEquals(listOf(local), dao.entries)
        assertEquals(null, drive.renamedTo)
    }

    @Test
    fun healthyBackupIsMergedAndNotSetAside() = runBlocking {
        val cloud = JournalEntry("2026-01-01", "cloud entry", 1L, false)
        val local = JournalEntry("2026-02-02", "local entry", 5L, false)
        val dao = FakeDao(mutableListOf(local))
        val drive = FakeDrive(backup = JournalBackupJson.toJson(listOf(cloud)))

        repository(dao, drive).pullMergePush()

        assertEquals(null, drive.renamedTo)
        assertTrue(dao.entries.containsAll(listOf(cloud, local)))
        assertTrue(JournalBackupJson.fromJson(drive.updated.orEmpty()).containsAll(listOf(cloud, local)))
    }

    private fun repository(dao: FakeDao, drive: FakeDrive) = JournalBackupRepository(
        journalRepository = JournalRepository(dao),
        driveClient = DriveAppDataClient(OkHttpClient.Builder().addInterceptor(drive).build()),
        accessToken = { "token" }
    )
}

/** Stands in for the Drive REST API, with one file until it is renamed. */
private class FakeDrive(private val backup: String) : Interceptor {
    var renamedTo: String? = null
    var created: String? = null
    var updated: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        val sent = request.body?.let { body -> Buffer().also(body::writeTo).readUtf8() }
        val reply = when {
            request.method == "GET" && "alt=media" in url -> backup
            request.method == "GET" ->
                if (renamedTo == null) "{\"files\":[{\"id\":\"file1\"}]}" else "{\"files\":[]}"
            request.method == "PATCH" && "/upload/" in url -> {
                updated = sent
                "{}"
            }
            request.method == "PATCH" -> {
                renamedTo = JSONObject(sent.orEmpty()).getString("name")
                "{}"
            }
            else -> {
                // Multipart create: the journal JSON is the part after the metadata.
                created = sent.orEmpty().substringAfter("\"entries\"").let { "{\"entries\"$it" }
                    .substringBefore("\r\n--")
                "{\"id\":\"file2\"}"
            }
        }
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(reply.toResponseBody("application/json".toMediaType()))
            .build()
    }
}

private class FakeDao(val entries: MutableList<JournalEntry>) : JournalEntryDao {
    override fun observeAllEntries(): Flow<List<JournalEntry>> = flowOf(entries)
    override suspend fun getAllEntries(): List<JournalEntry> = entries.toList()
    override suspend fun updateFavorite(date: String, isFavorite: Boolean) = Unit
    override suspend fun upsertEntry(entry: JournalEntry) = upsertEntries(listOf(entry))
    override suspend fun upsertEntries(entries: List<JournalEntry>) {
        entries.forEach { entry ->
            this.entries.removeAll { it.date == entry.date }
            this.entries += entry
        }
    }
    override suspend fun deleteAllEntries() = entries.clear()
    override suspend fun replaceAllEntries(entries: List<JournalEntry>) {
        deleteAllEntries()
        upsertEntries(entries)
    }
}
