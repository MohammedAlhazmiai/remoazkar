package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.NotificationHelper
import com.example.data.ReminderScheduler

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("id", 0)
        val title = intent.getStringExtra("title") ?: "تذكير"
        val body = intent.getStringExtra("body") ?: "حان وقت الذكر"

        val channel = if (
            id == 1 || id == 2 || id == 3 ||
            id == ReminderScheduler.REQ_KAHF || id == ReminderScheduler.REQ_MULK
        ) {
            NotificationHelper.CHANNEL_DAILY
        } else {
            NotificationHelper.CHANNEL_REMINDERS
        }
        NotificationHelper.show(context, id, title, body, channel = channel)

        // إعادة الجدولة: تذكيرات المستخدم تُعاد لليوم التالي، ومنبّهات الكهف والملك تُعاد أسبوعيًا/يوميًا
        if (id == ReminderScheduler.REQ_KAHF || id == ReminderScheduler.REQ_MULK) {
            ReminderScheduler.scheduleDailyFeatures(context)
        } else {
            val reminder = ReminderScheduler.load(context).firstOrNull { it.id == id }
            if (reminder != null && reminder.enabled) {
                ReminderScheduler.schedule(context, reminder)
            }
        }
    }
}
