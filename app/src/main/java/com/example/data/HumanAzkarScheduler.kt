package com.example.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.example.MainActivity
import com.example.receiver.HumanAzkarAlarmReceiver
import com.example.service.AudioAzkarPlaybackService

/**
 * مجدول التشغيل الدوري للتسجيلات البشرية للأذكار.
 *
 * يستخدم AlarmManager.setAlarmClock عند توفر إذن المنبهات الدقيقة لضمان:
 * 1) عدم تقييد التنبيهات في وضع السكون (Doze) أثناء قفل الشاشة.
 * 2) السماح لنظام أندرويد 12+ بتشغيل خدمة الصوت في الخلفية حتى بعد إزالة التطبيق من شاشة المهام.
 */
object HumanAzkarScheduler {
    const val MIN_INTERVAL_MINUTES = 1
    const val MAX_INTERVAL_MINUTES = 1440
    const val DEFAULT_INTERVAL_MINUTES = 5

    private const val PREFS_NAME = "rim_human_azkar_schedule"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_INTERVAL_MINUTES = "interval_minutes"
    private const val KEY_NEXT_TRACK_INDEX = "next_track_index"
    private const val KEY_NEXT_TRIGGER_AT = "next_trigger_wall_ms"
    private const val REQUEST_CODE = 9160
    private const val SHOW_INTENT_REQUEST_CODE = 9161
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

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    internal fun exactAlarmAccessAllowsPlayback(apiLevel: Int, hasExactAlarmAccess: Boolean): Boolean =
        apiLevel < Build.VERSION_CODES.S || hasExactAlarmAccess

    /**
     * يفعّل التشغيل الدوري عندما تتوفر صلاحية المنبه الدقيق ويُجدول الموعد القادم بـ setAlarmClock.
     */
    fun setEnabled(context: Context, enabled: Boolean): Boolean {
        if (!enabled) {
            prefs(context).edit()
                .putBoolean(KEY_ENABLED, false)
                .remove(KEY_NEXT_TRIGGER_AT)
                .apply()
            cancel(context)
            context.stopService(Intent(context, AudioAzkarPlaybackService::class.java))
            return true
        }

        if (!canScheduleExactAlarms(context)) {
            cancel(context)
            return false
        }

        prefs(context).edit().putBoolean(KEY_ENABLED, true).apply()
        if (scheduleNext(context, forceNew = true)) return true

        prefs(context).edit()
            .putBoolean(KEY_ENABLED, false)
            .remove(KEY_NEXT_TRIGGER_AT)
            .apply()
        cancel(context)
        return false
    }

    fun setIntervalMinutes(context: Context, minutes: Int) {
        val safeMinutes = minutes.coerceIn(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES)
        prefs(context).edit().putInt(KEY_INTERVAL_MINUTES, safeMinutes).apply()
        if (isEnabled(context)) {
            scheduleNext(context, forceNew = true)
        }
    }

    /**
     * يُعيد تثبيت المنبه القادم عند الإقلاع، استبدال الحزمة، إزالة التطبيق من المهام، أو استلام التنبيه.
     * - عند [forceNew] = false يُحافظ على الموعد المستقبلي المخزّن إن كان صالحًا حتى لا تتأخر الدورة عند فتح التطبيق.
     * - حتى إذا سُحبت صلاحية المنبه الدقيق لاحقًا، يُجدول احتياطيًا عبر setAndAllowWhileIdle دون إلغاء الإعداد.
     */
    fun rescheduleIfEnabled(context: Context, forceNew: Boolean = false): Boolean {
        if (!isEnabled(context)) return false
        return scheduleNext(context, forceNew = forceNew)
    }

    /** يختار المقطع التالي بالتسلسل الدوري. */
    fun takeNextTrackResource(context: Context): String {
        val sharedPrefs = prefs(context)
        val index = sharedPrefs.getInt(KEY_NEXT_TRACK_INDEX, 0)
            .coerceIn(0, HumanAzkarAudioCatalog.tracks.lastIndex)
        sharedPrefs.edit()
            .putInt(KEY_NEXT_TRACK_INDEX, (index + 1) % HumanAzkarAudioCatalog.tracks.size)
            .commit()
        return HumanAzkarAudioCatalog.trackAt(index).resourceName
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.cancel(pendingIntent(context))
    }

    private fun scheduleNext(context: Context, forceNew: Boolean): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val now = System.currentTimeMillis()
        val intervalMs = intervalMinutes(context).toLong() * 60_000L
        val storedTrigger = prefs(context).getLong(KEY_NEXT_TRIGGER_AT, 0L)
        val triggerAt = if (!forceNew && storedTrigger > now + 5_000L && storedTrigger <= now + intervalMs + 60_000L) {
            storedTrigger
        } else {
            now + intervalMs
        }
        prefs(context).edit().putLong(KEY_NEXT_TRIGGER_AT, triggerAt).apply()

        val triggerPi = pendingIntent(context)
        val showPi = showAppPendingIntent(context)

        if (canScheduleExactAlarms(context)) {
            val alarmClockSuccess = runCatching {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAt, showPi),
                    triggerPi
                )
                true
            }.getOrElse { error ->
                Log.w(TAG, "setAlarmClock failed, trying setExactAndAllowWhileIdle", error)
                false
            }
            if (alarmClockSuccess) return true

            val exactSuccess = runCatching {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    triggerPi
                )
                true
            }.getOrElse { error ->
                Log.e(TAG, "Could not schedule exact human-azkar alarm", error)
                false
            }
            if (exactSuccess) return true
        }

        // خطة احتياطية عندما تكون صلاحية المنبه الدقيق غير متاحة مؤقتًا
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, triggerPi)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, triggerPi)
            }
            true
        }.getOrElse { error ->
            Log.e(TAG, "Fallback inexact human-azkar alarm failed", error)
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

    private fun showAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, SHOW_INTENT_REQUEST_CODE, intent, flags)
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
