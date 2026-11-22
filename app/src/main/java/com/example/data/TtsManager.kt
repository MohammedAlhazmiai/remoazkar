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
    private var maleVoiceApplied = false

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
                        // نختار صوتًا عربيًا ذكوريًا قريبًا من الصوت البشري إن توفّر،
                        // مع الرجوع بأمان إلى الصوت الافتراضي إذا لم يتوفر صوت ذكوري.
                        maleVoiceApplied = selectArabicMaleVoice(engine)
                        if (!maleVoiceApplied) {
                            // لم نجد صوتًا ذكوريًا صريحًا: نخفض النبرة قليلًا لمحاكاة صوت رجل.
                            pitch = (pitch * 0.88f).coerceIn(0.6f, 1.0f)
                        }
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

    /**
     * يختار أفضل صوت عربي ذكوري متاح في محرّك النطق.
     *
     * لا يوجد في أندرويد وسمٌ قياسي يحدّد جنس الصوت، لذلك نعتمد على اسم الصوت
     * (male/female) وعلى بلد اللغة (SA أولًا) وعلى كونه محليًا (يعمل دون إنترنت).
     * نُرجع true إذا تم تعيين صوت ذكوري، وfalse إذا لم نجد صوتًا موثوقًا.
     */
    fun selectArabicMaleVoice(engine: TextToSpeech): Boolean {
        val voices = try {
            engine.voices
        } catch (_: Exception) {
            null
        } ?: return false

        val arabic = voices.filter {
            it.locale != null && it.locale.language.equals("ar", ignoreCase = true)
        }
        if (arabic.isEmpty()) return false

        fun score(voice: android.speech.tts.Voice): Int {
            val name = voice.name.lowercase(Locale.ROOT)
            val country = voice.locale.country.uppercase(Locale.ROOT)
            var s = 0
            // تفضيل صريح للذكوري، ورفض الأنثوي
            if (name.contains("female") || name.contains("woman") || name.contains("_f_")) s -= 500
            if (name.contains("male") || name.contains("man") || name.contains("_m_")) s += 300
            if (country == "SA") s += 120
            if (country == "EG") s += 60
            if (name.contains("ar-sa") || name.contains("ar_sa")) s += 80
            // الصوت المحلي أكثر موثوقية ويعمل دون إنترنت
            if (!voice.isNetworkConnectionRequired) s += 40
            // تجنّب الأصوات منخفضة الجودة إن وُسمت
            if (voice.quality >= android.speech.tts.Voice.QUALITY_HIGH) s += 20
            return s
        }

        val best = arabic.maxByOrNull { score(it) } ?: return false
        val bestScore = score(best)
        // لا نعتمد إلا على صوت يبدو ذكوريًا بدرجة معقولة.
        if (bestScore < 100) return false
        return try {
            engine.voice = best
            true
        } catch (_: Exception) {
            false
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
        // إن لم يتوفر صوت ذكوري صريح، نخفض النبرة قليلًا لمحاكاة صوت رجل.
        val effectivePitch = if (maleVoiceApplied) pitch else (pitch * 0.88f).coerceIn(0.55f, 1.2f)
        tts?.setSpeechRate(rate)
        tts?.setPitch(effectivePitch)
    }

    /**
     * تنظيف النص مع الحفاظ على نطق فصحى سليم:
     * - تحويل العلامات الصغيرة إلى حروف كاملة قبل حذف رموز المصحف.
     * - حذف رموز الوقف والتجويد والتطويل فقط.
     * - الإبقاء على الحركات (فتحة/ضمة/كسرة/سكون/شدة/تنوين) لأنها ضرورية للفصحى.
     */
    private fun cleanForSpeech(raw: String): String {
        var s = raw

        // 1) العبارات الرمزية الشائعة تُنطق كاملة بدل أن تُقرأ رموزًا.
        s = s.replace("\uFDFA", " صلى الله عليه وسلم ")
        s = s.replace("\uFDFB", " جل جلاله ")
        s = s.replace("\u0613", " رضي الله عنه ")
        s = s.replace("\u0612", " رحمه الله ")
        s = s.replace("\u0614", " عليه السلام ")

        // 2) الألف المقصورة والياء الصغيرة والألف الخنجرية إلى حروف كاملة.
        s = s.replace('\u06E5', 'و').replace('\u06E6', 'ي')
        s = s.replace('\u0670', 'ا').replace('\u0671', 'ا')

        // 3) حذف رموز الوقف والتجويد وعلامات المصحف (مع إبقاء الحركات).
        s = s.replace(Regex("[\\u06D6-\\u06DC\\u06DE-\\u06ED\\u08F0-\\u08FF\\u0610-\\u061A]"), "")

        // 4) التطويل يُحذف دون كسر الكلمة، والنجمة تصبح فاصلًا.
        s = s.replace('\u0640', ' ')
        s = s.replace("*", " ")

        // 5) رموز الأرباع والأجزاء والأقواس والعلامات الزخرفية إلى فواصل منطوقة.
        s = s.replace(Regex("[\\u06DD\\u06DE\\u06E9\\u06EA\\u06EB\\u06EC\\u06DE\\u06E9]"), " ")
        s = s.replace(Regex("[\\uFD3E\\uFD3F\\uFDFC\\uFDFD]"), " ")
        s = s.replace(Regex("[{}\\[\\]()<>«»\"“”'‘’]"), " ")

        // 6) علامات التشكيل غير الضرورية للفصحى (التعليمات الصوتية الدقيقة).
        s = s.replace(Regex("[\\u0653-\\u0655\\u0656-\\u065F]"), "")

        // 7) الأرقام العربية المشرقية والهندية إلى أرقام عربية-هندية يفهمها المحرّك.
        s = s.replace(Regex("[0-9]")) { m ->
            val d = m.value[0] - '0'
            ('\u0660' + d).toString()
        }
        s = s.replace(Regex("[\\u06F0-\\u06F9]")) { m ->
            val d = m.value[0] - '\u06F0'
            ('\u0660' + d).toString()
        }

        // 8) تنظيف الفواصل المتكررة والمسافات الزائدة.
        s = s.replace(Regex("[\\s\\u00A0]+"), " ")
        s = s.replace(Regex("\\s*([،؛:.!?])\\s*"), "$1 ")
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
