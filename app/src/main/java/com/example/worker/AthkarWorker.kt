package com.example.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.data.NotificationHelper
import com.example.data.ShortTextAzkarCatalog

/**
 * عمّال الأذكار الدوري:
 * يعرض أذكارًا نصية قصيرة ومختارة في الإشعارات الدورية العامة
 * (أما أذكار الصباح والمساء وأذكار النوم فتظهر في أوقات قراءتها المحددة عبر ReminderScheduler فقط).
 */
class AthkarWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    companion object {
        const val CHANNEL_ID = NotificationHelper.CHANNEL_REMINDERS
        const val NOTIFICATION_ID = 1001
    }

    override fun doWork(): Result {
        val context = applicationContext
        val zikr = ShortTextAzkarCatalog.nextItem(context)

        NotificationHelper.show(
            context = context,
            id = NOTIFICATION_ID,
            title = zikr.title,
            body = zikr.text,
            channel = CHANNEL_ID
        )
        return Result.success()
    }
}
