package com.example.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.receiver.PrayerAlarmReceiver

/**
 * مجدول تنبيهات الصلوات الخمس.
 * يجدول المنبه الدقيق لكل صلاة مفعّلة حسب الوقت المحسوب (أو المعدّل يدويًا)
 * مطروحًا منه دقائق التذكير المسبق المختارة.
 */
object PrayerAlarmScheduler {

    private const val ACTION_PREFIX = "com.aistudio.zadulmuslim.PRAYER_ALARM_"

    fun scheduleAll(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        if (!PrayerStore.isNotificationsEnabled(context)) {
            cancelAll(context)
            return
        }

        val leadMillis = PrayerStore.leadMinutes(context) * 60_000L
        val now = System.currentTimeMillis()
        val (_, prayers) = PrayerStore.resolveDayPrayers(context)

        prayers.forEach { item ->
            if (!item.enabled) {
                cancelPrayer(context, am, item.type)
                return@forEach
            }
            var trigger = item.nextCalendarAfter(now).timeInMillis - leadMillis
            if (trigger <= now) {
                trigger += 24L * 60L * 60L * 1000L
            }

            val pi = buildPendingIntent(context, item.type, item.formatted12())
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
                } else {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
                }
            } catch (_: SecurityException) {
                am.set(AlarmManager.RTC_WAKEUP, trigger, pi)
            }
        }
    }

    fun cancelAll(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        PrayerType.entries.forEach { type ->
            cancelPrayer(context, am, type)
        }
    }

    private fun cancelPrayer(context: Context, am: AlarmManager, type: PrayerType) {
        val pi = buildPendingIntent(context, type, "")
        am.cancel(pi)
    }

    private fun buildPendingIntent(
        context: Context,
        type: PrayerType,
        formattedTime: String
    ): PendingIntent {
        val lead = PrayerStore.leadMinutes(context)
        val title = if (lead > 0) {
            "اقترب موعد ${type.notificationTitle} (بعد $lead د)"
        } else {
            "حان موعد ${type.notificationTitle}"
        }
        val body = if (formattedTime.isNotBlank()) {
            "${type.notificationBody} (${formattedTime})"
        } else {
            type.notificationBody
        }
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_PREFIX + type.id
            putExtra("prayer_id", type.requestCode)
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
}
