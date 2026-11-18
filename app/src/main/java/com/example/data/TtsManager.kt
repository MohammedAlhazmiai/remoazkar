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
 * محرك القراءة الصوتية (الروبوت) — فصحى سليمة، بلا تلاوات مسجّلة.
 * يقرأ أي نص في التطبيق بمجرد تمريره إلى speak() أو toggle().
 */
object TtsManager {

    var isSpeaking by mutableStateOf(false)
        private set
    var isReady by mutableStateOf(false)
        private set
    var lastError by mutableStateOf<String?>(null)
        private set

    private var tts: TextToSpeech? = null
    private var initialized = false
    private var currentText: String = ""
    private var chunkCount = 0
    private var doneChunks = 0
    private var rate = 0.95f
    private var pitch = 1.0f

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
                        lastError = "محرك النطق العربي غير متوفر على هذا الجهاز"
                    }
                    engine.setSpeechRate(rate)
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
                        }

                        override fun onDone(utteranceId: String?) {
                            doneChunks++
                            if (doneChunks >= chunkCount) {
                                isSpeaking = false
                                currentText = ""
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
                lastError = "فشل تهيئة محرك النطق"
            }
        }
    }

    fun setParams(speechRate: Float, speechPitch: Float) {
        rate = speechRate.coerceIn(0.5f, 1.6f)
        pitch = speechPitch.coerceIn(0.5f, 1.8f)
        tts?.setSpeechRate(rate)
        tts?.setPitch(pitch)
    }

    /** تنظيف النص من علامات التجويد والرموز غير المنطوقة */
    private fun cleanForSpeech(raw: String): String {
        var s = raw

        // 1) تحويل العلامات الصغيرة إلى حروف كاملة قبل حذف رموز المصحف،
        //    وإلا حُذفت مع مجموعة \u06D6-\u06ED وضاع نطقها.
        s = s.replace('\u06E5', 'و').replace('\u06E6', 'ي')
        s = s.replace('\u0670', 'ا').replace('\u0671', 'ا')

        // 2) حذف رموز الوقف والتجويد وعلامات الترقيم المصحفية غير المنطوقة فقط.
        s = s.replace(Regex("[\\u06D6-\\u06DC\\u06DE-\\u06ED\\u08F0-\\u08FF\\u0610-\\u061A\\u06DD]"), "")

        // 3) التطويل والأقواس والنجوم التي يقرأها المحرّك حرفيًا.
        s = s.replace('\u0640', ' ')
        s = s.replace("*", " ")
        s = s.replace(Regex("[\\u06DD\\u06DE\\u06E9\\u06EA\\u06EB\\u06EC]"), " ")

        // 4) نُبقي الحركات (الفتحة والضمة والكسرة والسكون والشدة والتنوين)
        //    لأنها ضرورية لنطق فصحى سليم، ونحذف فقط ما لا يُنطق.
        s = s.replace(Regex("[\\u0653-\\u0655\\u0656-\\u065F]"), "")

        s = s.replace(Regex("\\s+"), " ")
        return s.trim()
    }

    private fun split(text: String, max: Int = 2200): List<String> {
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
                    window.lastIndexOf('،'), window.lastIndexOf(' ')
                )
                if (cut > max / 2) end = start + cut + 1
            }
            out.add(text.substring(start, end).trim())
            start = end
        }
        return out.filter { it.isNotBlank() }
    }

    fun speak(text: String) {
        val engine = tts ?: return
        if (!isReady) {
            lastError = "محرك النطق لم يجهز بعد"
            return
        }
        val clean = cleanForSpeech(text)
        if (clean.isBlank()) return
        stop()
        currentText = clean
        val chunks = split(clean)
        chunkCount = chunks.size
        doneChunks = 0
        isSpeaking = true
        chunks.forEachIndexed { i, chunk ->
            val mode = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val params = Bundle()
            engine.speak(chunk, mode, params, "rim-$i")
        }
    }

    fun toggle(text: String) {
        if (isSpeaking && currentText.isNotEmpty()) stop() else speak(text)
    }

    fun stop() {
        tts?.stop()
        isSpeaking = false
        doneChunks = 0
        chunkCount = 0
        currentText = ""
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        initialized = false
        isReady = false
        isSpeaking = false
    }

    fun isArabicAvailable(): Boolean = isReady
}
