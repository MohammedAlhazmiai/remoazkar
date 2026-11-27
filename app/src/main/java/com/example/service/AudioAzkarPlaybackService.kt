package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.HumanAzkarAudioCatalog
import com.example.data.HumanAzkarScheduler
import com.example.data.NotificationHelper
import com.example.data.PrayerAlarmScheduler
import java.util.Locale

/**
 * خدمة تشغيل الأذكار الصوتية وبداية الأذان في الخلفية.
 *
 * - التسجيلات البشرية تُشغَّل مقطعًا واحدًا في كل مرة، ثم يتوقف التشغيل، ويعود الدور إلى
 *   المجدول الزمني (HumanAzkarScheduler) لتشغيل المقطع التالي بعد الفاصل المحدد.
 * - صوت بداية الأذان (adhan_start) يُشغَّل مرة واحدة فقط عند حلول تذكير الصلاة.
 * - تدعم الاستمرار في قفل الشاشة وعند إزالة التطبيق من شاشة المهام عبر MediaSession + WakeLock +
 *   android:stopWithTask="false".
 */
class AudioAzkarPlaybackService : Service() {

    private var tts: TextToSpeech? = null
    private var ready = false
    private var pendingText: String? = null
    private var pendingTitle: String = DEFAULT_TITLE
    private var speechChunks: List<String> = emptyList()
    private var chunkIndex = 0
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var mediaSession: MediaSession? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var clipWatchdogRunnable: Runnable? = null

    /** نوع التشغيل الحالي: تسجيل بشري أو بداية أذان أو نص عبر TTS. */
    private var playingHuman = false
    private var playingAdhan = false
    private var humanIndex = 0

    private val prefs by lazy {
        applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    /* ----------------------------- WakeLock ----------------------------- */

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "RimAzkar:AudioWakeLock"
            ).apply { setReferenceCounted(false) }
        }
        renewWakeLock()
    }

    /** يُجدَّد عند كل مقطع حتى لا ينتهي أثناء التشغيل في قفل الشاشة. */
    private fun renewWakeLock() {
        try {
            wakeLock?.acquire(60 * 60 * 1000L)
        } catch (_: Exception) {
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) {
        }
        wakeLock = null
        PlaybackWakeLock.release()
    }

    /* --------------------------- MediaSession --------------------------- */

    private fun updateMediaSession(title: String, playing: Boolean) {
        runCatching {
            val session = mediaSession ?: MediaSession(applicationContext, "ZadAlMuslimAzkarSession").also {
                it.setFlags(
                    MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
                )
                it.setCallback(object : MediaSession.Callback() {
                    override fun onStop() {
                        stopPlayback()
                    }

                    override fun onPause() {
                        stopPlayback()
                    }
                })
                mediaSession = it
            }
            session.setMetadata(
                MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                    .build()
            )
            val state = if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_STOPPED
            session.setPlaybackState(
                PlaybackState.Builder()
                    .setActions(PlaybackState.ACTION_STOP or PlaybackState.ACTION_PAUSE)
                    .setState(state, 0L, 1.0f)
                    .build()
            )
            session.isActive = playing
        }
    }

    private fun releaseMediaSession() {
        val session = mediaSession
        mediaSession = null
        if (session != null) {
            runCatching {
                session.isActive = false
                session.release()
            }
        }
    }

    /* --------------------------- onStartCommand -------------------------- */

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopPlayback()
            return START_NOT_STICKY
        }

        // إعادة تشغيل الخدمة من النظام بعد إيقافها (intent == null): نستأنف آخر حالة محفوظة.
        if (intent == null) {
            restoreFromPrefs()
            return START_STICKY
        }

        pendingTitle = intent.getStringExtra(EXTRA_TITLE)?.takeIf { it.isNotBlank() } ?: DEFAULT_TITLE

        if (intent.getBooleanExtra(EXTRA_IS_ADHAN, false)) {
            val subtitle = intent.getStringExtra(EXTRA_SUBTITLE)
                ?.takeIf { it.isNotBlank() }
                ?: "بداية الأذان — الله أكبر، الله أكبر"
            acquireWakeLock()
            playAdhanStartClip(pendingTitle, subtitle)
            return START_NOT_STICKY
        }

        val audioResourceName = intent.getStringExtra(EXTRA_AUDIO_RESOURCE_NAME)?.trim().orEmpty()
        if (audioResourceName.isNotBlank()) {
            acquireWakeLock()
            playHumanRecording(audioResourceName)
            return START_STICKY
        }

        val text = intent.getStringExtra(EXTRA_TEXT)?.trim().orEmpty()
        if (text.isBlank()) {
            releaseWakeLock()
            stopSelf(startId)
            return START_NOT_STICKY
        }
        acquireWakeLock()
        cancelClipWatchdog()
        releaseMediaPlayer()
        playingHuman = false
        playingAdhan = false
        activeResourceName = null
        pendingText = text
        persistState(mode = MODE_TEXT, title = pendingTitle, text = text, index = 0)
        updateMediaSession(pendingTitle, playing = true)
        try {
            startForegroundCompat(buildNotification(pendingTitle, text.take(120)))
        } catch (error: Exception) {
            Log.e(TAG, "Unable to promote TTS service to foreground", error)
            stopPlayback()
            return START_NOT_STICKY
        }
        if (tts == null) initializeTts() else if (ready) speakPending()
        return START_STICKY
    }

    /* ------------------------ بداية الأذان والتسجيلات البشرية ------------------------ */

    private fun playAdhanStartClip(title: String, subtitle: String) {
        tts?.stop()
        cancelClipWatchdog()
        releaseMediaPlayer()
        playingHuman = false
        playingAdhan = true
        activeResourceName = null
        pendingText = null
        prefs.edit().putBoolean(KEY_PLAYING, false).commit()

        acquireWakeLock()
        renewWakeLock()
        updateMediaSession(title, playing = true)

        try {
            startForegroundCompat(buildNotification(title, subtitle))
        } catch (error: Exception) {
            Log.e(TAG, "Could not promote Adhan service to foreground", error)
            finishRawClip()
            return
        }

        startRawResourcePlayer(
            resourceId = R.raw.adhan_start,
            resourceLabel = "adhan_start",
            reportFallbackOnError = false
        )
    }

    private fun playHumanRecording(resourceName: String) {
        tts?.stop()
        cancelClipWatchdog()
        releaseMediaPlayer()
        playingHuman = true
        playingAdhan = false
        pendingText = null
        humanIndex = HumanAzkarAudioCatalog.tracks
            .indexOfFirst { it.resourceName == resourceName }
            .takeIf { it >= 0 } ?: 0
        startHumanTrack(showSeparateTextBanner = true)
    }

    private fun startHumanTrack(showSeparateTextBanner: Boolean = false) {
        val track = HumanAzkarAudioCatalog.trackAt(humanIndex)
        pendingTitle = track.title
        activeResourceName = track.resourceName
        acquireWakeLock()
        renewWakeLock()
        persistState(mode = MODE_HUMAN, title = track.title, text = "", index = humanIndex)
        updateMediaSession(track.title, playing = true)
        try {
            startForegroundCompat(
                buildNotification(
                    track.title,
                    humanNotificationSnippet(track.index)
                )
            )
        } catch (error: Exception) {
            reportHumanPlaybackFailure(
                track.resourceName,
                "Could not promote audio service to foreground for ${track.resourceName}",
                error
            )
            return
        }

        val resourceId = track.rawResId
        if (resourceId == 0) {
            reportHumanPlaybackFailure(
                track.resourceName,
                "Missing packaged audio resource ${track.resourceName}",
                null
            )
            return
        }

        if (showSeparateTextBanner) {
            runCatching {
                NotificationHelper.showHumanAzkarTextBanner(applicationContext)
            }
        }

        startRawResourcePlayer(
            resourceId = resourceId,
            resourceLabel = track.resourceName,
            reportFallbackOnError = true
        )
    }

    private fun startRawResourcePlayer(
        resourceId: Int,
        resourceLabel: String,
        reportFallbackOnError: Boolean
    ) {
        requestTransientAudioFocus()
        val player = MediaPlayer()
        mediaPlayer = player
        scheduleClipWatchdog(45_000L)
        try {
            player.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            player.setVolume(1.0f, 1.0f)
            player.setOnCompletionListener { completed ->
                if (mediaPlayer === completed) {
                    finishRawClip()
                }
            }
            player.setOnErrorListener { _, what, extra ->
                if (reportFallbackOnError) {
                    reportHumanPlaybackFailure(
                        resourceLabel,
                        "MediaPlayer error for $resourceLabel: what=$what extra=$extra",
                        null
                    )
                } else {
                    Log.e(TAG, "MediaPlayer error for $resourceLabel: what=$what extra=$extra")
                    finishRawClip()
                }
                true
            }
            resources.openRawResourceFd(resourceId).use { descriptor ->
                player.setDataSource(
                    descriptor.fileDescriptor,
                    descriptor.startOffset,
                    descriptor.length
                )
            }
            player.prepare()
            val clipDurationMs = runCatching { player.duration.toLong() }.getOrDefault(0L)
            val watchdogDelayMs = if (clipDurationMs > 0L) {
                (clipDurationMs + 10_000L).coerceIn(30_000L, 180_000L)
            } else {
                45_000L
            }
            scheduleClipWatchdog(watchdogDelayMs)
            if (mediaPlayer === player) {
                player.start()
            }
        } catch (error: Exception) {
            if (reportFallbackOnError) {
                reportHumanPlaybackFailure(
                    resourceLabel,
                    "Could not prepare/start human-azkar audio $resourceLabel",
                    error
                )
            } else {
                Log.e(TAG, "Could not prepare/start raw clip $resourceLabel", error)
                finishRawClip()
            }
        }
    }

    private fun scheduleClipWatchdog(timeoutMs: Long = 45_000L) {
        cancelClipWatchdog()
        val runnable = Runnable {
            if (playingHuman || playingAdhan) {
                Log.w(TAG, "Playback watchdog fired after ${timeoutMs}ms; releasing completed/stalled clip cleanly")
                finishRawClip()
            }
        }
        clipWatchdogRunnable = runnable
        mainHandler.postDelayed(runnable, timeoutMs)
    }

    private fun cancelClipWatchdog() {
        clipWatchdogRunnable?.let { mainHandler.removeCallbacks(it) }
        clipWatchdogRunnable = null
    }

    private fun reportHumanPlaybackFailure(resourceName: String, message: String, error: Throwable?) {
        if (error == null) Log.e(TAG, message) else Log.e(TAG, message, error)
        runCatching {
            NotificationHelper.showHumanAzkarFallback(
                context = applicationContext,
                id = NOTIFICATION_ID + 1,
                title = "تعذّر تشغيل التسجيل",
                body = "فشل تشغيل ملف الذكر كمقطع صوتي؛ تحقق من إعدادات التشغيل في الخلفية.",
                resourceName = resourceName
            )
        }
        finishRawClip()
    }

    private fun requestTransientAudioFocus() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusReq = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(false)
                    .build()
                audioFocusRequest = focusReq
                audioManager.requestAudioFocus(focusReq)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        }
    }

    private fun abandonTransientAudioFocus() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        }
    }

    /**
     * إنهاء تشغيل المقطع الحالي دون الانتقال إلى مقطع آخر.
     */
    private fun finishRawClip() {
        cancelClipWatchdog()
        playingHuman = false
        playingAdhan = false
        activeResourceName = null
        prefs.edit().putBoolean(KEY_PLAYING, false).commit()
        releaseMediaPlayer()
        releaseMediaSession()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /* ------------------------------- TTS ------------------------------- */

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
            engine.setSpeechRate(0.95f)
            if (!com.example.data.TtsManager.selectArabicMaleVoice(engine)) {
                engine.setPitch(0.88f)
            } else {
                engine.setPitch(1.0f)
            }
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    renewWakeLock()
                }

                override fun onDone(utteranceId: String?) {
                    if (chunkIndex + 1 < speechChunks.size) {
                        chunkIndex++
                        renewWakeLock()
                        speakChunk()
                    } else {
                        stopPlayback()
                    }
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

    private fun humanNotificationSnippet(index: Int): String {
        val total = HumanAzkarAudioCatalog.tracks.size
        val periodic = runCatching {
            HumanAzkarScheduler.isEnabled(applicationContext)
        }.getOrDefault(false)
        val minutes = runCatching {
            HumanAzkarScheduler.intervalMinutes(applicationContext)
        }.getOrDefault(0)
        return if (periodic && minutes > 0) {
            "المقطع $index من $total · التالي بعد ${formatInterval(minutes)}"
        } else {
            "المقطع $index من $total · تشغيل مرة واحدة"
        }
    }

    private fun formatInterval(minutes: Int): String {
        if (minutes % 60 == 0) {
            val hours = minutes / 60
            return if (hours == 1) "ساعة واحدة" else "$hours ساعات"
        }
        return "$minutes دقيقة"
    }

    /* --------------------------- الإشعارات --------------------------- */

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
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(openPending)
            .addAction(android.R.drawable.ic_media_pause, "إيقاف", stopPending)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "تشغيل الأذكار الصوتية",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار مستمر أثناء قراءة الأذكار صوتيًا وبداية الأذان في الخلفية"
                setSound(null, null)
                enableVibration(false)
            }
        )
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /* ------------------------ حفظ واستعادة الحالة ------------------------ */

    private fun persistState(mode: String, title: String, text: String, index: Int) {
        prefs.edit()
            .putBoolean(KEY_PLAYING, true)
            .putString(KEY_MODE, mode)
            .putString(KEY_TITLE, title)
            .putString(KEY_TEXT, text)
            .putInt(KEY_INDEX, index)
            .commit()
    }

    private fun restoreFromPrefs() {
        if (!prefs.getBoolean(KEY_PLAYING, false)) {
            stopSelf()
            return
        }
        val mode = prefs.getString(KEY_MODE, MODE_HUMAN)
        pendingTitle = prefs.getString(KEY_TITLE, DEFAULT_TITLE) ?: DEFAULT_TITLE
        if (mode == MODE_TEXT) {
            val text = prefs.getString(KEY_TEXT, "").orEmpty()
            if (text.isBlank()) {
                stopPlayback()
                return
            }
            pendingText = text
            acquireWakeLock()
            updateMediaSession(pendingTitle, playing = true)
            runCatching {
                startForegroundCompat(buildNotification(pendingTitle, text.take(120)))
            }.onFailure {
                stopPlayback()
                return
            }
            if (tts == null) initializeTts() else if (ready) speakPending()
        } else {
            playingHuman = true
            humanIndex = prefs.getInt(KEY_INDEX, 0)
                .coerceIn(0, HumanAzkarAudioCatalog.tracks.lastIndex)
            acquireWakeLock()
            startHumanTrack(showSeparateTextBanner = false)
        }
    }

    /* ------------------------- الإيقاف والتحرير ------------------------- */

    private fun stopPlayback() {
        cancelClipWatchdog()
        playingHuman = false
        playingAdhan = false
        pendingText = null
        activeResourceName = null
        prefs.edit().putBoolean(KEY_PLAYING, false).commit()
        tts?.stop()
        releaseMediaPlayer()
        releaseMediaSession()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releaseMediaPlayer() {
        val player = mediaPlayer
        mediaPlayer = null
        if (player != null) {
            runCatching {
                player.setOnPreparedListener(null)
                player.setOnCompletionListener(null)
                player.setOnErrorListener(null)
                if (player.isPlaying) player.stop()
                player.release()
            }
        }
        abandonTransientAudioFocus()
    }

    /**
     * إزالة التطبيق من شاشة المهام لا تقطع التشغيل الجاري (لا البشري ولا النصي ولا الأذان)،
     * وتُعيد تثبيت المنبهات المجدولة لضمان عدم مسحها على أجهزة الشركات المصنعة.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        runCatching {
            HumanAzkarScheduler.rescheduleIfEnabled(applicationContext, forceNew = false)
            PrayerAlarmScheduler.scheduleAll(applicationContext)
        }
        val isActive = playingHuman || playingAdhan || !pendingText.isNullOrBlank() || mediaPlayer != null
        if (!isActive) {
            stopPlayback()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        cancelClipWatchdog()
        activeResourceName = null
        releaseMediaPlayer()
        releaseMediaSession()
        releaseWakeLock()
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
        const val EXTRA_AUDIO_RESOURCE_NAME = "com.example.extra.HUMAN_AZKAR_AUDIO_RESOURCE"
        const val EXTRA_IS_ADHAN = "com.example.extra.IS_ADHAN_START"
        const val EXTRA_SUBTITLE = "com.example.extra.ADHAN_SUBTITLE"
        const val CHANNEL_ID = "rim_athkar_playback"
        const val NOTIFICATION_ID = 7041
        private const val TAG = "AudioAzkarPlaybackService"

        private const val DEFAULT_TITLE = "الأذكار الصوتية"
        private const val PREFS_NAME = "rim_athkar_playback_state"
        private const val KEY_PLAYING = "playing"
        private const val KEY_MODE = "mode"
        private const val KEY_TITLE = "title"
        private const val KEY_TEXT = "text"
        private const val KEY_INDEX = "index"
        private const val MODE_HUMAN = "human"
        private const val MODE_TEXT = "text"

        /** المقطع البشري قيد التشغيل حاليًا في الخدمة الأمامية (إن وُجد). */
        var activeResourceName by mutableStateOf<String?>(null)
            private set

        /** هل هناك تشغيل جارٍ حاليًا؟ */
        fun isPlaying(context: Context): Boolean =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_PLAYING, false)
    }
}
