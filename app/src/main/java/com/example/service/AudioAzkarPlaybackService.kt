package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.HumanAzkarAudioCatalog
import java.util.Locale

/**
 * خدمة تشغيل الأذكار الصوتية في الخلفية.
 *
 * - التسجيلات البشرية تُشغَّل **مقطعًا واحدًا في كل مرة**، ثم يتوقف التشغيل، ويعود الدور إلى
 *   المجدول الزمني (`HumanAzkarScheduler`) ليُشغّل المقطع التالي بعد الفاصل الذي اختاره المستخدم
 *   (دقائق أو ساعات، جاهزة أو يدوية). لا يوجد أي تشغيل تسلسلي متواصل.
 * - تُحفظ حالة التشغيل في SharedPreferences حتى تستأنف الخدمة نفسها إن أعادها النظام.
 * - الخدمة أمامية (Foreground) ولا تتوقف عند إزالة التطبيق من قائمة التطبيقات الحديثة
 *   (`android:stopWithTask="false"` + `onTaskRemoved`)، وتعمل مع قفل الشاشة عبر WakeLock متجدّد.
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

    /** نوع التشغيل الحالي: تسجيل بشري أو نص عبر TTS. */
    private var playingHuman = false
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

    /** يُجدَّد عند كل مقطع حتى لا ينتهي أثناء التشغيل الطويل. */
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
        val audioResourceName = intent.getStringExtra(EXTRA_AUDIO_RESOURCE_NAME)?.trim().orEmpty()

        if (audioResourceName.isNotBlank()) {
            // لا تسلسل: أي طلب جديد يستبدل المقطع الحالي ويُشغّل المقطع المطلوب مرة واحدة.
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
        releaseMediaPlayer()
        playingHuman = false
        pendingText = text
        persistState(mode = MODE_TEXT, title = pendingTitle, text = text, index = 0)
        startForegroundCompat(buildNotification(pendingTitle, text.take(120)))
        if (tts == null) initializeTts() else if (ready) speakPending()
        return START_STICKY
    }

    /* ------------------------ التسجيلات البشرية ------------------------ */

    private fun playHumanRecording(resourceName: String) {
        tts?.stop()
        releaseMediaPlayer()
        playingHuman = true
        humanIndex = HumanAzkarAudioCatalog.tracks
            .indexOfFirst { it.resourceName == resourceName }
            .takeIf { it >= 0 } ?: 0
        startHumanTrack()
    }

    private fun startHumanTrack() {
        val track = HumanAzkarAudioCatalog.trackAt(humanIndex)
        pendingTitle = track.title
        acquireWakeLock()
        renewWakeLock()
        persistState(mode = MODE_HUMAN, title = track.title, text = "", index = humanIndex)
        startForegroundCompat(
            buildNotification(
                track.title,
                humanNotificationSnippet(track.index)
            )
        )

        val resourceId = resources.getIdentifier(track.resourceName, "raw", packageName)
        if (resourceId == 0) {
            finishHumanTrack()
            return
        }

        val player = MediaPlayer()
        mediaPlayer = player
        try {
            player.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            player.setOnPreparedListener { prepared ->
                if (mediaPlayer === prepared) {
                    runCatching { prepared.start() }
                }
            }
            // عند انتهاء المقطع يتوقف التشغيل؛ المقطع التالي يأتي من المجدول الزمني حسب الفاصل المختار.
            player.setOnCompletionListener { completed ->
                if (mediaPlayer === completed) {
                    releaseMediaPlayer()
                    finishHumanTrack()
                }
            }
            player.setOnErrorListener { _, _, _ ->
                releaseMediaPlayer()
                finishHumanTrack()
                true
            }
            resources.openRawResourceFd(resourceId).use { descriptor ->
                player.setDataSource(
                    descriptor.fileDescriptor,
                    descriptor.startOffset,
                    descriptor.length
                )
            }
            player.prepareAsync()
        } catch (_: Exception) {
            releaseMediaPlayer()
            finishHumanTrack()
        }
    }

    /**
     * إنهاء تشغيل المقطع الحالي دون الانتقال إلى مقطع آخر.
     * المجدول الزمني (HumanAzkarScheduler) هو من يُحدّد موعد المقطع التالي حسب الفاصل المختار،
     * لذا لا نعيد الجدولة هنا حتى يبقى الفاصل دقيقًا من لحظة المنبّه.
     */
    private fun finishHumanTrack() {
        playingHuman = false
        prefs.edit().putBoolean(KEY_PLAYING, false).apply()
        releaseMediaPlayer()
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
            // اختيار صوت عربي ذكوري إن توفّر، وإلا نخفض النبرة لمحاكاة صوت رجل.
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
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) {
                    if (chunkIndex + 1 < speechChunks.size) {
                        chunkIndex++
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

    /** نص الإشعار: يوضّح أن التشغيل مقطع واحد وأن التالي يأتي حسب الفاصل الذي اختاره المستخدم. */
    private fun humanNotificationSnippet(index: Int): String {
        val total = HumanAzkarAudioCatalog.tracks.size
        val periodic = runCatching {
            com.example.data.HumanAzkarScheduler.isEnabled(applicationContext)
        }.getOrDefault(false)
        val minutes = runCatching {
            com.example.data.HumanAzkarScheduler.intervalMinutes(applicationContext)
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
                description = "إشعار مستمر أثناء قراءة الأذكار صوتيًا في الخلفية"
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
            .apply()
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
            startForegroundCompat(buildNotification(pendingTitle, text.take(120)))
            if (tts == null) initializeTts() else if (ready) speakPending()
        } else {
            playingHuman = true
            humanIndex = prefs.getInt(KEY_INDEX, 0)
                .coerceIn(0, HumanAzkarAudioCatalog.tracks.lastIndex)
            acquireWakeLock()
            startHumanTrack()
        }
    }

    /* ------------------------- الإيقاف والتحرير ------------------------- */

    private fun stopPlayback() {
        playingHuman = false
        prefs.edit().putBoolean(KEY_PLAYING, false).apply()
        tts?.stop()
        releaseMediaPlayer()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releaseMediaPlayer() {
        val player = mediaPlayer ?: return
        mediaPlayer = null
        runCatching {
            player.setOnPreparedListener(null)
            player.setOnCompletionListener(null)
            player.setOnErrorListener(null)
            if (player.isPlaying) player.stop()
            player.release()
        }
    }

    /**
     * إزالة التطبيق من قائمة التطبيقات الحديثة لا تقطع المقطع الجاري؛ الخدمة تبقى أمامية حتى نهاية المقطع.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!playingHuman && mediaPlayer == null) {
            stopPlayback()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        releaseMediaPlayer()
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
        const val CHANNEL_ID = "rim_athkar_playback"
        const val NOTIFICATION_ID = 7041

        private const val DEFAULT_TITLE = "الأذكار الصوتية"
        private const val PREFS_NAME = "rim_athkar_playback_state"
        private const val KEY_PLAYING = "playing"
        private const val KEY_MODE = "mode"
        private const val KEY_TITLE = "title"
        private const val KEY_TEXT = "text"
        private const val KEY_INDEX = "index"
        private const val MODE_HUMAN = "human"
        private const val MODE_TEXT = "text"

        /** هل هناك تشغيل جارٍ حاليًا؟ */
        fun isPlaying(context: Context): Boolean =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_PLAYING, false)
    }
}
