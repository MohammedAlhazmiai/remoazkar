package com.example.ai

/** Intent domains supported by the local offline Islamic language model. */
enum class IslamicIntent(val arabicLabel: String) {
    GREETING("ترحيب وتعريف"),
    FATWA("فتوى ومسألة فقهية"),
    DREAMS("تفسير الرؤى والأحلام"),
    LETTER("كتابة رسالة دينية"),
    KHATIRA("خاطرة إيمانية"),
    KHUTBAH("خطبة منبرية"),
    LECTURE("محاضرة ودرس دعوي"),
    CHAT_QA("دردشة واستدلال ديني"),
    FOLLOW_UP("متابعة السياق")
}

/** Indexed token representation used for fast offline Arabic semantic retrieval. */
data class IndexedText(
    val normalized: String,
    val tokens: List<String>,
    val stems: Set<String>
)

/**
 * Pure-Kotlin Arabic NLP normalizer, light stemmer, intent classifier, and semantic scorer.
 * Runs 100% locally offline with zero external network dependencies.
 */
object ArabicNlpEngine {

    private val DIACRITICS = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u08F0-\\u08FF\\u0640]")
    private val NON_ARABIC_OR_DIGIT = Regex("[^\\p{IsArabic}0-9\\s]")
    private val MULTI_SPACE = Regex("\\s+")

    private val STOPWORDS = setOf(
        "في", "من", "الي", "على", "عن", "مع", "او", "و", "ثم", "هل", "ما", "ماذا", "كيف",
        "متي", "اين", "لماذا", "كم", "اي", "ايه", "ان", "إن", "أن", "كان", "يكون", "هو", "هي",
        "هم", "هن", "انا", "اني", "انني", "نحن", "انت", "انتم", "هذا", "هذه", "ذلك", "تلك", "التي", "الذي",
        "الذين", "كل", "بعض", "غير", "عند", "بعد", "قبل", "بين", "تحت", "فوق", "حول", "ضد",
        "لي", "لك", "له", "لها", "لهم", "لنا", "منه", "منها", "فيه", "فيها", "عليه", "عليها",
        "اكتب", "اكتبلي", "اكتبي", "اريد", "ابغى", "ممكن", "لو", "سمحت", "فضلك", "رجاء",
        "اعطني", "اخبرني", "اشرح", "وضح", "حدثني", "قل", "قول", "شيء", "بشكل", "جدا", "بس",
        "حلمت", "رايت", "احلم", "منام", "البارحه", "شاهدت"
    )

    fun normalize(input: String): String {
        var text = input.replace("\uFEFF", "")
        text = DIACRITICS.replace(text, "")
        text = text.replace('\u0671', '\u0627')
        text = text.replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ى', 'ي')
            .replace('ة', 'ه')
            .replace('ؤ', 'و')
            .replace('ئ', 'ي')
        text = NON_ARABIC_OR_DIGIT.replace(text, " ")
        return MULTI_SPACE.replace(text, " ").trim()
    }

    fun lightStem(word: String): String {
        var w = word.trim()
        if (w.length <= 3) return w

        val prefixes = listOf("وال", "فال", "بال", "كال", "لل", "ال", "و", "ف", "ب", "ل", "س")
        for (prefix in prefixes) {
            if (w.startsWith(prefix) && w.length - prefix.length >= 3) {
                w = w.substring(prefix.length)
                break
            }
        }

        val suffixes = listOf("يات", "ات", "ون", "ين", "ان", "يه", "ها", "هم", "هن", "كم", "نا", "ه", "ي")
        for (suffix in suffixes) {
            if (w.endsWith(suffix) && w.length - suffix.length >= 3) {
                w = w.substring(0, w.length - suffix.length)
                break
            }
        }
        return w
    }

    fun indexText(input: String): IndexedText {
        val norm = normalize(input)
        val rawTokens = norm.split(' ').filter { it.isNotBlank() }
        val filtered = rawTokens.filter { it.length > 1 && it !in STOPWORDS }
        val stems = filtered.mapTo(LinkedHashSet()) { lightStem(it) }
        return IndexedText(
            normalized = norm,
            tokens = filtered,
            stems = stems
        )
    }

    fun detectIntent(query: String, hasPriorContext: Boolean): IslamicIntent {
        val norm = normalize(query)
        val tokens = norm.split(' ').filter { it.isNotBlank() }

        if (tokens.size <= 4 && GREETING_SIGNALS.any { norm == it || norm.startsWith("$it ") }) {
            val hasOtherIntent = listOf(
                DREAM_SIGNALS, FATWA_SIGNALS, LETTER_SIGNALS, KHUTBAH_SIGNALS,
                LECTURE_SIGNALS, KHATIRA_SIGNALS
            ).any { list -> list.any { norm.contains(it) } }
            if (!hasOtherIntent) return IslamicIntent.GREETING
        }

        if (hasPriorContext && tokens.size <= 10) {
            val isFollowUpPhrase = FOLLOW_UP_SIGNALS.any { norm.contains(it) } ||
                ((norm.contains("اخر") || norm.contains("اخري") || norm.contains("اضافي")) &&
                    (norm.contains("دليل") || norm.contains("ايه") || norm.contains("حديث") || norm.contains("شاهد")))
            val isExplicitNewDomain = norm.contains("حلمت") || norm.contains("ما حكم")
            if (isFollowUpPhrase && !isExplicitNewDomain) return IslamicIntent.FOLLOW_UP
        }

        val scores = mutableMapOf(
            IslamicIntent.DREAMS to scoreSignals(norm, DREAM_SIGNALS, exactBonus = 5),
            IslamicIntent.KHUTBAH to scoreSignals(norm, KHUTBAH_SIGNALS, exactBonus = 5),
            IslamicIntent.LECTURE to scoreSignals(norm, LECTURE_SIGNALS, exactBonus = 5),
            IslamicIntent.LETTER to scoreSignals(norm, LETTER_SIGNALS, exactBonus = 4),
            IslamicIntent.KHATIRA to scoreSignals(norm, KHATIRA_SIGNALS, exactBonus = 5),
            IslamicIntent.FATWA to scoreSignals(norm, FATWA_SIGNALS, exactBonus = 3),
            IslamicIntent.CHAT_QA to scoreSignals(norm, QA_SIGNALS, exactBonus = 2)
        )

        val best = scores.maxByOrNull { it.value }
        if (best != null && best.value > 0) {
            return best.key
        }
        return if (hasPriorContext && norm.length < 28 && tokens.size <= 4) {
            IslamicIntent.FOLLOW_UP
        } else {
            IslamicIntent.CHAT_QA
        }
    }

    private fun scoreSignals(norm: String, signals: List<String>, exactBonus: Int): Int {
        var score = 0
        for (signal in signals) {
            if (norm == signal) score += exactBonus * 2
            else if (" $norm ".contains(" $signal ")) score += exactBonus
            else if (norm.contains(signal)) score += (exactBonus - 1).coerceAtLeast(1)
        }
        return score
    }

    fun scoreSemanticMatch(
        queryIndexed: IndexedText,
        titleIndexed: IndexedText,
        keywordsIndexed: IndexedText,
        bodyIndexed: IndexedText,
        domainAffinity: Double = 0.0
    ): Double {
        if (queryIndexed.stems.isEmpty() && queryIndexed.normalized.isBlank()) return 0.0

        var score = 0.0

        // Whole phrase substring match against title or keywords
        if (queryIndexed.normalized.length >= 4) {
            if (titleIndexed.normalized.contains(queryIndexed.normalized)) score += 14.0
            if (keywordsIndexed.normalized.contains(queryIndexed.normalized)) score += 12.0
        }

        for (stem in queryIndexed.stems) {
            if (stem in keywordsIndexed.stems) score += 6.5
            if (stem in titleIndexed.stems) score += 5.5
            if (stem in bodyIndexed.stems) score += 2.0
        }

        for (rawToken in queryIndexed.tokens) {
            val token = if (rawToken.length > 3 && (rawToken.startsWith('و') || rawToken.startsWith('ف'))) {
                rawToken.substring(1)
            } else {
                rawToken
            }
            if (token.length >= 3) {
                if (" ${keywordsIndexed.normalized} ".contains(" $token ")) score += 4.5
                if (" ${titleIndexed.normalized} ".contains(" $token ")) score += 3.5
                if (" ${bodyIndexed.normalized} ".contains(" $token ")) score += 1.2
            }
        }

        // Bigram match bonus for multi-word terms like "سجود السهو" or "كفاره اليمين"
        if (queryIndexed.tokens.size >= 2) {
            for (i in 0 until queryIndexed.tokens.lastIndex) {
                val bigram = "${queryIndexed.tokens[i]} ${queryIndexed.tokens[i + 1]}"
                if (keywordsIndexed.normalized.contains(bigram)) score += 9.0
                if (titleIndexed.normalized.contains(bigram)) score += 8.0
                if (bodyIndexed.normalized.contains(bigram)) score += 3.0
            }
        }

        if (score <= 0.0) return 0.0
        score += domainAffinity

        // Gentle normalization so super-short noise doesn't over-rank
        val denom = 1.0 + 0.08 * kotlin.math.sqrt(bodyIndexed.stems.size.toDouble())
        return score / denom
    }

    private val GREETING_SIGNALS = listOf(
        "السلام عليكم", "سلام عليكم", "مرحبا", "اهلا", "صباح الخير", "مساء الخير", "من انت", "عرفني بنفسك"
    )

    private val FOLLOW_UP_SIGNALS = listOf(
        "دليل اخر", "دليلا اخر", "ايه اخري", "حديث اخر", "حديثا اخر", "ادله اخري", "المزيد", "تفصيل", "اكثر",
        "اطول", "اختصر", "اقصر", "لخص", "باسلوب", "كمل", "اكمل", "اضف دعاء", "وسع الشرح",
        "لوالدي", "للام", "للصديق", "وللسفر", "وماذا عن", "ماذا لو", "خصص الرساله", "حول الموضوع"
    )

    private val DREAM_SIGNALS = listOf(
        "حلم", "حلمت", "احلام", "تفسير حلم", "تفسير الاحلام", "رؤيا", "رويا", "رايت في المنام",
        "في المنام", "منام", "كابوس", "تعبير الرؤيا", "ابن سيرين", "النابلسي"
    )

    private val KHUTBAH_SIGNALS = listOf(
        "خطبه", "خطبة", "خطب", "جمعه", "خطبه جمعه", "منبر", "منبريه", "موعظه منبريه", "اوصيكم بتقوي"
    )

    private val LECTURE_SIGNALS = listOf(
        "محاضره", "محاضرة", "محاضرات", "درس ديني", "ندوه", "ندوة", "كلمه دعويه", "محاور المحاضره"
    )

    private val LETTER_SIGNALS = listOf(
        "رساله", "رسالة", "رسائل", "تهنئه", "تهنئة", "تعزيه", "تعزية", "مواساه", "مواساة",
        "معايده", "برقيه", "بطاقه دعويه", "رساله دينيه"
    )

    private val KHATIRA_SIGNALS = listOf(
        "خاطره", "خاطرة", "خواطر", "تاملات", "كلمات ايمانيه", "خاطره دينيه", "خاطره ايمانيه"
    )

    private val FATWA_SIGNALS = listOf(
        "ما حكم", "هل يجوز", "حكم", "فتوي", "فتوى", "فقه", "فقهيه", "يفطر", "مفطرات",
        "ينقض", "نواقض", "كفاره", "كفارة", "زكاه", "زكاة", "نصاب", "قصر", "جمع",
        "سجود السهو", "مسح علي الخفين", "تيمم", "غسل", "جنابه", "حيض", "طلاق", "ربا",
        "تورق", "بطاقه ائتمانيه", "نذر", "يمين", "اضحيه", "قضاء الصيام", "فوائد البنوك"
    )

    private val QA_SIGNALS = listOf(
        "دليل", "استدلال", "ايه قرانيه", "حديث", "حديث نبوي", "فضل", "معني", "ما هي اركان",
        "سوره", "تفسير ايه", "دعاء", "السيره", "الصحابه", "قصه"
    )
}
