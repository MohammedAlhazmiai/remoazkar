package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.NotificationHelper
import com.example.data.PrayerAlarmScheduler
import com.example.data.PrayerStore
import com.example.data.PrayerType
import com.example.service.PlaybackWakeLock

/**
 * يستقبل منبّه كل صلاة عند حلول وقتها:
 * 1) يبقي المعالج مستيقظًا عبر PlaybackWakeLock.
 * 2) يعرض إشعار الصلاة ويُشغّل صوت بداية الأذان مرة واحدة عند التذكير فقط.
 * 3) يُعيد جدولة الصلوات لليوم التالي.
 */
class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        if (!PrayerStore.isNotificationsEnabled(appContext)) return

        val typeId = intent.getStringExtra("prayer_type_id")
        val type = PrayerType.entries.firstOrNull { it.id == typeId }
        if (type != null && !PrayerStore.isPrayerEnabled(appContext, type)) {
            PrayerAlarmScheduler.scheduleAll(appContext)
            return
        }

        val id = intent.getIntExtra("prayer_id", type?.requestCode ?: 5100)
        val title = intent.getStringExtra("prayer_title") ?: "حان موعد الصلاة"
        val body = intent.getStringExtra("prayer_body") ?: "أقم صلاتك تنعم بحياتك."

        PlaybackWakeLock.acquire(appContext, 30_000L)
        val pendingResult = goAsync()

        NotificationHelper.triggerPrayerReminderAlert(
            context = appContext,
            id = id,
            title = title,
            body = body,
            playAdhanStart = true,
            pendingResult = pendingResult
        )

        PrayerAlarmScheduler.scheduleAll(appContext)
    }
}
