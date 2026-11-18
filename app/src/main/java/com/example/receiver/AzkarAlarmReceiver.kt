package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AthkarReminderScheduler
import com.example.data.PreferencesManager

/**
 * يستقبل منبّه وقت الأذكار اليومي الذي يحدده المستخدم يدويًا،
 * فيُطلق عملًا فوريًا لعرض الإشعار ثم يعيد جدولة المنبّه لليوم التالي.
 */
class AzkarAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AthkarReminderScheduler.runOnceNow(context)
        val time = PreferencesManager.azkarTime(context)
        AthkarReminderScheduler.scheduleDailyAlarm(context, time.first, time.second)
    }
}
