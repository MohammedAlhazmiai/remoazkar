package com.example.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.example.receiver.HumanAzkarAlarmReceiver
import com.example.service.AudioAzkarPlaybackService

/** Schedules the user's explicitly enabled human-recorded azkar playback. */
object HumanAzkarScheduler {
    const val MIN_INTERVAL_MINUTES = 1
    const val MAX_INTERVAL_MINUTES = 1440
    const val DEFAULT_INTERVAL_MINUTES = 5

    private const val PREFS_NAME = "rim_human_azkar_schedule"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_INTERVAL_MINUTES = "interval_minutes"
    private const val KEY_NEXT_TRACK_INDEX = "next_track_index"
    private const val REQUEST_CODE = 9160
    private const val TAG = "HumanAzkarScheduler"
    const val ACTION_ALARM = "com.example.action.HUMAN_AZKAR_INTERVAL_ALARM"

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun intervalMinutes(context: Context): Int = prefs(context)
        .getInt(KEY_INTERVAL_MINUTES, DEFAULT_INTERVAL_MINUTES)
        .coerceIn(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES)

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return exactAlarmAccessAllowsPlayback(Build.VERSION.SDK_INT, alarmManager.canScheduleExactAlarms())
    }

    internal fun exactAlarmAccessAllowsPlayback(apiLevel: Int, hasExactAlarmAccess: Boolean): Boolean =
        apiLevel < Build.VERSION_CODES.S || hasExactAlarmAccess

    /**
     * Enables periodic playback only when Android can schedule the exact alarm that starts
     * the background media service. Returns false without changing the preference if it cannot.
     */
    fun setEnabled(context: Context, enabled: Boolean): Boolean {
        if (!enabled) {
            prefs(context).edit().putBoolean(KEY_ENABLED, false).apply()
            cancel(context)
            context.stopService(Intent(context, AudioAzkarPlaybackService::class.java))
            return true
        }

        if (!canScheduleExactAlarms(context)) {
            cancel(context)
            return false
        }

        prefs(context).edit().putBoolean(KEY_ENABLED, true).apply()
        if (scheduleNext(context)) return true

        prefs(context).edit().putBoolean(KEY_ENABLED, false).apply()
        cancel(context)
        return false
    }

    fun setIntervalMinutes(context: Context, minutes: Int) {
        val safeMinutes = minutes.coerceIn(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES)
        prefs(context).edit().putInt(KEY_INTERVAL_MINUTES, safeMinutes).apply()
        if (isEnabled(context) && !scheduleNext(context)) cancel(context)
    }

    /** Restarts the interval after a reboot, package update, time change, or permission grant. */
    fun rescheduleIfEnabled(context: Context): Boolean {
        if (!isEnabled(context)) return false
        if (!canScheduleExactAlarms(context)) {
            // Do not replace actual audio playback with an inexact notification and imply success.
            cancel(context)
            return false
        }
        val scheduled = scheduleNext(context)
        if (!scheduled) cancel(context)
        return scheduled
    }

    /** Selects clips in order and wraps around after all available clips have played. */
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

    private fun scheduleNext(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        if (!canScheduleExactAlarms(context)) return false

        val triggerAt = SystemClock.elapsedRealtime() + intervalMinutes(context).toLong() * 60_000L
        return runCatching {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAt,
                pendingIntent(context)
            )
            true
        }.getOrElse { error ->
            Log.e(TAG, "Could not schedule the exact human-azkar alarm", error)
            false
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
