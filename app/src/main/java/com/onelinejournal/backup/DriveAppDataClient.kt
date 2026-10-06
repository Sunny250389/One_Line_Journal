package com.onelinejournal.backup

import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class DriveAppDataClient(
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    @Volatile
    private var cachedFileId: String? = null

    suspend fun downloadBackup(accessToken: String): String? = withContext(Dispatchers.IO) {
        val fileId = findBackupFileId(accessToken) ?: return@withContext null
        val request = Request.Builder()
            .url("$DRIVE_FILES/$fileId?alt=media")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        execute(request).takeIf { it.isNotBlank() }
    }

    suspend fun uploadBackup(accessToken: String, json: String) = withContext(Dispatchers.IO) {
        val fileId = findBackupFileId(accessToken)
        if (fileId == null) {
            createBackup(accessToken, json)
        } else {
            updateBackup(accessToken, fileId, json)
        }
    }

    private fun findBackupFileId(accessToken: String): String? {
        cachedFileId?.let { return it }
        val query = URLEncoder.encode("name='$BACKUP_FILE_NAME'", StandardCharsets.UTF_8.name())
        val request = Request.Builder()
            .url("$DRIVE_FILES?spaces=appDataFolder&q=$query&fields=files(id,name)&pageSize=10")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        val body = execute(request)
        val files = JSONObject(body).optJSONArray("files") ?: return null
        if (files.length() == 0) return null
        val fileId = files.getJSONObject(0).optString("id").takeIf { it.isNotBlank() }
        cachedFileId = fileId
        return fileId
    }

    private fun createBackup(accessToken: String, json: String) {
        val metadata = JSONObject()
            .put("name", BACKUP_FILE_NAME)
            .put("mimeType", JSON_MEDIA_TYPE)
            .put("parents", org.json.JSONArray().put("appDataFolder"))
            .toString()
        val boundary = "onelinejournal_${System.currentTimeMillis()}"
        val multipart = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadata)
            append("\r\n--$boundary\r\n")
            append("Content-Type: $JSON_MEDIA_TYPE; charset=UTF-8\r\n\r\n")
            append(json)
            append("\r\n--$boundary--\r\n")
        }
        val request = Request.Builder()
            .url("$DRIVE_UPLOAD?uploadType=multipart")
            .header("Authorization", "Bearer $accessToken")
            .post(multipart.toRequestBody("multipart/related; boundary=$boundary".toMediaType()))
            .build()
        val body = execute(request)
        cachedFileId = JSONObject(body).optString("id").takeIf { it.isNotBlank() }
    }

    private fun updateBackup(accessToken: String, fileId: String, json: String) {
        val request = Request.Builder()
            .url("$DRIVE_UPLOAD/$fileId?uploadType=media")
            .header("Authorization", "Bearer $accessToken")
            .patch(json.toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
            .build()
        execute(request)
    }

    private fun execute(request: Request): String {
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Drive backup failed (${response.code})")
            }
            return body
        }
    }

    companion object {
        const val BACKUP_FILE_NAME = "one_line_journal.json"
        private const val JSON_MEDIA_TYPE = "application/json"
        private const val DRIVE_FILES = "https://www.googleapis.com/drive/v3/files"
        private const val DRIVE_UPLOAD = "https://www.googleapis.com/upload/drive/v3/files"
    }
}
