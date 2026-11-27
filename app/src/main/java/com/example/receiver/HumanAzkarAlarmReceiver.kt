package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.HumanAzkarAudioCatalog
import com.example.data.HumanAzkarScheduler
import com.example.data.NotificationHelper
import com.example.service.AudioAzkarPlaybackService
import com.example.service.PlaybackWakeLock
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class HumanAzkarAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext
        if (!HumanAzkarScheduler.isEnabled(appContext)) return

        // إبقاء المعالج مستيقظًا فورًا حتى انتهاء تسليم الصوت أو بدء الخدمة في قفل الشاشة
        PlaybackWakeLock.acquire(appContext, 45_000L)

        val resourceName = HumanAzkarScheduler.takeNextTrackResource(appContext)
        val track = HumanAzkarAudioCatalog.trackByResourceName(resourceName)
        val trackTitle = track?.title ?: "أذكار صوتية بشرية"

        var serviceStarted = false
        try {
            val playbackIntent = Intent(appContext, AudioAzkarPlaybackService::class.java).apply {
                putExtra(AudioAzkarPlaybackService.EXTRA_TITLE, trackTitle)
                putExtra(AudioAzkarPlaybackService.EXTRA_AUDIO_RESOURCE_NAME, resourceName)
            }
            ContextCompat.startForegroundService(appContext, playbackIntent)
            serviceStarted = true
        } catch (error: Exception) {
            Log.w(TAG, "Foreground service blocked in background; playing human azkar via goAsync fallback", error)
        } finally {
            // جدولة الموعد التالي من لحظة التسليم الفعلية
            HumanAzkarScheduler.rescheduleIfEnabled(appContext, forceNew = true)
        }

        if (!serviceStarted) {
            val rawResId = track?.rawResId ?: 0
            if (rawResId != 0) {
                playFallbackInReceiver(appContext, trackTitle, resourceName, rawResId)
            } else {
                PlaybackWakeLock.release()
                runCatching {
                    NotificationHelper.showHumanAzkarFallback(
                        context = appContext,
                        id = (System.currentTimeMillis() and 0x7fffffff).toInt(),
                        title = "حان وقت الذكر: $trackTitle",
                        body = "مقطع الأذكار الصوتية البشرية.",
                        resourceName = resourceName
                    )
                }
            }
        }
    }

    /**
     * مشغّل احتياطي مباشر داخل goAsync عندما تمنع بعض الأجهزة تشغيل خدمة أمامية في الخلفية:
     * ينطق المقطع البشري فعليًا في قفل الشاشة دون انقطاع، ثم يحرر القفل بأمان.
     */
    private fun playFallbackInReceiver(
        appContext: Context,
        trackTitle: String,
        resourceName: String,
        rawResId: Int
    ) {
        val pendingResult = goAsync()
        thread(start = true, name = "HumanAzkarFallbackPlayer") {
            val doneLatch = CountDownLatch(1)
            var player: MediaPlayer? = null
            try {
                runCatching {
                    NotificationHelper.showHumanAzkarTextBanner(appContext)
                }
                val mp = MediaPlayer().apply {
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
                    appContext.resources.openRawResourceFd(rawResId).use { afd ->
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    }
                    prepare()
                    start()
                }
                player = mp
                doneLatch.await(11, TimeUnit.SECONDS)
            } catch (e: Exception) {
                Log.e(TAG, "Direct receiver fallback failed for $resourceName", e)
                runCatching {
                    NotificationHelper.showHumanAzkarFallback(
                        context = appContext,
                        id = FALLBACK_NOTIF_ID,
                        title = "حان وقت الذكر: $trackTitle",
                        body = "تشغيل التسجيل البشري عبر الإشعار.",
                        resourceName = resourceName
                    )
                }
            } finally {
                runCatching {
                    player?.apply {
                        if (isPlaying) stop()
                        release()
                    }
                }
                PlaybackWakeLock.release()
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "HumanAzkarAlarmReceiver"
        const val FALLBACK_NOTIF_ID = 7043
    }
}
