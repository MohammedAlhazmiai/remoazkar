package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.service.AudioAzkarPlaybackService
import com.example.service.PlaybackWakeLock
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * إشعارات التذكير — تعمل من أندرويد 7 (API 24) حتى أحدث إصدار.
 */
object NotificationHelper {

    const val CHANNEL_REMINDERS = "rim_reminders"
    const val CHANNEL_DAILY = "rim_daily"
    const val CHANNEL_REMINDERS_SILENT = "rim_reminders_silent"
    const val CHANNEL_DAILY_SILENT = "rim_daily_silent"
    const val CHANNEL_HUMAN_AZKAR = "rim_human_azkar_v1"
    const val CHANNEL_HUMAN_AZKAR_SILENT = "rim_human_azkar_silent"
    const val CHANNEL_PRAYERS = "zad_prayers_adhan_v2"
    const val CHANNEL_PRAYERS_SILENT = "zad_prayers_silent"

    private const val TAG = "NotificationHelper"

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
            description = "تنبيه احتياطي إذا تعذّر بدء خدمة تشغيل التسجيل الصوتي"
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

        // قناة إشعارات الصلاة: عالية الأهمية مع اهتزاز وصامتة على القناة نفسها لأن صوت بداية الأذان
        // يُشغَّل مباشرةً عبر مشغل الصوت (AudioAzkarPlaybackService) لتجنّب التداخل أو تكرار الصوت.
        val prayers = NotificationChannel(
            CHANNEL_PRAYERS,
            "مواقيت الصلوات الخمس",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "تنبيهات تلقائية عند حلول مواعيد الصلوات الخمس مع صوت بداية الأذان"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 250, 500)
            enableLights(true)
            setSound(null, null)
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
            CHANNEL_HUMAN_AZKAR_SILENT -> CHANNEL_HUMAN_AZKAR_SILENT
            CHANNEL_REMINDERS_SILENT -> CHANNEL_REMINDERS_SILENT
            else -> if (soundEnabled) CHANNEL_REMINDERS else CHANNEL_REMINDERS_SILENT
        }
    }

    fun showPrayerNotification(context: Context, id: Int, title: String, body: String) {
        show(context, id, title, body, CHANNEL_PRAYERS)
    }

    /**
     * يُطلق تنبيه الصلاة الكامل عند حلول موعد التذكير فقط:
     * 1) يُظهر إشعار الصلاة عالي الأولوية (حتى في شاشة القفل).
     * 2) يُشغّل صوت بداية الأذان (R.raw.adhan_start) مرة واحدة فقط إذا كان مفعّلاً.
     */
    fun triggerPrayerReminderAlert(
        context: Context,
        id: Int,
        title: String,
        body: String,
        playAdhanStart: Boolean = true,
        pendingResult: BroadcastReceiver.PendingResult? = null
    ) {
        val appContext = context.applicationContext
        showPrayerNotification(appContext, id, title, body)

        val shouldPlayAdhan = playAdhanStart &&
            PreferencesManager.isNotificationSoundEnabled(appContext) &&
            PrayerStore.isAdhanSoundEnabled(appContext)

        if (!shouldPlayAdhan) {
            PlaybackWakeLock.release()
            pendingResult?.finish()
            return
        }

        val playbackIntent = Intent(appContext, AudioAzkarPlaybackService::class.java).apply {
            putExtra(AudioAzkarPlaybackService.EXTRA_IS_ADHAN, true)
            putExtra(AudioAzkarPlaybackService.EXTRA_TITLE, title)
            putExtra(AudioAzkarPlaybackService.EXTRA_SUBTITLE, body)
        }

        val serviceStarted = runCatching {
            ContextCompat.startForegroundService(appContext, playbackIntent)
            true
        }.getOrElse { error ->
            Log.w(TAG, "Background FGS denied for Adhan start; playing via direct MediaPlayer fallback", error)
            false
        }

        if (serviceStarted) {
            pendingResult?.finish()
            return
        }

        // خطة احتياطية مباشرة لنطق بداية الأذان (3.2 ثانية) إذا قيّد النظام تشغيل الخدمة الخلفية
        thread(start = true, name = "AdhanStartFallbackPlayer") {
            val doneLatch = CountDownLatch(1)
            var mp: MediaPlayer? = null
            try {
                val player = MediaPlayer().apply {
                    setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    setVolume(1.0f, 1.0f)
                    setOnCompletionListener { doneLatch.countDown() }
                    setOnErrorListener { _, _, _ ->
                        doneLatch.countDown()
                        true
                    }
                    appContext.resources.openRawResourceFd(R.raw.adhan_start).use { afd ->
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    }
                    prepare()
                    start()
                }
                mp = player
                doneLatch.await(8, TimeUnit.SECONDS)
            } catch (e: Exception) {
                Log.e(TAG, "Direct Adhan-start playback failed", e)
            } finally {
                runCatching {
                    mp?.apply {
                        if (isPlaying) stop()
                        release()
                    }
                }
                PlaybackWakeLock.release()
                pendingResult?.finish()
            }
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
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pi)

        val usesChannelAudio = targetChannel != CHANNEL_PRAYERS &&
            targetChannel != CHANNEL_PRAYERS_SILENT &&
            targetChannel != CHANNEL_HUMAN_AZKAR_SILENT &&
            targetChannel != CHANNEL_REMINDERS_SILENT &&
            targetChannel != CHANNEL_DAILY_SILENT

        if (soundEnabled && targetChannel != CHANNEL_HUMAN_AZKAR_SILENT) {
            builder.setVibrate(longArrayOf(0, 400, 200, 400))
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && usesChannelAudio) {
                builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            } else if (targetChannel == CHANNEL_PRAYERS) {
                builder.setSound(null)
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
        val selectedTrack = HumanAzkarAudioCatalog.trackByResourceName(resourceName)
        val resolvedResourceId = selectedTrack?.rawResId
            ?: context.resources.getIdentifier(resourceName, "raw", context.packageName)
                .takeIf { it != 0 }
            ?: R.raw.human_azkar_01
        val resolvedResourceName = selectedTrack?.resourceName ?: "human_azkar_01"
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
                    description = "يشغّل المقطع البشري كتنبيه احتياطي عند تعذّر بدء خدمة الصوت."
                    setSound(soundUri, audioAttrs)
                    enableVibration(false)
                }
            )
        }
        return channelId
    }
}
