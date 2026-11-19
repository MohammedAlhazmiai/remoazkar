package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import java.util.Locale

class AudioAzkarPlaybackService : Service() {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pendingText: String? = null
    private var pendingTitle: String = "الأذكار الصوتية"
    private var speechChunks: List<String> = emptyList()
    private var chunkIndex = 0

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopPlayback()
            return START_NOT_STICKY
        }
        val text = intent?.getStringExtra(EXTRA_TEXT)?.trim().orEmpty()
        if (text.isBlank()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        pendingText = text
        pendingTitle = intent?.getStringExtra(EXTRA_TITLE) ?: "الأذكار الصوتية"
        startForegroundCompat(buildNotification(pendingTitle, text.take(100)))
        if (tts == null) initializeTts() else if (ready) speakPending()
        return START_NOT_STICKY
    }

    private fun initializeTts() {
        tts = TextToSpeech(applicationContext) { status ->
            val engine = tts ?: return@TextToSpeech
            if (status != TextToSpeech.SUCCESS) {
                stopPlayback()
                return@TextToSpeech
            }
            val locale = Locale("ar", "SA")
            val languageStatus = engine.setLanguage(locale)
            if (languageStatus == TextToSpeech.LANG_MISSING_DATA || languageStatus == TextToSpeech.LANG_NOT_SUPPORTED) {
                val fallback = engine.setLanguage(Locale("ar"))
                if (fallback == TextToSpeech.LANG_MISSING_DATA || fallback == TextToSpeech.LANG_NOT_SUPPORTED) {
                    stopPlayback()
                    return@TextToSpeech
                }
            }
            engine.voices
                ?.filter { it.locale.language.equals("ar", ignoreCase = true) }
                ?.sortedWith(
                    compareBy<android.speech.tts.Voice> { it.isNetworkConnectionRequired }
                        .thenByDescending { it.quality }
                        .thenBy { it.latency }
                )
                ?.firstOrNull()
                ?.let { engine.voice = it }
            engine.setSpeechRate(0.95f)
            engine.setPitch(1.0f)
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) {
                    if (chunkIndex + 1 < speechChunks.size) {
                        chunkIndex++
                        speakChunk()
                    } else stopPlayback()
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) = stopPlayback()
                override fun onError(utteranceId: String?, errorCode: Int) = stopPlayback()
            })
            ready = true
            speakPending()
        }
    }

    private fun speakPending() {
        val text = pendingText ?: return
        speechChunks = splitForSpeech(text)
        chunkIndex = 0
        speakChunk()
    }

    private fun speakChunk() {
        val engine = tts ?: return
        val text = speechChunks.getOrNull(chunkIndex) ?: return
        val bundle = android.os.Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f)
        }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, bundle, "background-athkar-$chunkIndex")
    }

    private fun splitForSpeech(text: String, maxLength: Int = 3000): List<String> {
        val out = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            var end = (start + maxLength).coerceAtMost(text.length)
            if (end < text.length) {
                val boundary = text.lastIndexOf(' ', end)
                if (boundary > start + maxLength / 2) end = boundary
            }
            text.substring(start, end).trim().takeIf(String::isNotBlank)?.let(out::add)
            start = end
        }
        return out
    }

    private fun buildNotification(title: String, snippet: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 701, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(this, AudioAzkarPlaybackService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this, 702, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(snippet)
            .setStyle(NotificationCompat.BigTextStyle().bigText(snippet))
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPending)
            .addAction(android.R.drawable.ic_media_pause, "إيقاف", stopPending)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "تشغيل الأذكار الصوتية", NotificationManager.IMPORTANCE_LOW).apply {
                description = "إشعار مستمر أثناء قراءة الأذكار صوتيًا في الخلفية"
                setSound(null, null)
                enableVibration(false)
            }
        )
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopPlayback() {
        tts?.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.example.action.STOP_BACKGROUND_ATHKAR"
        const val EXTRA_TEXT = "com.example.extra.ATHKAR_TEXT"
        const val EXTRA_TITLE = "com.example.extra.ATHKAR_TITLE"
        const val CHANNEL_ID = "rim_athkar_playback"
        const val NOTIFICATION_ID = 7041
    }
}
