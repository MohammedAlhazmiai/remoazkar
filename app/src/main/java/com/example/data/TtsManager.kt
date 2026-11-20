package com.example.data

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * محرّك القراءة الصوتية (الروبوت) — فصحى سليمة، بلا تلاوات مسجّلة.
 *
 * المزايا:
 *  - يقرأ أي نص في التطبيق بمجرد تمريره إلى [speak] أو [toggle].
 *  - يُقسّم النص إلى مقاطع صغيرة ويحفظ موضع كل مقطع، وهذا ما يجعل
 *    شريط التقديم/التأخير ([seekTo]) ممكنًا فعليًا رغم أن أندرويد
 *    لا يوفّر دالة seek في محرّك النطق.
 *  - يدعم الإيقاف المؤقت والاستئناف من نفس الموضع ([pause] / [resume]).
 *  - يعرض [progress] من 0 إلى 1 ليتحرّك الشريط تلقائيًا أثناء القراءة.
 */
object TtsManager {

    var isSpeaking by mutableStateOf(false)
        private set
    var isPaused by mutableStateOf(false)
        private set
    var isReady by mutableStateOf(false)
        private set
    var lastError by mutableStateOf<String?>(null)
        private set

    /** نسبة التقدّم من 0.0 إلى 1.0 — يقرأها شريط التشغيل. */
    var progress by mutableStateOf(0f)
        private set

    /** نص المقطع الذي يُقرأ الآن (لعرضه في الشريط). */
    var currentSnippet by mutableStateOf("")
        private set

    private var tts: TextToSpeech? = null
    private var initialized = false
    private var rate = 0.95f
    private var pitch = 1.0f

    /** المقاطع الحالية للنص المطلوب قراءته. */
    private var chunks: List<String> = emptyList()
    private var currentIndex = 0

    /** حجم المقطع: صغير بما يكفي ليكون التقديم دقيقًا، وكبير بما يكفي لسلامة الوقف. */
    private const val CHUNK_SIZE = 380

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = tts
                if (engine != null) {
                    val locale = Locale.Builder().setLanguage("ar").setRegion("SA").build()
                    val res = engine.setLanguage(locale)
                    if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                        lastError = "محرّك النطق العربي غير متوفر على هذا الجهاز"
                    } else {
                        // لا نفرض صوتًا عشوائيًا من قائمة المحرك؛ بذلك يُحترم الصوت
                        // العربي الذي يختاره المستخدم من إعدادات تحويل النص إلى كلام.
                        lastError = null
                    }
                    engine.setSpeechRate(rate.coerceIn(0.82f, 1.15f))
                    engine.setPitch(pitch)
                    engine.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            isSpeaking = true
                            isPaused = false
                            val idx = indexOf(utteranceId)
                            if (idx >= 0) {
                                currentIndex = idx
                                currentSnippet = chunks.getOrNull(idx) ?: ""
                                progress = fractionOf(idx)
                            }
                        }

                        override fun onDone(utteranceId: String?) {
                            val idx = indexOf(utteranceId)
                            if (idx >= 0 && chunks.isNotEmpty()) {
                                progress = fractionOf(idx + 1).coerceIn(0f, 1f)
                            }
                            // آخر مقطع فقط يُنهي الحالة
                            if (idx >= 0 && idx >= chunks.size - 1) {
                                isSpeaking = false
                                isPaused = false
                                currentSnippet = ""
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            isSpeaking = false
                            lastError = "تعذّر تشغيل الصوت"
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            isSpeaking = false
                            lastError = "تعذّر تشغيل الصوت ($errorCode)"
                        }
                    })
                    isReady = true
                }
            } else {
                lastError = "فشل تهيئة محرّك النطق"
            }
        }
    }

    private fun indexOf(utteranceId: String?): Int {
        if (utteranceId == null) return -1
        return utteranceId.removePrefix("rim-").toIntOrNull() ?: -1
    }

    private fun fractionOf(index: Int): Float {
        if (chunks.isEmpty()) return 0f
        return (index.toFloat() / chunks.size.toFloat()).coerceIn(0f, 1f)
    }

    fun setParams(speechRate: Float, speechPitch: Float) {
        rate = speechRate.coerceIn(0.5f, 1.6f)
        pitch = speechPitch.coerceIn(0.5f, 1.8f)
        tts?.setSpeechRate(rate)
        tts?.setPitch(pitch)
    }

    /**
     * تنظيف النص مع الحفاظ على نطق فصحى سليم:
     * - تحويل العلامات الصغيرة إلى حروف كاملة قبل حذف رموز المصحف.
     * - حذف رموز الوقف والتجويد والتطويل فقط.
     * - الإبقاء على الحركات (فتحة/ضمة/كسرة/سكون/شدة/تنوين) لأنها ضرورية للفصحى.
     */
    private fun cleanForSpeech(raw: String): String {
        var s = raw

        s = s.replace('\u06E5', 'و').replace('\u06E6', 'ي')
        s = s.replace('\u0670', 'ا').replace('\u0671', 'ا')

        s = s.replace(Regex("[\\u06D6-\\u06DC\\u06DE-\\u06ED\\u08F0-\\u08FF\\u0610-\\u061A]"), "")

        s = s.replace('\u0640', ' ')
        s = s.replace("*", " ")
        s = s.replace(Regex("[\\u06DD\\u06DE\\u06E9\\u06EA\\u06EB\\u06EC]"), " ")

        s = s.replace(Regex("[\\u0653-\\u0655\\u0656-\\u065F]"), "")

        s = s.replace(Regex("\\s+"), " ")
        return s.trim()
    }

    /** تقسيم النص على حدود الجُمل والكلمات قدر الإمكان. */
    private fun split(text: String, max: Int = CHUNK_SIZE): List<String> {
        if (text.length <= max) return listOf(text)
        val out = ArrayList<String>()
        var start = 0
        while (start < text.length) {
            var end = (start + max).coerceAtMost(text.length)
            if (end < text.length) {
                val window = text.substring(start, end)
                val cut = maxOf(
                    window.lastIndexOf('.'), window.lastIndexOf('!'),
                    window.lastIndexOf('?'), window.lastIndexOf('\n'),
                    window.lastIndexOf('،'), window.lastIndexOf(' '),
                    window.lastIndexOf('؛'), window.lastIndexOf(':')
                )
                if (cut > max / 2) end = start + cut + 1
            }
            out.add(text.substring(start, end).trim())
            start = end
        }
        return out.filter { it.isNotBlank() }
    }

    fun speak(text: String) {
        val engine = tts
        if (engine == null || !isReady) {
            lastError = "محرّك النطق لم يجهز بعد"
            return
        }
        val clean = cleanForSpeech(text)
        if (clean.isBlank()) return

        engine.stop()
        chunks = split(clean)
        if (chunks.isEmpty()) return
        currentIndex = 0
        isPaused = false
        isSpeaking = true
        progress = 0f
        currentSnippet = chunks.first()
        enqueueFrom(0)
    }

    /** يبدأ الإرسال إلى المحرّك من مقطع محدد. */
    private fun enqueueFrom(from: Int) {
        val engine = tts ?: return
        for (i in from until chunks.size) {
            val mode = if (i == from) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            engine.speak(chunks[i], mode, Bundle(), "rim-$i")
        }
    }

    /** تقديم أو تأخير القراءة إلى نسبة محددة من النص (0.0 إلى 1.0). */
    fun seekTo(fraction: Float) {
        if (chunks.isEmpty()) return
        val target = (fraction.coerceIn(0f, 1f) * chunks.size).toInt()
            .coerceIn(0, chunks.size - 1)
        tts?.stop()
        currentIndex = target
        progress = fractionOf(target)
        currentSnippet = chunks.getOrNull(target) ?: ""
        isPaused = false
        isSpeaking = true
        enqueueFrom(target)
    }

    /** إيقاف مؤقت مع تذكّر الموضع. */
    fun pause() {
        if (!isSpeaking) return
        tts?.stop()
        isPaused = true
        isSpeaking = false
    }

    /** استئناف القراءة من الموضع الذي توقفت عنده. */
    fun resume() {
        if (chunks.isEmpty()) return
        isPaused = false
        isSpeaking = true
        enqueueFrom(currentIndex.coerceIn(0, chunks.size - 1))
    }

    fun togglePlayPause() {
        when {
            isSpeaking -> pause()
            isPaused -> resume()
            else -> if (chunks.isNotEmpty()) resume()
        }
    }

    fun toggle(text: String) {
        if (isSpeaking && currentSnippet.isNotEmpty()) stop() else speak(text)
    }

    fun stop() {
        tts?.stop()
        isSpeaking = false
        isPaused = false
        progress = 0f
        currentSnippet = ""
        currentIndex = 0
        chunks = emptyList()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        initialized = false
        isReady = false
        isSpeaking = false
        isPaused = false
        chunks = emptyList()
    }

    fun isArabicAvailable(): Boolean = isReady
}
