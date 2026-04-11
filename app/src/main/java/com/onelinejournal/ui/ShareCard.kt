package com.onelinejournal.ui

import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.core.content.FileProvider
import com.onelinejournal.data.JournalEntry
import java.io.File
import java.io.FileOutputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

private const val CARD_WIDTH = 1080
private const val CARD_HEIGHT = 1350
private const val CARD_PADDING = 104f
private const val BRANDING = "\u2014 One Line Journal"

fun shareJournalEntryCard(context: Context, entry: JournalEntry) {
    val imageUri = createJournalEntryCardUri(context, entry)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, imageUri)
        clipData = ClipData.newUri(context.contentResolver, "Journal card", imageUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(
        Intent.createChooser(shareIntent, "Share journal card")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    )
}

private fun createJournalEntryCardUri(context: Context, entry: JournalEntry): Uri {
    val bitmap = createJournalEntryCard(entry)
    val shareDir = File(context.cacheDir, "shared_cards").apply {
        mkdirs()
    }
    val shareFile = File(shareDir, "journal-${entry.date}.png")

    FileOutputStream(shareFile).use { output ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    }
    bitmap.recycle()

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        shareFile
    )
}

private fun createJournalEntryCard(entry: JournalEntry): Bitmap {
    val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(246, 240, 229)
    }
    canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), backgroundPaint)

    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 252, 246)
    }
    canvas.drawRoundRect(
        RectF(56f, 56f, CARD_WIDTH - 56f, CARD_HEIGHT - 56f),
        48f,
        48f,
        cardPaint
    )

    val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(108, 91, 67)
        textSize = 44f
        letterSpacing = 0.04f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
    }
    val entryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(35, 31, 26)
        textSize = 62f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.NORMAL)
    }
    val brandingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(108, 91, 67)
        textSize = 38f
        letterSpacing = 0.03f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
    }

    var y = 210f
    canvas.drawText(formatShareDate(entry.date), CARD_PADDING, y, datePaint)

    y += 210f
    val quoteLines = wrapText("\"${entry.content}\"", entryPaint, CARD_WIDTH - (CARD_PADDING * 2))
    quoteLines.forEach { line ->
        canvas.drawText(line, CARD_PADDING, y, entryPaint)
        y += entryPaint.textSize + 18f
    }

    canvas.drawText(BRANDING, CARD_PADDING, CARD_HEIGHT - 190f, brandingPaint)

    return bitmap
}

private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
    val lines = mutableListOf<String>()
    var currentLine = ""

    text.split(" ").forEach { word ->
        val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
        if (paint.measureText(candidate) <= maxWidth) {
            currentLine = candidate
        } else {
            if (currentLine.isNotEmpty()) {
                lines += currentLine
            }
            currentLine = word
        }
    }

    if (currentLine.isNotEmpty()) {
        lines += currentLine
    }

    return lines
}

private fun formatShareDate(date: String): String {
    return try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)
        if (parsed == null) {
            date
        } else {
            SimpleDateFormat("MMMM d yyyy", Locale.US).format(parsed)
        }
    } catch (_: ParseException) {
        date
    }
}
