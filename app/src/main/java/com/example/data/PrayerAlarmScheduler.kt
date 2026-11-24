package com.example.data

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.receiver.PrayerAlarmReceiver
import java.util.Calendar

/**
 * مجدول تنبيهات الصلوات الخمس.
 *
 * - يحسب وقت التذكير الدقيق لليوم والغد بعد طرح دقائق التذكير المسبق المختارة.
 * - يستخدم AlarmManager.setAlarmClock عند توفر إذن المنبهات الدقيقة لضمان
 *   عمل تنبيه الصلاة وبداية الأذان في قفل الشاشة، وضع السكون (Doze)، وبعد إزالة التطبيق من المهام.
 */
object PrayerAlarmScheduler {

    private const val ACTION_PREFIX = "com.aistudio.zadulmuslim.PRAYER_ALARM_"
    private const val SHOW_INTENT_BASE = 5200
    private const val TAG = "PrayerAlarmScheduler"

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return am.canScheduleExactAlarms()
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun scheduleAll(context: Context) {
        val appContext = context.applicationContext
        val am = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        if (!PrayerStore.isNotificationsEnabled(appContext)) {
            cancelAll(appContext)
            return
        }

        val leadMinutes = PrayerStore.leadMinutes(appContext)
        val leadMillis = leadMinutes * 60_000L
        val now = System.currentTimeMillis()

        val todayCal = Calendar.getInstance().apply { timeInMillis = now }
        val tomorrowCal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
        }

        val (_, todayPrayers) = PrayerStore.resolveDayPrayers(appContext, todayCal)
        val (_, tomorrowPrayers) = PrayerStore.resolveDayPrayers(appContext, tomorrowCal)

        todayPrayers.forEachIndexed { idx, todayItem ->
            if (!todayItem.enabled) {
                cancelPrayer(appContext, am, todayItem.type)
                return@forEachIndexed
            }

            val tomorrowItem = tomorrowPrayers.getOrNull(idx) ?: todayItem
            val resolvedNext = selectNextPrayerTrigger(
                todayCal = todayCal,
                todayItem = todayItem,
                tomorrowCal = tomorrowCal,
                tomorrowItem = tomorrowItem,
                nowMillis = now,
                leadMillis = leadMillis
            ) ?: return@forEachIndexed

            val (prayerItem, trigger) = resolvedNext
            val pi = buildPendingIntent(appContext, prayerItem.type, prayerItem.formatted12(), leadMinutes)
            val showPi = buildShowPendingIntent(appContext, prayerItem.type)

            if (canScheduleExactAlarms(appContext)) {
                val alarmClockOk = runCatching {
                    am.setAlarmClock(
                        AlarmManager.AlarmClockInfo(trigger, showPi),
                        pi
                    )
                    true
                }.getOrElse { error ->
                    Log.w(TAG, "setAlarmClock failed for ${prayerItem.type.id}", error)
                    false
                }
                if (alarmClockOk) return@forEachIndexed

                val exactOk = runCatching {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
                    true
                }.getOrElse { error ->
                    Log.w(TAG, "setExactAndAllowWhileIdle failed for ${prayerItem.type.id}", error)
                    false
                }
                if (exactOk) return@forEachIndexed
            }

            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, trigger, pi)
                }
            }
        }
    }

    internal fun selectNextPrayerTrigger(
        todayCal: Calendar,
        todayItem: ResolvedPrayerTime,
        tomorrowCal: Calendar,
        tomorrowItem: ResolvedPrayerTime,
        nowMillis: Long,
        leadMillis: Long
    ): Pair<ResolvedPrayerTime, Long>? {
        val todayAt = (todayCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, todayItem.hour)
            set(Calendar.MINUTE, todayItem.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis - leadMillis

        if (todayAt > nowMillis + 1_000L) {
            return todayItem to todayAt
        }

        val tomorrowAt = (tomorrowCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, tomorrowItem.hour)
            set(Calendar.MINUTE, tomorrowItem.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis - leadMillis

        return if (tomorrowAt > nowMillis) tomorrowItem to tomorrowAt else null
    }

    fun cancelAll(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        PrayerType.entries.forEach { type ->
            cancelPrayer(context, am, type)
        }
    }

    private fun cancelPrayer(context: Context, am: AlarmManager, type: PrayerType) {
        val pi = buildPendingIntent(context, type, "", 0)
        am.cancel(pi)
    }

    private fun buildPendingIntent(
        context: Context,
        type: PrayerType,
        formattedTime: String,
        lead: Int
    ): PendingIntent {
        val title = if (lead > 0) {
            "اقترب موعد ${type.notificationTitle} (بعد $lead د)"
        } else {
            "حان موعد ${type.notificationTitle}"
        }
        val body = if (formattedTime.isNotBlank()) {
            "${type.notificationBody} ($formattedTime)"
        } else {
            type.notificationBody
        }
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_PREFIX + type.id
            putExtra("prayer_id", type.requestCode)
            putExtra("prayer_type_id", type.id)
            putExtra("prayer_title", title)
            putExtra("prayer_body", body)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, type.requestCode, intent, flags)
    }

    private fun buildShowPendingIntent(context: Context, type: PrayerType): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, SHOW_INTENT_BASE + type.ordinal, intent, flags)
    }
}
