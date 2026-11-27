package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import com.example.data.ai.AiChatRepository
import com.example.data.ai.AiMessageEntity
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import java.io.InputStream

/**
 * محرك الذكاء الاصطناعي التوليدي السحابي عبر Firebase AI Logic (Gemini).
 *
 * - يُهيأ بشكل كسول (Lazy) عند وجود اتصال بالإنترنت وعند إرسال طلب من شاشة المساعد فقط.
 * - يدعم الفهم الحقيقي للنصوص (فصحى وعامية)، الدردشة اليومية الودية كصديقين،
 *   تحليل الصور المرفوعة فعليًا، وفهم محتوى الملفات الصوتية المرفوعة (تلاوة، كلام، سؤال بالعامية).
 * - في حال انقطاع الإنترنت أو تعذر الاتصال يعيد null ليتولى المحرك المحلي الإجابة فورًا.
 */
object FirebaseAiEngine {
    private const val TAG = "FirebaseAiEngine"
    const val PRIMARY_MODEL = "gemini-3.5-flash-lite"
    const val FALLBACK_MODEL = "gemini-3.8-flash"
    private const val MAX_IMAGE_DIMENSION = 1536
    private const val MAX_AUDIO_BYTES = 15 * 1024 * 1024 // 15 MB

    data class CloudReplyResult(
        val text: String?,
        val isOffline: Boolean,
        val attemptedCloud: Boolean
    )

    private val SYSTEM_INSTRUCTION = """
        أنت المساعد الإسلامي الذكي التابع لتطبيق «زاد المسلم». أنت نموذج لغوي متطور وعالم شرعي لطيف وأديب بليغ ورفيق حوار صادق يجمع بين العلم الشرعي المؤصل والفهم الدقيق لمختلف اللهجات والأساليب البشرية.

        قواعدك الصارمة التي لا تحيد عنها:
        1. **الفهم الدقيق والمطابقة التامة لسؤال المستخدم:** افهم قصد المستخدم بدقة من كلامه (سواء بالعربية الفصحى أو بأي لهجة عامية: خليجية، يمنية، مصرية، شامية، مغاربية، عراقية...) وأجب عن طلبه هو تحديدًا ولا تنحرف أبدًا إلى موضوع آخر!
           - **الفتاوى والمسائل الفقهية:** أجب بواقعية وعلم ومنهجية، واذكر القول الراجح مع الدليل من القرآن الكريم والسنة الصحيحة، وبيّن الخلاف الفقهي المعتبر وأقوال المذاهب عند الحاجة بأسلوب ميسر وعملي.
           - **تفسير الأحلام وتعبير الرؤى:** فسّر كل رمز ذكره الرائي بدقة وفق قواعد ابن سيرين والنابلسي مع مراعاة القرائن والسياق، والتفريق بين الرؤيا الصالحة وحديث النفس والكابوس، والتنبيه الشرعي أن التعبير ظنّ واجتهاد.
           - **الرسائل الدينية والخواطر والخطب والمحاضرات:** اكتب نصوصًا واقعية بليغة مؤثرة ومكتملة البناء (خطبة أولى وثانية، محاور محاضرة متكاملة، رسائل تهنئة/تعزية/نصح/مودة، خواطر وجدانية راقية) مشفوعة بآيات وأحاديث صحيحة.
           - **إن سألك عن خدماتك** فوضّح قدراتك بوضوح.
        2. **الدردشة اليومية الصادقة كصديقين:** إن تحدث معك المستخدم في شأن حياته اليومي (تعب عمل، ملل، همّ، فرح، فضفضة، سلام، دردشة أخوية عادية)، فتفاعل معه بدفء ولطف وروح أخوية قريبة من القلب دون تكلف آلي، مع تطعيم الحديث بلمسة إيمانية مطمئنة تناسب حاله.
        3. **الفهم الفعلي للصور والصوتيات المرفقة:**
           - عند إرفاق صورة: اقرأ محتواها البصري بدقة (نصوص، آيات، لوحات، مصاحف، وثائق، مشاهد) وأجب بناءً على ما تراه فعليًا فيها.
           - عند إرفاق ملف صوتي: استمع لمحتواه الفعلي بدقة (سؤال بالعامية أو الفصحى، تلاوة قرآن، ذكر، خاطرة صوتية) وأجب مباشرة على ما سمعته في التسجيل.
        4. **الاختصاص الشرعي والأدب الإسلامي:** يقتصر عملك على العلوم الشرعية والإسلامية، وتفسير الرؤى، والآداب والخواطر والخطب والدروس، والدردشة الأخوية الطيبة والحياتية المنضبطة بأخلاق الإسلام. اعتذر بلطف وأدب إن طُلب منك كتابة أكواد برمجية، أو اختراق، أو حل مسائل فيزياء/كيمياء، أو أخبار أفلام/مباريات/موسيقى محرمة (ما لم يكن السؤال عن الحكم الشرعي فيها).
        5. **الأسلوب والتوثيق:** اكتب بلغة عربية واضحة جذابة ومنظمة بعناوين ونقاط مريحة للعين، ولا تختلق آية أو حديثًا غير صحيح أبدًا.
    """.trimIndent()

    private fun buildModel(modelName: String): GenerativeModel {
        val sysInstruction = content { text(SYSTEM_INSTRUCTION) }
        val config = generationConfig {
            temperature = 0.65f
            topP = 0.9f
            maxOutputTokens = 4096
        }
        return Firebase.ai.generativeModel(
            modelName = modelName,
            generationConfig = config,
            systemInstruction = sysInstruction
        )
    }

    private val primaryModel by lazy { buildModel(PRIMARY_MODEL) }
    private val fallbackModel by lazy { buildModel(FALLBACK_MODEL) }

    fun isNetworkAvailable(context: Context): Boolean {
        return runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }.getOrDefault(false)
    }

    suspend fun generateGenerativeReply(
        context: Context?,
        query: String,
        attachmentType: String,
        attachmentUri: String,
        recentMessages: List<AiMessageEntity>
    ): String? = generateGenerativeResult(
        context = context,
        query = query,
        attachmentType = attachmentType,
        attachmentUri = attachmentUri,
        recentMessages = recentMessages
    ).text

    suspend fun generateGenerativeResult(
        context: Context?,
        query: String,
        attachmentType: String,
        attachmentUri: String,
        recentMessages: List<AiMessageEntity>
    ): CloudReplyResult {
        if (context == null || !isNetworkAvailable(context)) {
            return CloudReplyResult(text = null, isOffline = true, attemptedCloud = false)
        }

        return try {
            AppCheckInitializer.ensureConfigured(context.applicationContext)

            val userContent = buildUserContent(context, query, attachmentType, attachmentUri)
                ?: return CloudReplyResult(text = null, isOffline = false, attemptedCloud = false)
            val history = buildChatHistory(recentMessages)

            val text = tryModel(PRIMARY_MODEL, primaryModel, history, userContent)
                ?: tryModel(FALLBACK_MODEL, fallbackModel, history, userContent)
            CloudReplyResult(text = text, isOffline = false, attemptedCloud = true)
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Log.w(TAG, "Cloud AI unavailable, falling back to local model: ${t.message}", t)
            CloudReplyResult(text = null, isOffline = false, attemptedCloud = true)
        }
    }

    private suspend fun tryModel(
        modelName: String,
        model: GenerativeModel,
        history: List<Content>,
        userContent: Content
    ): String? {
        return try {
            withTimeout(60_000L) {
                val response = if (history.isNotEmpty()) {
                    model.startChat(history).sendMessage(userContent)
                } else {
                    model.generateContent(userContent)
                }
                response.text?.trim()?.takeIf { it.isNotBlank() }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Log.w(TAG, "Gemini call failed for $modelName: ${t.message}", t)
            null
        }
    }

    private fun buildChatHistory(recentMessages: List<AiMessageEntity>): List<Content> {
        val cleanTurns = recentMessages
            .takeLast(8)
            .filter { it.content.isNotBlank() }

        if (cleanTurns.isEmpty()) return emptyList()

        // Firebase AI Chat history expects alternating user / model turns starting with user
        val normalized = mutableListOf<Content>()
        for (msg in cleanTurns) {
            val role = if (msg.role == AiChatRepository.ROLE_USER) "user" else "model"
            if (normalized.isEmpty() && role == "model") continue
            val lastRole = normalized.lastOrNull()?.role
            if (lastRole == role) {
                // Merge consecutive same-role messages cleanly
                val prevText = normalized.removeAt(normalized.lastIndex)
                val merged = prevText.parts.filterIsInstance<com.google.firebase.ai.type.TextPart>()
                    .joinToString("\n") { it.text } + "\n" + msg.content
                normalized += content(role = role) { text(merged.trim()) }
            } else {
                normalized += content(role = role) { text(msg.content.trim()) }
            }
        }
        // History passed to startChat before sendMessage(userContent) should ideally end with "model"
        if (normalized.lastOrNull()?.role == "user") {
            normalized.removeAt(normalized.lastIndex)
        }
        return normalized
    }

    private fun buildUserContent(
        context: Context,
        query: String,
        attachmentType: String,
        attachmentUri: String
    ): Content? {
        val cleanQuery = query.trim()
        val effectiveText = when {
            cleanQuery.isNotBlank() -> cleanQuery
            attachmentType == "image" -> "تفضل بقراءة وتحليل محتوى هذه الصورة المرفقة بدقة وإيضاح ما فيها من منظور إسلامي نافع."
            attachmentType == "audio" -> "استمع إلى هذا التسجيل الصوتي المرفق بعناية، وافهم ما فيه سواء كان سؤالًا بالفصحى أو العامية أو تلاوة أو ذكرًا، ثم أجب عن مضمونه بدقة ولطف."
            else -> return null
        }

        return content(role = "user") {
            if (attachmentType == "image" && attachmentUri.isNotBlank()) {
                val bitmap = decodeScaledBitmap(context, Uri.parse(attachmentUri))
                if (bitmap != null) {
                    image(bitmap)
                }
            } else if (attachmentType == "audio" && attachmentUri.isNotBlank()) {
                val audioPair = readAudioBytes(context, Uri.parse(attachmentUri))
                if (audioPair != null) {
                    inlineData(audioPair.first, audioPair.second)
                }
            }
            text(effectiveText)
        }
    }

    private fun decodeScaledBitmap(context: Context, uri: Uri): Bitmap? {
        return runCatching {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            var sampleSize = 1
            val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
            if (maxDim > MAX_IMAGE_DIMENSION) {
                while (maxDim / sampleSize > MAX_IMAGE_DIMENSION) {
                    sampleSize *= 2
                }
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            }
        }.onFailure {
            Log.w(TAG, "Failed decoding image attachment: ${it.message}")
        }.getOrNull()
    }

    private fun readAudioBytes(context: Context, uri: Uri): Pair<ByteArray, String>? {
        return runCatching {
            val rawMime = context.contentResolver.getType(uri)?.lowercase()?.trim()
            val mimeType = when {
                rawMime.isNullOrBlank() || rawMime == "application/octet-stream" -> guessAudioMimeFromUri(uri)
                rawMime.startsWith("audio/") -> rawMime
                rawMime == "video/mp4" -> "audio/mp4"
                else -> "audio/mpeg"
            }

            val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                readStreamBounded(stream, MAX_AUDIO_BYTES)
            } ?: return null

            if (bytes.isEmpty()) null else bytes to mimeType
        }.onFailure {
            Log.w(TAG, "Failed reading audio attachment: ${it.message}")
        }.getOrNull()
    }

    private fun readStreamBounded(stream: InputStream, maxBytes: Int): ByteArray? {
        val buffer = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        var total = 0
        while (true) {
            val read = stream.read(chunk)
            if (read == -1) break
            total += read
            if (total > maxBytes) {
                Log.w(TAG, "Audio file exceeds max allowed size ($maxBytes bytes)")
                return null
            }
            buffer.write(chunk, 0, read)
        }
        return buffer.toByteArray()
    }

    private fun guessAudioMimeFromUri(uri: Uri): String {
        val path = uri.toString().lowercase()
        return when {
            path.endsWith(".m4a") || path.endsWith(".mp4") -> "audio/mp4"
            path.endsWith(".wav") -> "audio/wav"
            path.endsWith(".ogg") || path.endsWith(".opus") -> "audio/ogg"
            path.endsWith(".aac") -> "audio/aac"
            path.endsWith(".webm") -> "audio/webm"
            path.endsWith(".3gp") -> "audio/3gpp"
            path.endsWith(".amr") -> "audio/amr"
            path.endsWith(".flac") -> "audio/flac"
            else -> "audio/mpeg"
        }
    }
}
