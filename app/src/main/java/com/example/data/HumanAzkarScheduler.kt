package com.example.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.example.receiver.HumanAzkarAlarmReceiver
import com.example.service.AudioAzkarPlaybackService

/** Schedules the user's explicitly enabled human-recorded azkar playback. */
object HumanAzkarScheduler {
    const val MIN_INTERVAL_MINUTES = 1
    const val MAX_INTERVAL_MINUTES = 1440
    const val DEFAULT_INTERVAL_MINUTES = 30

    private const val PREFS_NAME = "rim_human_azkar_schedule"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_INTERVAL_MINUTES = "interval_minutes"
    private const val KEY_NEXT_TRACK_INDEX = "next_track_index"
    private const val REQUEST_CODE = 9160
    const val ACTION_ALARM = "com.example.action.HUMAN_AZKAR_INTERVAL_ALARM"

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun intervalMinutes(context: Context): Int = prefs(context)
        .getInt(KEY_INTERVAL_MINUTES, DEFAULT_INTERVAL_MINUTES)
        .coerceIn(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES)

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) {
            scheduleNext(context)
        } else {
            cancel(context)
            context.stopService(Intent(context, AudioAzkarPlaybackService::class.java))
        }
    }

    fun setIntervalMinutes(context: Context, minutes: Int) {
        val safeMinutes = minutes.coerceIn(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES)
        prefs(context).edit().putInt(KEY_INTERVAL_MINUTES, safeMinutes).apply()
        if (isEnabled(context)) scheduleNext(context)
    }

    /** Restarts the interval after a reboot, package update, or device time change. */
    fun rescheduleIfEnabled(context: Context) {
        if (isEnabled(context)) scheduleNext(context)
    }

    /** Selects clips in order and wraps around after all 28 have played. */
    fun takeNextTrackResource(context: Context): String {
        val sharedPrefs = prefs(context)
        val index = sharedPrefs.getInt(KEY_NEXT_TRACK_INDEX, 0)
            .coerceIn(0, HumanAzkarAudioCatalog.tracks.lastIndex)
        sharedPrefs.edit()
            .putInt(KEY_NEXT_TRACK_INDEX, (index + 1) % HumanAzkarAudioCatalog.tracks.size)
            .apply()
        return HumanAzkarAudioCatalog.trackAt(index).resourceName
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.cancel(pendingIntent(context))
    }

    private fun scheduleNext(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = pendingIntent(context)
        val triggerAt = SystemClock.elapsedRealtime() +
            intervalMinutes(context).toLong() * 60_000L

        try {
            if (canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // Keep a best-effort reminder if exact-alarm access was revoked during scheduling.
            runCatching {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
        }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, HumanAzkarAlarmReceiver::class.java).apply {
            action = ACTION_ALARM
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
