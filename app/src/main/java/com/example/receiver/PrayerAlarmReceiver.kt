package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.NotificationHelper
import com.example.data.PrayerAlarmScheduler

/**
 * يستقبل منبّه كل صلاة عند حلول وقتها ويعرض الإشعار ثم يُعيد الجدولة لليوم التالي.
 */
class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("prayer_id", 5100)
        val title = intent.getStringExtra("prayer_title") ?: "حان موعد الصلاة"
        val body = intent.getStringExtra("prayer_body") ?: "أقم صلاتك تنعم بحياتك."

        NotificationHelper.showPrayerNotification(context, id, title, body)
        PrayerAlarmScheduler.scheduleAll(context)
    }
}
