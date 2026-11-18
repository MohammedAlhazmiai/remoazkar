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

        NotificationHelper.show(context, id, title, body)

        // إعادة الجدولة للتذكيرات اليومية/الأسبوعية
        val reminder = ReminderScheduler.load(context).firstOrNull { it.id == id }
        if (reminder != null && reminder.enabled) {
            ReminderScheduler.schedule(context, reminder)
        }
    }
}
