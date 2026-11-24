package com.example.ai

/** Intent domains supported by the local offline Islamic language model. */
enum class IslamicIntent(val arabicLabel: String) {
    GREETING("ترحيب وتعريف"),
    APPRECIATION("شكر وثناء"),
    FATWA("فتوى ومسألة فقهية"),
    DREAMS("تفسير الرؤى والأحلام"),
    LETTER("كتابة رسالة دينية"),
    KHATIRA("خاطرة إيمانية"),
    KHUTBAH("خطبة منبرية"),
    LECTURE("محاضرة ودرس دعوي"),
    CHAT_QA("دردشة واستدلال ديني"),
    IMAGE_DESIGN("تصميم بطاقة دعوية"),
    FOLLOW_UP("متابعة السياق")
}

/** Indexed token representation used for fast offline Arabic semantic retrieval. */
data class IndexedText(
    val normalized: String,
    val tokens: List<String>,
    val stems: Set<String>
)

/**
 * Pure-Kotlin Arabic NLP normalizer, dialect bridge, light stemmer, neural hybrid intent classifier,
 * and semantic scorer. Runs 100% locally offline with zero external network dependencies.
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
        "اكتب", "اكتبلي", "اكتبي", "اريد", "ابغي", "ابغا", "ابي", "عايز", "عاوز", "ودي",
        "ممكن", "لو", "سمحت", "فضلك", "رجاء", "اعطني", "اخبرني", "اشرح", "وضح", "حدثني",
        "قل", "قول", "شيء", "بشكل", "جدا", "بس", "وش", "ايش", "شو", "ليش", "عشان",
        "حلمت", "رايت", "احلم", "منام", "البارحه", "شاهدت"
    )

    /**
     * جذور وصفية عامة لمجال الطلب (مثل: تفسير، حلم، حكم، فتوى، خطبة، مساعدة)
     * لا تكفي وحدها لإثبات تطابق الموضوع إذا لم يتطابق المحتوى الفعلي للسؤال.
     */
    val DOMAIN_META_STEMS = setOf(
        "تفسير", "فسر", "تعبير", "حلم", "احلام", "رويا", "روي", "منام",
        "حكم", "احكام", "فتو", "فتوي", "فقه", "يجوز", "حلال", "حرام", "شرع", "شرعا",
        "دليل", "ادله", "خطب", "خطبه", "رسال", "رساله", "خاطر", "خاطره",
        "محاضر", "محاضره", "درس", "سوال", "استفسار", "موضوع", "مسال", "مساله",
        "مساعد", "خدم", "مهام", "تقدم", "تستطيع", "تقدر", "كيفيه"
    )

    fun extractContentStems(indexed: IndexedText): Set<String> =
        indexed.stems.filter { it !in DOMAIN_META_STEMS && it.length >= 2 }.toSet()

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
        text = MULTI_SPACE.replace(text, " ").trim()
        return bridgeColloquialForms(text)
    }

    /** تحويل الصيغ الدارجة الشائعة إلى ما يقابلها فصحى لضمان سرعة الفهم في كل اللهجات العربية. */
    private fun bridgeColloquialForms(norm: String): String {
        var s = " $norm "
        s = s.replace(" فسرلي ", " فسر لي ")
            .replace(" اكتبلي ", " اكتب لي ")
            .replace(" اعطيني ", " اعطني ")
            .replace(" قولي ", " قل لي ")
            .replace(" علمني ", " اخبرني ")
            .replace(" وش حكم ", " ما حكم ")
            .replace(" ايش حكم ", " ما حكم ")
            .replace(" شو حكم ", " ما حكم ")
            .replace(" وش تفسير ", " ما تفسير ")
            .replace(" ايش تفسير ", " ما تفسير ")
            .replace(" شو تفسير ", " ما تفسير ")
            .replace(" حلمتلك ", " حلمت ")
            .replace(" شفت في المنام ", " رايت في المنام ")
            .replace(" تحلمت ", " حلمت ")
        return MULTI_SPACE.replace(s, " ").trim()
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

        if (tokens.size <= 5 && GREETING_SIGNALS.any { norm == it || norm.startsWith("$it ") }) {
            val hasOtherIntent = listOf(
                DREAM_SIGNALS, FATWA_SIGNALS, LETTER_SIGNALS, KHUTBAH_SIGNALS,
                LECTURE_SIGNALS, KHATIRA_SIGNALS, IMAGE_DESIGN_SIGNALS
            ).any { list -> list.any { norm.contains(it) } }
            if (!hasOtherIntent) return IslamicIntent.GREETING
        }

        if (tokens.size <= 6 && APPRECIATION_SIGNALS.any { norm == it || norm.contains(it) }) {
            val hasActionIntent = listOf(
                DREAM_SIGNALS, FATWA_SIGNALS, LETTER_SIGNALS, KHUTBAH_SIGNALS,
                LECTURE_SIGNALS, KHATIRA_SIGNALS, IMAGE_DESIGN_SIGNALS
            ).any { list -> list.any { norm.contains(it) } }
            if (!hasActionIntent) return IslamicIntent.APPRECIATION
        }

        if (hasPriorContext && tokens.size <= 10) {
            val isFollowUpPhrase = FOLLOW_UP_SIGNALS.any { norm.contains(it) } ||
                ((norm.contains("اخر") || norm.contains("اخري") || norm.contains("اضافي")) &&
                    (norm.contains("دليل") || norm.contains("ايه") || norm.contains("حديث") || norm.contains("شاهد")))
            val isExplicitNewDomain = norm.contains("حلمت") || norm.contains("ما حكم") || norm.contains("تفسير حلم")
            if (isFollowUpPhrase && !isExplicitNewDomain) return IslamicIntent.FOLLOW_UP
        }

        val ruleScores = mutableMapOf(
            IslamicIntent.DREAMS to scoreSignals(norm, DREAM_SIGNALS, exactBonus = 5),
            IslamicIntent.KHUTBAH to scoreSignals(norm, KHUTBAH_SIGNALS, exactBonus = 5),
            IslamicIntent.LECTURE to scoreSignals(norm, LECTURE_SIGNALS, exactBonus = 5),
            IslamicIntent.LETTER to scoreSignals(norm, LETTER_SIGNALS, exactBonus = 4),
            IslamicIntent.KHATIRA to scoreSignals(norm, KHATIRA_SIGNALS, exactBonus = 5),
            IslamicIntent.IMAGE_DESIGN to scoreSignals(norm, IMAGE_DESIGN_SIGNALS, exactBonus = 6),
            IslamicIntent.FATWA to scoreSignals(norm, FATWA_SIGNALS, exactBonus = 3),
            IslamicIntent.CHAT_QA to scoreSignals(norm, QA_SIGNALS, exactBonus = 2)
        )

        val bestRule = ruleScores.maxByOrNull { it.value }
        if (bestRule != null && bestRule.value > 0) {
            return bestRule.key
        }

        if (hasPriorContext && norm.length < 28 && tokens.size <= 4) {
            return IslamicIntent.FOLLOW_UP
        }

        val isQuestionForm = norm.startsWith("هل ") || norm.startsWith("ما ") || norm.startsWith("كم ") ||
            norm.startsWith("متي ") || norm.startsWith("كيف ") || norm.startsWith("لماذا ")
        val neuralProbs = NeuralArabicLanguageCore.classifyIntentDistribution(query)
        val filteredNeural = if (isQuestionForm) {
            neuralProbs.filterKeys {
                it == IslamicIntent.FATWA || it == IslamicIntent.CHAT_QA || it == IslamicIntent.DREAMS
            }
        } else {
            neuralProbs
        }
        val bestNeural = filteredNeural.maxByOrNull { it.value }
        if (bestNeural != null && bestNeural.value >= 0.16) {
            return bestNeural.key
        }

        return if (isQuestionForm) IslamicIntent.FATWA else IslamicIntent.CHAT_QA
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
        domainAffinity: Double = 0.0,
        queryEmbedding: FloatArray? = null,
        entryEmbedding: FloatArray? = null
    ): Double {
        if (queryIndexed.stems.isEmpty() && queryIndexed.normalized.isBlank()) return 0.0

        val contentStems = extractContentStems(queryIndexed)
        val requireContentHit = contentStems.isNotEmpty()
        var contentHitCount = 0
        var score = 0.0

        // Whole phrase substring match against title or keywords (only meaningful if query has content)
        if (queryIndexed.normalized.length >= 4 && (!requireContentHit || queryIndexed.tokens.size >= 2)) {
            if (titleIndexed.normalized.contains(queryIndexed.normalized)) {
                score += 14.0
                contentHitCount++
            }
            if (keywordsIndexed.normalized.contains(queryIndexed.normalized)) {
                score += 12.0
                contentHitCount++
            }
        }

        for (stem in queryIndexed.stems) {
            val isMeta = stem in DOMAIN_META_STEMS
            val kwWeight = if (isMeta) 0.1 else 7.0
            val titleWeight = if (isMeta) 0.1 else 6.0
            val bodyWeight = if (isMeta) 0.05 else 2.2

            var hit = false
            if (stem in keywordsIndexed.stems) {
                score += kwWeight
                hit = true
            }
            if (stem in titleIndexed.stems) {
                score += titleWeight
                hit = true
            }
            if (stem in bodyIndexed.stems) {
                score += bodyWeight
                hit = true
            }
            if (hit && !isMeta) {
                contentHitCount++
            }
        }

        for (rawToken in queryIndexed.tokens) {
            val token = if (rawToken.length > 3 && (rawToken.startsWith('و') || rawToken.startsWith('ف'))) {
                rawToken.substring(1)
            } else {
                rawToken
            }
            if (token.length >= 3 && lightStem(token) !in DOMAIN_META_STEMS) {
                var tokenHit = false
                if (" ${keywordsIndexed.normalized} ".contains(" $token ")) {
                    score += 4.5
                    tokenHit = true
                }
                if (" ${titleIndexed.normalized} ".contains(" $token ")) {
                    score += 3.5
                    tokenHit = true
                }
                if (" ${bodyIndexed.normalized} ".contains(" $token ")) {
                    score += 1.2
                    tokenHit = true
                }
                if (tokenHit) contentHitCount++
            }
        }

        // Bigram match bonus for multi-word terms (only count if at least one word isn't generic meta)
        if (queryIndexed.tokens.size >= 2) {
            for (i in 0 until queryIndexed.tokens.lastIndex) {
                val w1 = queryIndexed.tokens[i]
                val w2 = queryIndexed.tokens[i + 1]
                if (lightStem(w1) in DOMAIN_META_STEMS && lightStem(w2) in DOMAIN_META_STEMS) continue
                val bigram = "$w1 $w2"
                var biHit = false
                if (keywordsIndexed.normalized.contains(bigram)) {
                    score += 9.0
                    biHit = true
                }
                if (titleIndexed.normalized.contains(bigram)) {
                    score += 8.0
                    biHit = true
                }
                if (bodyIndexed.normalized.contains(bigram)) {
                    score += 3.0
                    biHit = true
                }
                if (biHit) contentHitCount++
            }
        }

        // منع التطابق الكاذب تمامًا: إذا حوى السؤال موضوعًا محددًا ولم يتطابق أي جذر فعلي منه، نُعيد 0.0 فورًا
        if (requireContentHit && contentHitCount == 0) {
            return 0.0
        }

        // Hybrid Dense Neural Cosine Reranking (يُضاف فقط بعد التأكد من صلة المحتوى)
        if (queryEmbedding != null && entryEmbedding != null && entryEmbedding.isNotEmpty()) {
            val neuralCosine = NeuralArabicLanguageCore.cosineSimilarity(queryEmbedding, entryEmbedding)
            if (neuralCosine >= 0.16 && (contentHitCount > 0 || (!requireContentHit && neuralCosine >= 0.35))) {
                score += neuralCosine * 10.0
            }
        }

        if (score <= 0.0) return 0.0
        score += domainAffinity

        val denom = 1.0 + 0.08 * kotlin.math.sqrt(bodyIndexed.stems.size.toDouble())
        return score / denom
    }

    private val GREETING_SIGNALS = listOf(
        "السلام عليكم", "سلام عليكم", "مرحبا", "اهلا", "صباح الخير", "مساء الخير", "من انت", "عرفني بنفسك", "اهلين", "حياك"
    )

    private val APPRECIATION_SIGNALS = listOf(
        "شكرا", "جزاك الله خيرا", "جزاك الله خير", "بارك الله فيك", "احسنت", "ممتاز", "رائع",
        "الله يسعدك", "يعطيك العافيه", "مشكور", "تسلم", "تقبل الله", "امين"
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
        "ما حكم", "هل يجوز", "حكم", "فتوي", "فتوى", "فقه", "فقهيه", "يفطر", "تفطر", "مفطرات",
        "الصيام", "صيام", "الصايم", "صايم", "بخاخ الربو", "قطره العين",
        "ينقض", "نواقض", "كفاره", "كفارة", "زكاه", "زكاة", "نصاب", "قصر", "جمع",
        "سجود السهو", "مسح علي الخفين", "تيمم", "غسل", "جنابه", "حيض", "طلاق", "ربا",
        "تورق", "بطاقه ائتمانيه", "نذر", "يمين", "اضحيه", "قضاء الصيام", "فوائد البنوك",
        "شروط الصلاه", "اركان الصلاه", "واجبات الصلاه", "مبطلات الصلاه", "نجاسه", "وسوسه", "ميراث", "دين"
    )

    private val QA_SIGNALS = listOf(
        "دليل", "استدلال", "ايه قرانيه", "حديث", "حديث نبوي", "فضل", "معني", "ما هي اركان",
        "سوره", "تفسير ايه", "دعاء", "السيره", "الصحابه", "قصه"
    )

    private val IMAGE_DESIGN_SIGNALS = listOf(
        "صمم بطاقه", "صمم لي بطاقه", "ولد صوره", "ولد لي صوره", "صمم صوره", "بطاقه دعويه",
        "بطاقه جمعه", "بطاقه اذكار", "بطاقه دعاء", "لوحه اسلاميه", "تصميم اسلامي", "صوره دينيه",
        "صمم بطاقه ايه", "صمم بطاقه حديث", "صوره جمعه مباركه", "تصميم بطاقه"
    )
}
