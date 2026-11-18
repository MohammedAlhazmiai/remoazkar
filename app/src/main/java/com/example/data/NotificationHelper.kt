package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {

    const val CHANNEL_REMINDERS = "rim_reminders"
    const val CHANNEL_DAILY = "rim_daily"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val reminders = NotificationChannel(
            CHANNEL_REMINDERS,
            "تذكيرات الأذكار",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "تنبيهات الأذكار التي تحددها بنفسك"
            enableVibration(true)
        }

        val daily = NotificationChannel(
            CHANNEL_DAILY,
            "الأذكار اليومية",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "أذكار الصباح والمساء وسورة الكهف والملك"
        }

        nm.createNotificationChannel(reminders)
        nm.createNotificationChannel(daily)
    }

    fun show(context: Context, id: Int, title: String, body: String, channel: String = CHANNEL_REMINDERS) {
        createChannels(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        val pi = PendingIntent.getActivity(context, id, intent, flags)

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)

        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (_: SecurityException) {
            // إذن الإشعارات غير ممنوح
        }
    }
}
