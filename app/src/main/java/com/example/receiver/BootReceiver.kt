package com.example.receiver

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AthkarReminderScheduler
import com.example.data.HumanAzkarScheduler
import com.example.data.NotificationHelper
import com.example.data.ReminderScheduler

/**
 * يعيد جدولة كل التذكيرات بعد إعادة تشغيل الجهاز أو تحديث التطبيق أو تغيير الوقت.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
            HumanAzkarScheduler.rescheduleIfEnabled(context)
            return
        }
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            NotificationHelper.createChannels(context)
            ReminderScheduler.scheduleAll(context)
            AthkarReminderScheduler.syncFromMirror(context)
            HumanAzkarScheduler.rescheduleIfEnabled(context)
        }
    }
}
