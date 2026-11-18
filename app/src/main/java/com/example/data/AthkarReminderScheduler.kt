package com.example.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.receiver.AzkarAlarmReceiver
import com.example.worker.AthkarWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * يُشغّل إشعارات الأذكار التلقائية بشكل حقيقي:
 *  - عمل دوري عبر WorkManager كل عدد دقائق يحدده المستخدم (بحد أدنى 15 دقيقة).
 *  - منبّه يومي دقيق في الوقت الذي يُدخله المستخدم يدويًا (ساعة/دقيقة).
 *  - إمكانية إرسال إشعار تجريبي فوري.
 */
object AthkarReminderScheduler {

    const val UNIQUE_PERIODIC = "athkar_periodic_work"
    const val REQ_DAILY = 9100
    const val ACTION_DAILY = "com.example.AZKAR_DAILY_ALARM"

    /** يزامن الجدولة مع الإعدادات المحفوظة. */
    fun sync(context: Context, enabled: Boolean, intervalMinutes: Int, hour: Int, minute: Int) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(UNIQUE_PERIODIC)
            cancelDailyAlarm(context)
            return
        }

        val interval = intervalMinutes.coerceAtLeast(15).toLong()
        val request = PeriodicWorkRequestBuilder<AthkarWorker>(interval, TimeUnit.MINUTES)
            .setInitialDelay(interval, TimeUnit.MINUTES)
            .addTag("athkar_auto")
            .build()
        wm.enqueueUniquePeriodicWork(UNIQUE_PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)

        scheduleDailyAlarm(context, hour, minute)
    }

    /** يزامن انطلاقًا من القيم المحفوظة في المرآة (SharedPreferences). */
    fun syncFromMirror(context: Context) {
        sync(
            context = context,
            enabled = PreferencesManager.isAutoRemindersEnabled(context),
            intervalMinutes = PreferencesManager.reminderInterval(context),
            hour = PreferencesManager.azkarTime(context).first,
            minute = PreferencesManager.azkarTime(context).second
        )
    }

    fun scheduleDailyAlarm(context: Context, hour: Int, minute: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = dailyPendingIntent(context)
        val trigger = nextDaily(hour, minute)
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

    fun cancelDailyAlarm(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        am.cancel(dailyPendingIntent(context))
    }

    /** يرسل إشعار ذكر فوريًا (للتجربة أو عند ضغط المستخدم). */
    fun runOnceNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<AthkarWorker>()
            .addTag("athkar_auto")
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    private fun dailyPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AzkarAlarmReceiver::class.java).apply {
            action = ACTION_DAILY
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getBroadcast(context, REQ_DAILY, intent, flags)
    }

    private fun nextDaily(hour: Int, minute: Int): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, hour)
        c.set(Calendar.MINUTE, minute)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        if (c.timeInMillis <= System.currentTimeMillis()) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }
}
