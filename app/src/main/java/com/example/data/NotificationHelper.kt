package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

/**
 * إشعارات التذكير — تعمل من أندرويد 7 (API 24) حتى أحدث إصدار.
 *
 * - من أندرويد 8 (API 26) صار الصوت والاهتزاز مرتبطًا بـ "قناة الإشعار" لا بالإشعار نفسه،
 *   لذلك نُنشئ قناتين لكل نوع: قناة بصوت وقناة صامتة، ونختار القناة حسب إعداد المستخدم.
 * - قبل أندرويد 8 نضبط الصوت والاهتزاز مباشرة على الإشعار.
 * - الإذن POST_NOTIFICATIONS مطلوب من أندرويد 13 ويُطلب من الواجهة.
 */
object NotificationHelper {

    const val CHANNEL_REMINDERS = "rim_reminders"
    const val CHANNEL_DAILY = "rim_daily"
    const val CHANNEL_REMINDERS_SILENT = "rim_reminders_silent"
    const val CHANNEL_DAILY_SILENT = "rim_daily_silent"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val reminders = NotificationChannel(
            CHANNEL_REMINDERS,
            "تذكيرات الأذكار (بصوت)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "تنبيهات الأذكار التي تحددها بنفسك، مع صوت واهتزاز"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
            enableLights(true)
            setSound(soundUri, audioAttrs)
        }

        val remindersSilent = NotificationChannel(
            CHANNEL_REMINDERS_SILENT,
            "تذكيرات الأذكار (صامتة)",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "تنبيهات الأذكار بدون صوت ولا اهتزاز"
            setSound(null, null)
            enableVibration(false)
        }

        val daily = NotificationChannel(
            CHANNEL_DAILY,
            "الأذكار اليومية (بصوت)",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "أذكار الصباح والمساء وسورة الكهف والملك، مع صوت"
            enableVibration(true)
            setSound(soundUri, audioAttrs)
        }

        val dailySilent = NotificationChannel(
            CHANNEL_DAILY_SILENT,
            "الأذكار اليومية (صامتة)",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "أذكار الصباح والمساء وسورة الكهف والملك، بدون صوت"
            setSound(null, null)
            enableVibration(false)
        }

        nm.createNotificationChannel(reminders)
        nm.createNotificationChannel(remindersSilent)
        nm.createNotificationChannel(daily)
        nm.createNotificationChannel(dailySilent)
    }

    /** يختار القناة المناسبة حسب إعداد الصوت في التطبيق. */
    fun resolveChannel(channel: String, soundEnabled: Boolean): String {
        return when (channel) {
            CHANNEL_DAILY -> if (soundEnabled) CHANNEL_DAILY else CHANNEL_DAILY_SILENT
            CHANNEL_DAILY_SILENT -> CHANNEL_DAILY_SILENT
            CHANNEL_REMINDERS_SILENT -> CHANNEL_REMINDERS_SILENT
            else -> if (soundEnabled) CHANNEL_REMINDERS else CHANNEL_REMINDERS_SILENT
        }
    }

    fun show(context: Context, id: Int, title: String, body: String, channel: String = CHANNEL_REMINDERS) {
        createChannels(context)

        val soundEnabled = PreferencesManager.isNotificationSoundEnabled(context)
        val targetChannel = resolveChannel(channel, soundEnabled)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        val pi = PendingIntent.getActivity(context, id, intent, flags)

        val builder = NotificationCompat.Builder(context, targetChannel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(
                if (soundEnabled) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_LOW
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pi)

        if (soundEnabled) {
            builder.setVibrate(longArrayOf(0, 400, 200, 400))
            // قبل أندرويد 8 لا توجد قنوات، فيُضبط الصوت على الإشعار نفسه
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            }
        } else {
            builder.setSilent(true)
            builder.setVibrate(null)
            builder.setSound(null)
        }

        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (_: SecurityException) {
            // إذن الإشعارات غير ممنوح (أندرويد 13+)
        }
    }
}
