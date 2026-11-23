package com.example.ai

import kotlin.math.exp
import kotlin.math.sqrt

/**
 * نواة تمثيل لغوي عصبية محلية خفيفة (On-Device Subword Neural Embedding & Softmax Attention Core).
 *
 * تحوّل النصوص العربية (الفصحى واللهجات الدارجة) إلى متجهات كثيفة مكونة من 128 بُعدًا
 * عبر دمج الجذور اللغوية (Stems) والثنائيات السياقية (Bigrams) والمقاطع الحرفية الثلاثية (Char 3-grams)،
 * ثم تستخدم التشابه الجيبي (Cosine Similarity) ودالة Softmax الانتباهية لتصنيف النية وإعادة ترتيب المعرفة.
 */
object NeuralArabicLanguageCore {

    const val EMBEDDING_DIM = 128

    /** تحويل النص إلى متجه دلالي كثيف مُطبَّع الطول (L2-Normalized Dense Vector). */
    fun encodeDenseVector(text: String): FloatArray {
        val indexed = ArabicNlpEngine.indexText(text)
        return encodeFromIndexed(indexed)
    }

    fun encodeFromIndexed(indexed: IndexedText, titleBoostText: String = ""): FloatArray {
        val vector = FloatArray(EMBEDDING_DIM)

        // 1. وزن الجذور اللغوية (أعلى دلالة موضوعية)
        for (stem in indexed.stems) {
            addFeature(vector, "S:$stem", 2.6f)
        }

        // 2. وزن التراكيب الثنائية (Bigrams) لفهم السياق المتجاور
        for (i in 0 until indexed.tokens.lastIndex) {
            val bigram = "${indexed.tokens[i]}_${indexed.tokens[i + 1]}"
            addFeature(vector, "B:$bigram", 3.0f)
        }

        // 3. وزن المقاطع الحرفية الثلاثية (Char 3-grams) لمقاومة الأخطاء الإملائية واللواصق اللهجية دون طغيان
        val compact = indexed.normalized.replace(" ", "_")
        if (compact.length >= 3) {
            for (i in 0..compact.length - 3) {
                val gram = compact.substring(i, i + 3)
                addFeature(vector, "C:$gram", 0.35f)
            }
        }

        // 4. تعزيز العنوان والكلمات المفتاحية إن وُجدت
        if (titleBoostText.isNotBlank()) {
            val titleIdx = ArabicNlpEngine.indexText(titleBoostText)
            for (stem in titleIdx.stems) {
                addFeature(vector, "S:$stem", 1.8f)
            }
        }

        return l2Normalize(vector)
    }

    /** التشابه الجيبي (Cosine Similarity) بين متجهين مطبعين في [-1, 1]. */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
        if (a.size != b.size || a.isEmpty()) return 0.0
        var dot = 0.0
        for (i in a.indices) {
            dot += a[i] * b[i]
        }
        return dot
    }

    /** دالة Softmax مع معامل حرارة (Temperature) لتحويل درجات التطابق إلى توزيع احتمالي للنوايا. */
    fun softmaxIntentProbabilities(
        logits: Map<IslamicIntent, Double>,
        temperature: Double = 0.28
    ): Map<IslamicIntent, Double> {
        if (logits.isEmpty()) return emptyMap()
        val scaled = logits.mapValues { (_, v) -> v / temperature.coerceAtLeast(0.05) }
        val maxLogit = scaled.values.maxOrNull() ?: 0.0
        val exps = scaled.mapValues { (_, v) -> exp(v - maxLogit) }
        val sum = exps.values.sum().coerceAtLeast(1e-9)
        return exps.mapValues { (_, v) -> v / sum }
    }

    /**
     * تصنيف عصبي مساند للنوايا عبر مقارنة متجه الاستعلام مع النماذج الأولية (Intent Prototypes)
     * المبنية لكل مجال شرعي وإبداعي، ما يتيح فهم الأسئلة الدارجة وغير المباشرة بسرعة عالية.
     */
    fun classifyIntentDistribution(query: String): Map<IslamicIntent, Double> {
        val queryVec = encodeDenseVector(query)
        val logits = intentPrototypeVectors.mapValues { (_, protoVec) ->
            cosineSimilarity(queryVec, protoVec) * 4.5
        }
        return softmaxIntentProbabilities(logits, temperature = 0.35)
    }

    private val intentPrototypeVectors: Map<IslamicIntent, FloatArray> by lazy {
        mapOf(
            IslamicIntent.APPRECIATION to encodeDenseVector(
                "شكرا جزاك الله خيرا احسنت بارك الله فيك ممتاز رائع الله يسعدك تسلم مشكور يعطيك العافية امين"
            ),
            IslamicIntent.GREETING to encodeDenseVector(
                "السلام عليكم ورحمة الله وبركاته مرحبا اهلا صباح الخير مساء الخير من انت عرفني بنفسك"
            ),
            IslamicIntent.DREAMS to encodeDenseVector(
                "حلمت رايت في المنام تفسير حلم رؤيا كابوس تعبير الرؤيا ابن سيرين النابلسي رمز الحلم منام"
            ),
            IslamicIntent.FATWA to encodeDenseVector(
                "ما حكم هل يجوز فتوى فقهية شرعية مفطرات الصيام نواقض الوضوء سجود السهو كفارة اليمين زكاة نصاب طلاق ربا تيمم غسل"
            ),
            IslamicIntent.LETTER to encodeDenseVector(
                "اكتب رسالة دينية تهنئة تعزية مواساة اعتذار شكر لوالدي لصديقي مريض عريس مولود بطاقة معايدة"
            ),
            IslamicIntent.KHATIRA to encodeDenseVector(
                "اكتب خاطرة ايمانية تأملات كلمات تريح القلب الصبر التوكل الفرج جبر الخواطر قيام الليل"
            ),
            IslamicIntent.KHUTBAH to encodeDenseVector(
                "اكتب خطبة جمعة منبرية الخطبة الاولى الخطبة الثانية موعظة تقوى الله صلة الرحم حفظ اللسان"
            ),
            IslamicIntent.LECTURE to encodeDenseVector(
                "اكتب محاضرة درس دعوي ندوة محاور تربية الابناء تزكية النفس العقيدة التوحيد السيرة"
            ),
            IslamicIntent.CHAT_QA to encodeDenseVector(
                "ما دليل اية قرانية حديث نبوي فضل سورة قصص الانبياء اركان الاسلام استجابة الدعاء السيرة"
            ),
            IslamicIntent.IMAGE_DESIGN to encodeDenseVector(
                "صمم بطاقة دعوية اسلامية ولد صورة جمعة مباركة تصميم بطاقة اية قرانية حديث شريف خلفية اسلامية"
            )
        )
    }

    /** إسقاط ثنائي الإشارة (Signed Feature Hashing) لمنع تداخل الأبعاد دون الحاجة لقاموس ثابت الحجم. */
    private fun addFeature(vector: FloatArray, feature: String, weight: Float) {
        val h1 = feature.hashCode()
        val h2 = (feature + "#2").hashCode()
        val idx = ((h1 and Int.MAX_VALUE) % EMBEDDING_DIM)
        val sign = if ((h2 and 1) == 0) 1.0f else -1.0f
        vector[idx] += sign * weight
    }

    private fun l2Normalize(vector: FloatArray): FloatArray {
        var normSq = 0.0
        for (v in vector) normSq += v * v
        val norm = sqrt(normSq).toFloat()
        if (norm <= 1e-6f) return vector
        val out = FloatArray(vector.size)
        for (i in vector.indices) {
            out[i] = vector[i] / norm
        }
        return out
    }
}
