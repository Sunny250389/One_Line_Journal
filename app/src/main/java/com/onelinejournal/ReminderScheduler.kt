package com.onelinejournal

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object ReminderScheduler {
    private const val SETTINGS_FILE = "journal_settings"
    private const val REMINDER_TIME_KEY = "reminder_time"

    /**
     * Arms one alarm for the next [time] (HH:mm). It is scheduled as a clock alarm, which the
     * system delivers on time even when the app is frozen or in Doze. ReminderReceiver arms the
     * following day each time it fires.
     */
    fun scheduleDaily(context: Context, time: String) {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return
        val triggerAt = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }.timeInMillis
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Tapping the alarm icon in the status bar opens the app.
        val showApp = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAt, showApp),
                reminderIntent(context)
            )
        } catch (error: SecurityException) {
            // Exact alarms are not allowed; a regular alarm still fires, possibly a little late.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, reminderIntent(context))
        }
    }

    /** Android 12+ requires the user to allow exact alarms for this app. */
    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    /** Arms tomorrow's reminder from the saved time. Does nothing once the reminder is off. */
    fun scheduleNextFromPrefs(context: Context) {
        val time = context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE)
            .getString(REMINDER_TIME_KEY, null) ?: return
        scheduleDaily(context, time)
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = reminderIntent(context)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun reminderIntent(context: Context): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
