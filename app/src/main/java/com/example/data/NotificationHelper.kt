package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

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
    const val CHANNEL_HUMAN_AZKAR = "rim_human_azkar_v1"
    const val CHANNEL_HUMAN_AZKAR_SILENT = "rim_human_azkar_silent"
    const val CHANNEL_PRAYERS = "zad_prayers_v1"
    const val CHANNEL_PRAYERS_SILENT = "zad_prayers_silent"

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

        val humanAzkarUri = Uri.parse("android.resource://${context.packageName}/${R.raw.human_azkar_01}")
        val humanAzkar = NotificationChannel(
            CHANNEL_HUMAN_AZKAR,
            "التسجيلات البشرية للأذكار",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "صوت تسجيل بشري بديل عندما لا يتوفر إذن المنبه الدقيق"
            setSound(
                humanAzkarUri,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            enableVibration(true)
        }
        val humanAzkarSilent = NotificationChannel(
            CHANNEL_HUMAN_AZKAR_SILENT,
            "التسجيلات البشرية للأذكار (صامتة)",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "تنبيهات التسجيلات البشرية بدون صوت أو اهتزاز"
            setSound(null, null)
            enableVibration(false)
        }

        val prayers = NotificationChannel(
            CHANNEL_PRAYERS,
            "مواقيت الصلوات الخمس (بصوت)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "تنبيهات تلقائية عند حلول مواعيد الصلوات الخمس"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 250, 500)
            enableLights(true)
            setSound(soundUri, audioAttrs)
        }

        val prayersSilent = NotificationChannel(
            CHANNEL_PRAYERS_SILENT,
            "مواقيت الصلوات الخمس (صامتة)",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "تنبيهات مواعيد الصلوات الخمس بدون صوت"
            setSound(null, null)
            enableVibration(false)
        }

        nm.createNotificationChannel(reminders)
        nm.createNotificationChannel(remindersSilent)
        nm.createNotificationChannel(daily)
        nm.createNotificationChannel(dailySilent)
        nm.createNotificationChannel(humanAzkar)
        nm.createNotificationChannel(humanAzkarSilent)
        nm.createNotificationChannel(prayers)
        nm.createNotificationChannel(prayersSilent)
    }

    /** يختار القناة المناسبة حسب إعداد الصوت في التطبيق. */
    fun resolveChannel(channel: String, soundEnabled: Boolean): String {
        return when (channel) {
            CHANNEL_PRAYERS -> if (soundEnabled) CHANNEL_PRAYERS else CHANNEL_PRAYERS_SILENT
            CHANNEL_PRAYERS_SILENT -> CHANNEL_PRAYERS_SILENT
            CHANNEL_DAILY -> if (soundEnabled) CHANNEL_DAILY else CHANNEL_DAILY_SILENT
            CHANNEL_DAILY_SILENT -> CHANNEL_DAILY_SILENT
            CHANNEL_REMINDERS_SILENT -> CHANNEL_REMINDERS_SILENT
            else -> if (soundEnabled) CHANNEL_REMINDERS else CHANNEL_REMINDERS_SILENT
        }
    }

    fun showPrayerNotification(context: Context, id: Int, title: String, body: String) {
        show(context, id, title, body, CHANNEL_PRAYERS)
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

    /** Uses a human-recorded resource as the notification sound when background playback is restricted. */
    fun showHumanAzkarFallback(
        context: Context,
        id: Int,
        title: String,
        body: String,
        resourceName: String = "human_azkar_01"
    ) {
        createChannels(context)
        val soundEnabled = PreferencesManager.isNotificationSoundEnabled(context)
        val resolvedResourceId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
            .takeIf { it != 0 } ?: R.raw.human_azkar_01
        val resolvedResourceName = HumanAzkarAudioCatalog.tracks
            .firstOrNull { it.resourceName == resourceName }?.resourceName ?: "human_azkar_01"
        val selectedTrack = HumanAzkarAudioCatalog.tracks
            .firstOrNull { it.resourceName == resolvedResourceName }
        val channel = if (soundEnabled) {
            humanAzkarTrackChannel(
                context = context,
                resourceName = resolvedResourceName,
                resourceId = resolvedResourceId,
                displayName = selectedTrack?.title ?: "تسجيل بشري"
            )
        } else CHANNEL_HUMAN_AZKAR_SILENT
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val openPending = PendingIntent.getActivity(context, id, openIntent, flags)
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .setPriority(if (soundEnabled) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && soundEnabled) {
            builder.setSound(Uri.parse("android.resource://${context.packageName}/$resolvedResourceId"))
        }
        if (!soundEnabled) builder.setSilent(true)

        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (_: SecurityException) {
            // Notification permission can be revoked while an interval is already scheduled.
        }
    }

    private fun humanAzkarTrackChannel(
        context: Context,
        resourceName: String,
        resourceId: Int,
        displayName: String
    ): String {
        val channelId = "${CHANNEL_HUMAN_AZKAR}_$resourceName"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = Uri.parse("android.resource://${context.packageName}/$resourceId")
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "التسجيل البشري — $displayName",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "يشغل المقطع البشري التالي بالتناوب عند تعذر تشغيل الخدمة الصوتية."
                    setSound(soundUri, audioAttrs)
                    enableVibration(false)
                }
            )
        }
        return channelId
    }
}
