package com.example.ai

import android.content.Context
import com.example.data.HisnRepository
import com.example.data.QuranRepository
import com.example.data.ai.AiChatRepository
import com.example.data.ai.AiMessageEntity

data class IslamicAiResponse(
    val text: String,
    val domainTag: String,
    val detectedTopic: String,
    val references: List<String>,
    val followUps: List<String>
)

/**
 * Local Offline Islamic Language Model Engine.
 *
 * Trained on curated Fiqh fatwas, Ibn Sirin & Al-Nabulsi dream interpretation rules,
 * Islamic letters, faith reflections (khawatir), Jumu'ah khutbahs, da'wah lectures,
 * and cross-indexed with the full bundled Holy Quran (6,236 Ayat), AdiyaData, and Hisn Al-Muslim.
 */
object IslamicAiModel {

    fun prepareAssets(context: Context) {
        IslamicCorpusLoader.ensureLoaded(context)
        QuranRepository.ensureLoaded(context)
        HisnRepository.ensureLoaded(context)
    }

    fun generateReply(
        query: String,
        entries: List<CorpusEntry>,
        recentMessages: List<AiMessageEntity> = emptyList()
    ): IslamicAiResponse {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            return greetingResponse()
        }

        val lastUserMsg = recentMessages.lastOrNull { it.role == AiChatRepository.ROLE_USER }
        val lastAssistantMsg = recentMessages.lastOrNull { it.role == AiChatRepository.ROLE_ASSISTANT }
        val hasPriorContext = lastAssistantMsg != null

        val intent = ArabicNlpEngine.detectIntent(cleanQuery, hasPriorContext)

        return when (intent) {
            IslamicIntent.GREETING -> greetingResponse()
            IslamicIntent.APPRECIATION -> appreciationResponse()
            IslamicIntent.FOLLOW_UP -> handleFollowUp(cleanQuery, entries, lastUserMsg, lastAssistantMsg)
            IslamicIntent.DREAMS -> composeDreamReply(cleanQuery, entries)
            IslamicIntent.LETTER -> composeCreativeReply(cleanQuery, IslamicIntent.LETTER, "letters", entries)
            IslamicIntent.KHATIRA -> composeCreativeReply(cleanQuery, IslamicIntent.KHATIRA, "khawatir", entries)
            IslamicIntent.KHUTBAH -> composeCreativeReply(cleanQuery, IslamicIntent.KHUTBAH, "khutbah", entries)
            IslamicIntent.LECTURE -> composeCreativeReply(cleanQuery, IslamicIntent.LECTURE, "lecture", entries)
            IslamicIntent.FATWA -> composeKnowledgeReply(cleanQuery, IslamicIntent.FATWA, "fatwa", entries)
            IslamicIntent.CHAT_QA -> composeKnowledgeReply(cleanQuery, IslamicIntent.CHAT_QA, "chat_qa", entries)
        }
    }

    private fun greetingResponse(): IslamicAiResponse = IslamicAiResponse(
        text = "وعليكم السلام ورحمة الله وبركاته، أهلاً ومرحباً بك أخي الكريم في **المساعد الإسلامي الذكي المحلي** (يعمل بكفاءة عالية بدون إنترنت).\n\n" +
            "يسعدني ويشرفني خدمتك بأسلوب عربي فصيح ومتزن في المجالات التالية:\n" +
            "• **الفتاوى والمسائل الفقهية:** الطهارة، الصلاة، الصيام ومفطراته، الزكاة، المعاملات، الأيمان، الأسرة، مع الأدلة من القرآن والسنة.\n" +
            "• **تفسير الأحلام وتعبير الرؤى:** وفق قواعد ابن سيرين والنابلسي والضوابط الشرعية.\n" +
            "• **الرسائل الدينية والخواطر:** تهنئة، تعزية، مواساة، اعتذار، بر والدين، وتأملات إيمانية.\n" +
            "• **الخطب والمحاضرات:** خطب جمعة كاملة الأركان ودروس دعوية بالمحاور والشواهد.\n" +
            "• **الاستدلال والبحث الديني:** استخراج الآيات القرآنية والأحاديث النبوية في أي موضوع.\n\n" +
            "تفضل بكتابة سؤالك أو طلبك، وستُحفظ محادثتك محليًا للعودة إليها في أي وقت.",
        domainTag = IslamicIntent.GREETING.arabicLabel,
        detectedTopic = "الترحيب والتعريف",
        references = emptyList(),
        followUps = listOf(
            "ما حكم بخاخ الربو وقطرة العين في الصيام؟",
            "فسر لي حلم المطر والبحر في المنام",
            "اكتب لي خطبة جمعة عن تقوى الله",
            "اكتب رسالة دينية للتهنئة بالعيد"
        )
    )

    private fun appreciationResponse(): IslamicAiResponse = IslamicAiResponse(
        text = "وجزاك الله خيرًا وبارك فيك وفي أهلك ووقتك أخي الكريم، شكرًا لطيب كلماتك ودعائك.\n\n" +
            "أنا في خدمتك دائمًا لصياغة الرسائل الدينية والخواطر والخطب والمحاضرات، أو الإجابة على المسائل الفقهية وتفسير الرؤى بالأدلة الشرعية.\n" +
            "تقبل الله منا ومنكم صالح الأعمال، وأسعدك في الدارين.",
        domainTag = IslamicIntent.APPRECIATION.arabicLabel,
        detectedTopic = "دعاء وشكر متبادل",
        references = emptyList(),
        followUps = listOf(
            "اكتب لي خاطرة إيمانية عن شكر النعم",
            "أعطني دعاءً جامعًا للخير كله"
        )
    )

    private fun handleFollowUp(
        query: String,
        entries: List<CorpusEntry>,
        lastUserMsg: AiMessageEntity?,
        lastAssistantMsg: AiMessageEntity?
    ): IslamicAiResponse {
        val norm = ArabicNlpEngine.normalize(query)
        val priorTopic = lastUserMsg?.content.orEmpty()
        val combinedTopicQuery = "$priorTopic $query".trim()

        if (norm.contains("دليل") || norm.contains("ايه") || norm.contains("حديث") || norm.contains("ادله")) {
            val exclude = lastAssistantMsg?.referencesSummary
                ?.split("•")
                ?.map { it.trim() }
                ?.toSet()
                ?: emptySet()
            val quran = QuranHadithRetriever.findQuranCitations(priorTopic.ifBlank { query }, limit = 2, excludeSources = exclude)
            val hadith = QuranHadithRetriever.findHadithAndDuaCitations(priorTopic.ifBlank { query }, limit = 2, excludeSources = exclude)
            val refs = (quran.map { "${it.label}: ${it.source}" } + hadith.map { it.source }).distinct()
            val extraRefs = refs.toMutableList()
            val body = buildString {
                append("استكمالًا للحديث حول الموضوع السابق، إليك شواهد وأدلة إضافية موثّقة:\n")
                if (quran.isNotEmpty()) {
                    append("\n**من القرآن الكريم:**\n")
                    quran.forEach { append("• ${it.text} [${it.source}]\n") }
                }
                if (hadith.isNotEmpty()) {
                    append("\n**من السنة النبوية والأذكار المأثورة:**\n")
                    hadith.forEach { append("• «${it.text}» — ${it.source}\n") }
                }
                if (quran.isEmpty() && hadith.isEmpty()) {
                    val top = rankEntries(priorTopic, entries, targetDomain = null).firstOrNull()?.first
                    if (top != null) {
                        if (top.quranEvidence.isNotBlank()) append("\n• **الدليل القرآني:** ${top.quranEvidence}\n")
                        if (top.hadithEvidence.isNotBlank()) append("• **الدليل النبوي:** ${top.hadithEvidence}\n")
                        extraRefs += top.title
                    } else {
                        append("\n﴿وَذَكِّرْ فَإِنَّ الذِّكْرَى تَنْفَعُ الْمُؤْمِنِينَ﴾ [الذاريات: 55].\n")
                        extraRefs += "الذاريات: 55"
                    }
                }
            }.trim()

            return IslamicAiResponse(
                text = body,
                domainTag = IslamicIntent.FOLLOW_UP.arabicLabel,
                detectedTopic = lastAssistantMsg?.categoryTag ?: "شواهد إضافية",
                references = extraRefs.distinct(),
                followUps = listOf("اجعل الرد مختصرًا في نقاط", "اكتب خاطرة حول هذا الموضوع")
            )
        }

        if (norm.contains("اختصر") || norm.contains("اقصر") || norm.contains("لخص")) {
            val prevText = lastAssistantMsg?.content.orEmpty()
            val condensed = prevText.lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .take(5)
                .joinToString("\n")
                .ifBlank { "خلاصة ما سبق: التقوى، والمحافظة على الفرائض، والعمل بالدليل الشرعي." }
            return IslamicAiResponse(
                text = "**ملخص مركز للموضوع:**\n\n$condensed",
                domainTag = "ملخص موجز",
                detectedTopic = lastAssistantMsg?.categoryTag ?: "ملخص",
                references = emptyList(),
                followUps = listOf("أعطني دليلًا إضافيًا", "وسّع الشرح مع التفصيل")
            )
        }

        if (norm.contains("اضف دعاء") || norm.contains("دعاء")) {
            return IslamicAiResponse(
                text = "**دعاء جامع مناسب لخاتمة ما سبق:**\n\n" +
                    "«اللهم إنا نسألك الهدى والتقى والعفاف والغنى، اللهم أصلح لنا ديننا الذي هو عصمة أمرنا، وأصلح لنا دنيانا التي فيها معاشنا، وأصلح لنا آخرتنا التي فيها معادنا، واجعل الحياة زيادةً لنا في كل خير، والموت راحةً لنا من كل شر، ربنا آتنا في الدنيا حسنة وفي الآخرة حسنة وقنا عذاب النار».",
                domainTag = "دعاء مأثور",
                detectedTopic = "دعاء ختامي",
                references = listOf("صحيح مسلم • البقرة: 201"),
                followUps = listOf("حوّل النص إلى رسالة مشاركة", "موضوع جديد")
            )
        }

        // Re-evaluate full intent on merged context
        val combinedIntent = ArabicNlpEngine.detectIntent(combinedTopicQuery, hasPriorContext = false)
        return when (combinedIntent) {
            IslamicIntent.DREAMS -> composeDreamReply(combinedTopicQuery, entries)
            IslamicIntent.LETTER -> composeCreativeReply(combinedTopicQuery, IslamicIntent.LETTER, "letters", entries)
            IslamicIntent.KHATIRA -> composeCreativeReply(combinedTopicQuery, IslamicIntent.KHATIRA, "khawatir", entries)
            IslamicIntent.KHUTBAH -> composeCreativeReply(combinedTopicQuery, IslamicIntent.KHUTBAH, "khutbah", entries)
            IslamicIntent.LECTURE -> composeCreativeReply(combinedTopicQuery, IslamicIntent.LECTURE, "lecture", entries)
            IslamicIntent.FATWA -> composeKnowledgeReply(combinedTopicQuery, IslamicIntent.FATWA, "fatwa", entries)
            else -> composeKnowledgeReply(combinedTopicQuery, IslamicIntent.CHAT_QA, "chat_qa", entries)
        }
    }

    private fun composeDreamReply(query: String, entries: List<CorpusEntry>): IslamicAiResponse {
        val dreamEntries = entries.filter { it.domain == "dreams" }
        val ranked = rankEntries(query, dreamEntries, targetDomain = null)
            .filter { it.second >= 4.0 }
        val rulesEntry = dreamEntries.firstOrNull { it.id == "dream_rules_etiquette" }

        // Gather up to 4 matched symbols (excluding general rules entry unless it's the only one)
        val symbols = ranked.map { it.first }.filter { it.id != "dream_rules_etiquette" }.take(4)
        val norm = ArabicNlpEngine.normalize(query)

        val text = buildString {
            append("**تفسير الرؤيا وضوابطها الشرعية (وفق منهج ابن سيرين والنابلسي):**\n\n")
            if (symbols.isNotEmpty()) {
                append("**دلالات الرموز الواردة في منامك:**\n")
                symbols.forEach { sym ->
                    append("\n• **${sym.title}:**\n${sym.content}\n")
                }
                if (norm.contains("حزن") || norm.contains("خوف") || norm.contains("كابوس") || norm.contains("مزعج")) {
                    append("\n**توجيه نبوي عند القلق أو الكابوس:**\n")
                    append("إذا صاحَبَ المنام فزعٌ أو كدر فكثيرًا ما يكون من حديث النفس أو تهويل الشيطان؛ فاستعذ بالله ثلاثًا، وانفث عن يسارك ثلاثًا، ولا تحدّث بها أحدًا، ولن تضرك بإذن الله.\n")
                }
            } else if (rulesEntry != null) {
                append(rulesEntry.content)
                append("\n\n**نصيحة:** اذكر أبرز الرموز التي رأيتها (مثل: المطر، البحر، الميت، الصلاة، الكعبة، السفر، الذهب، البيت، الأسنان...) لأستخرج لك دلالتها بدقة.")
            }

            val primaryRef = symbols.firstOrNull() ?: rulesEntry
            if (primaryRef != null) {
                append("\n**الشواهد الشرعية:**\n")
                if (primaryRef.quranEvidence.isNotBlank()) append("• ${primaryRef.quranEvidence}\n")
                if (primaryRef.hadithEvidence.isNotBlank()) append("• ${primaryRef.hadithEvidence}\n")
            }
            append("\n*تنبيه شرعي: تعبير الرؤى ظنّ واجتهاد وتختلف دلالته باختلاف حال الرائي، ولا يُبنى عليه حكم شرعي ولا قطع بغيب.*")
        }.trim()

        val refs = buildList {
            symbols.forEach { add(it.title) }
            if (isEmpty() && rulesEntry != null) add(rulesEntry.title)
        }

        return IslamicAiResponse(
            text = text,
            domainTag = IslamicIntent.DREAMS.arabicLabel,
            detectedTopic = symbols.firstOrNull()?.title ?: "تعبير الرؤى",
            references = refs,
            followUps = symbols.firstOrNull()?.followUps ?: rulesEntry?.followUps.orEmpty()
        )
    }

    private fun composeCreativeReply(
        query: String,
        intent: IslamicIntent,
        domain: String,
        entries: List<CorpusEntry>
    ): IslamicAiResponse {
        val domainEntries = entries.filter { it.domain == domain }
        val bestPair = rankEntries(query, domainEntries, targetDomain = domain).firstOrNull()
        val bestScore = bestPair?.second ?: 0.0
        val best = bestPair?.first
        val norm = ArabicNlpEngine.normalize(query)

        val liveQuran = QuranHadithRetriever.findQuranCitations(query, limit = 2)
        val liveHadith = QuranHadithRetriever.findHadithAndDuaCitations(query, limit = 1)
        val topicPhrase = extractRequestedTopic(query)

        val text = if (best != null && bestScore >= 7.5 && !needsCustomTopicSynthesis(query, best)) {
            buildAdaptedTemplate(query, best, liveQuran, liveHadith)
        } else {
            synthesizeCustomComposition(intent, topicPhrase, norm, best, liveQuran, liveHadith)
        }

        val refs = buildList {
            liveQuran.forEach { add("${it.label}: ${it.source}") }
            liveHadith.forEach { add(it.source) }
            if (isEmpty() && best != null && best.quranEvidence.isNotBlank()) add(best.title)
        }

        return IslamicAiResponse(
            text = text,
            domainTag = intent.arabicLabel,
            detectedTopic = topicPhrase.ifBlank { best?.title ?: intent.arabicLabel },
            references = refs,
            followUps = best?.followUps?.takeIf { it.isNotEmpty() } ?: defaultFollowUps(intent)
        )
    }

    private fun composeKnowledgeReply(
        query: String,
        intent: IslamicIntent,
        preferredDomain: String,
        entries: List<CorpusEntry>
    ): IslamicAiResponse {
        val ranked = rankEntries(query, entries, targetDomain = preferredDomain)
        val topPair = ranked.firstOrNull()
        val top = topPair?.first
        val topScore = topPair?.second ?: 0.0

        val liveQuran = QuranHadithRetriever.findQuranCitations(query, limit = 2)
        val liveHadith = QuranHadithRetriever.findHadithAndDuaCitations(query, limit = 2)

        val text = buildString {
            if (top != null && topScore >= 5.5) {
                append("**${top.title}**\n\n")
                append(top.content)
                append("\n\n**الأدلة الشرعية:**\n")
                if (top.quranEvidence.isNotBlank()) append("• **من القرآن الكريم:** ${top.quranEvidence}\n")
                if (top.hadithEvidence.isNotBlank()) append("• **من السنة النبوية:** ${top.hadithEvidence}\n")
                if (liveQuran.isNotEmpty()) {
                    append("\n**آيات ذات صلة مباشرة من المصحف:**\n")
                    liveQuran.forEach { append("• ${it.text} [${it.source}]\n") }
                }
                if (top.scholarlyNote.isNotBlank()) {
                    append("\n**فائدة فقهية:** ${top.scholarlyNote}")
                }
            } else if (liveQuran.isNotEmpty() || liveHadith.isNotEmpty()) {
                append("استنادًا إلى النصوص الشرعية الموثّقة المحفوظة داخل التطبيق حول موضوع استفسارك:\n")
                if (liveQuran.isNotEmpty()) {
                    append("\n**من كتاب الله تعالى:**\n")
                    liveQuran.forEach { append("• ${it.text} [${it.source}]\n") }
                }
                if (liveHadith.isNotEmpty()) {
                    append("\n**من السنة والأذكار المأثورة:**\n")
                    liveHadith.forEach { append("• «${it.text}» — ${it.source}\n") }
                }
                append("\n*تنبيه: في المسائل الشخصية والفتاوى التفصيلية الدقيقة يُنصح باستشارة أهل العلم الموثوقين بعد بيان كامل تفاصيل الحالة.*")
            } else {
                append("لم أجد مسألة مطابقة تمامًا لهذا اللفظ في قاعدة الفتاوى المحلية حتى لا أفتي بغير علم، فالأمانة العلمية تقتضي التثبت.\n\n")
                append("يمكنك إعادة صياغة السؤال بكلمات أوضح (مثل: حكم سجود السهو، بخاخ الربو للصائم، زكاة المال، كفارة اليمين، تفسير حلم...) أو طلب خطبة/رسالة/خاطرة في أي موضوع.")
            }
        }.trim()

        val refs = buildList {
            liveQuran.forEach { add("${it.label}: ${it.source}") }
            liveHadith.forEach { add(it.source) }
            if (top != null && topScore >= 5.5 && top.quranEvidence.isNotBlank()) {
                add(top.title)
            }
        }.distinct()

        return IslamicAiResponse(
            text = text,
            domainTag = top?.let { domainArabicLabel(it.domain) } ?: intent.arabicLabel,
            detectedTopic = top?.takeIf { topScore >= 5.5 }?.title ?: extractRequestedTopic(query),
            references = refs,
            followUps = top?.takeIf { topScore >= 5.5 }?.followUps?.takeIf { it.isNotEmpty() }
                ?: defaultFollowUps(intent)
        )
    }

    private fun buildAdaptedTemplate(
        query: String,
        best: CorpusEntry,
        liveQuran: List<EvidenceCitation>,
        liveHadith: List<EvidenceCitation>
    ): String {
        val norm = ArabicNlpEngine.normalize(query)
        val wantsShort = norm.contains("قصير") || norm.contains("مختصر")
        val recipientPrefix = detectRecipientGreeting(norm)

        return buildString {
            append("**${best.title}**\n\n")
            if (recipientPrefix != null && best.domain == "letters") {
                append("$recipientPrefix\n\n")
            }
            val content = if (wantsShort) {
                best.content.split("\n\n").take(3).joinToString("\n\n")
            } else {
                best.content
            }
            append(content)
            append("\n\n**الشواهد المأثورة:**\n")
            if (best.quranEvidence.isNotBlank()) append("• ${best.quranEvidence}\n")
            if (best.hadithEvidence.isNotBlank()) append("• ${best.hadithEvidence}\n")
            liveQuran.take(1).forEach { q ->
                if (!best.quranEvidence.contains(q.source)) {
                    append("• ${q.text} [${q.source}]\n")
                }
            }
            liveHadith.take(1).forEach { h ->
                if (!best.hadithEvidence.contains(h.source)) {
                    append("• «${h.text}» — ${h.source}\n")
                }
            }
        }.trim()
    }

    private fun synthesizeCustomComposition(
        intent: IslamicIntent,
        topic: String,
        norm: String,
        fallbackEntry: CorpusEntry?,
        liveQuran: List<EvidenceCitation>,
        liveHadith: List<EvidenceCitation>
    ): String {
        val displayTopic = topic.ifBlank { "التقوى وصلاح القلوب" }
        val quranCite = liveQuran.firstOrNull()?.let { "${it.text} [${it.source}]" }
            ?: fallbackEntry?.quranEvidence
            ?: "﴿يَا أَيُّهَا الَّذِينَ آمَنُوا اتَّقُوا اللَّهَ وَقُولُوا قَوْلًا سَدِيدًا﴾ [الأحزاب: 70]"
        val hadithCite = liveHadith.firstOrNull()?.let { "«${it.text}» (${it.source})" }
            ?: fallbackEntry?.hadithEvidence
            ?: "«اتق الله حيثما كنت، وأتبع السيئة الحسنة تمحها، وخالق الناس بخلق حسن» (رواه الترمذي)"
        val recipient = detectRecipientGreeting(norm)

        return when (intent) {
            IslamicIntent.KHUTBAH -> buildString {
                append("**خطبة منبرية عن: $displayTopic**\n\n")
                append("**الخطبة الأولى:**\n")
                append("إن الحمد لله نحمده ونستعينه ونستغفره، ونعوذ بالله من شرور أنفسنا ومن سيئات أعمالنا، من يهده الله فلا مضل له، ومن يضلل فلا هادي له، وأشهد أن لا إله إلا الله وحده لا شريك له، وأشهد أن محمدًا عبده ورسوله.\n")
                append("﴿يَا أَيُّهَا الَّذِينَ آمَنُوا اتَّقُوا اللَّهَ حَقَّ تُقَاتِهِ وَلَا تَمُوتُنَّ إِلَّا وَأَنْتُمْ مُسْلِمُونَ﴾.\n\n")
                append("أما بعد، عباد الله:\n")
                append("أوصيكم ونفسي بتقوى الله والاعتصام بكتابه وسنة نبيه ﷺ، فإن من أعظم ما ينبغي أن يعتني به المسلم في دينه ودنياه **$displayTopic**؛ لما فيه من صلاح القلب، واستقامة السلوك، ونيل رضا الله تعالى والبركة في الرزق والعمر.\n\n")
                append("وقد قرر القرآن الكريم هذا الأصل العظيم في قوله تعالى:\n$quranCite\n\n")
                append("كما بيّن النبي ﷺ هديه في ذلك بقوله:\n$hadithCite\n\n")
                append("فحريّ بنا يا عباد الله أن نجعل هذا الخُلق والطاعة واقعًا عمليًا في بيوتنا وأعمالنا ومعاملاتنا.\n\n")
                append("**الخطبة الثانية:**\n")
                append("الحمد لله رب العالمين، والصلاة والسلام على المبعوث رحمةً للعالمين.\n")
                append("عباد الله: بادروا بالتوبة الصادقة والإحسان قبل فوات الأوان، وأكثروا من الصلاة على النبي ﷺ.\n")
                append("اللهم اهدنا لأحسن الأخلاق لا يهدي لأحسنها إلا أنت، واصرف عنا سيئها، ربنا آتنا في الدنيا حسنة وفي الآخرة حسنة وقنا عذاب النار، وأقم الصلاة.")
            }

            IslamicIntent.LECTURE -> buildString {
                append("**محاضرة دعوية بعنوان: $displayTopic**\n\n")
                append("**1. التمهيد والافتتاح:**\n")
                append("الحمد لله رب العالمين، والصلاة والسلام على أشرف الأنبياء والمرسلين. إخوتي الكرام، نتناول اليوم موضوعًا يمسّ حياة كل مسلم وسلامة قلبه، ألا وهو: **$displayTopic**.\n\n")
                append("**2. المحور الأول — الأساس القرآني والنبوي:**\n")
                append("• قال تعالى: $quranCite\n")
                append("• وجاء في الأثر والهدي النبوي: $hadithCite\n\n")
                append("**3. المحور الثاني — الأثر الإيماني والسلوكي:**\n")
                append("يورث الالتزام بـ($displayTopic) طمأنينةً في القلب، وقوةً في الإيمان، وإصلاحًا للعلاقة مع الله ثم مع الأهل والمجتمع.\n\n")
                append("**4. المحور الثالث — خطوات تطبيقية عملية:**\n")
                append("- تجديد النية والإخلاص لله تعالى.\n")
                append("- البدء بخطوة ثابتة كل يوم ولو كانت يسيرة.\n")
                append("- الدعاء بالثبات والقبول.\n\n")
                append("**5. الخاتمة:**\n")
                append("اللهم علّمنا ما ينفعنا وانفعنا بما علّمتنا وزدنا علمًا وعملًا صالحًا متقبلًا.")
            }

            IslamicIntent.LETTER -> buildString {
                append("**رسالة دينية حول: $displayTopic**\n\n")
                append(recipient ?: "أخي الكريم / أختي الفاضلة، حفظكم الله ورعاكم،")
                append("\nالسلام عليكم ورحمة الله وبركاته،\n\n")
                append("أبعث إليكم بهذه الكلمات الطيبة محبّةً في الله وتذكيرًا بخيرٍ يجمعنا على طاعته؛ وأوصي نفسي وإياكم بـ**$displayTopic**، فهي من أسباب السكينة والبركة وحفظ الله للعبد.\n\n")
                append("ولنتأمّل قول الله تعالى:\n$quranCite\n\n")
                append("أسأل الله أن يشرح صدوركم، وييسر أموركم، ويبارك لكم في أهلكم وأعماركم، وألا ينسينا الله من فضله وكرمه.\n\n")
                append("أخوكم المحب لكم في الله / تقبل الله منا ومنكم.")
            }

            else -> buildString {
                append("**خاطرة إيمانية حول: $displayTopic**\n\n")
                append("في زحام الأيام وتقلّب الأحوال، يظل القلب محتاجًا إلى وقفة صدقٍ يستشعر فيها معنى **$displayTopic**.\n\n")
                append("كلما اقترب العبد من ربه، وتأمّل قوله تعالى: $quranCite، أدرك أن الخيرات كلها بيد الله، وأن الطمأنينة الحقيقية تبدأ حين نُصلح ما بيننا وبين خالقنا.\n\n")
                append("فاجعل لنفسك نصيبًا من الذكر والدعاء، واذكر قول نبيك ﷺ: $hadithCite، واملأ يومك بحسن الظن بالله.")
            }
        }.trim()
    }

    private fun needsCustomTopicSynthesis(query: String, entry: CorpusEntry): Boolean {
        val queryIdx = ArabicNlpEngine.indexText(query)
        val overlap = queryIdx.stems.count { it in entry.keywordsIndexed.stems || it in entry.titleIndexed.stems }
        // Only generic format keyword ("خطبه", "رساله", "خاطره", "محاضره") matched without matching the entry's subject
        return overlap <= 1 && queryIdx.stems.size >= 3
    }

    private fun extractRequestedTopic(query: String): String {
        val cleaned = query
            .trim()
            .replace(Regex("^(اكتب لي|أكتب لي|اكتبلي|اكتب|أكتب|أعد لي|أعد|اعد لي|اعد|اعطني|أعطني|صغ لي|صغ|ممكن|أريد|اريد|حضر لي|حضر)\\s*"), "")
            .replace(Regex("^(خطبة جمعة كاملة|خطبة جمعة|خطبة منبرية|خطبة|محاضرة دعوية|محاضرة|درس ديني|درس|رسالة دينية إسلامية|رسالة دينية|رسالة|خاطرة إيمانية|خاطرة دينيه|خاطرة)\\s*(عن|حول|في|بشأن|موضوع|بمناسبة)?\\s*"), "")
            .trim()
        return if (cleaned.length >= 3) cleaned else query.trim()
    }

    private fun detectRecipientGreeting(norm: String): String? = when {
        norm.contains("لامي") || norm.contains("للام") || norm.contains("امي") -> "أمي الحبيبة، يا تاج رأسي وبركة عمري،"
        norm.contains("لابي") || norm.contains("للاب") || norm.contains("ابي") -> "أبي الغالي، يا سندي وقدوتي بعد الله،"
        norm.contains("لوالدي") || norm.contains("للولادين") || norm.contains("للوالدين") -> "والديّ الكريمين، حفظكما الله وأدام بركتكما،"
        norm.contains("لصديق") || norm.contains("لصاحبي") || norm.contains("لزميل") -> "صديقي العزيز وأخي في الله،"
        norm.contains("لاخي") || norm.contains("لاختي") -> "أخي الحبيب / أختي العزيزة في الله،"
        norm.contains("لمريض") -> "أخي الكريم، شفاك الله وعافاك وطهّرك،"
        else -> null
    }

    private fun rankEntries(
        query: String,
        entries: List<CorpusEntry>,
        targetDomain: String?
    ): List<Pair<CorpusEntry, Double>> {
        val queryIndexed = ArabicNlpEngine.indexText(query)
        val queryEmbedding = NeuralArabicLanguageCore.encodeFromIndexed(queryIndexed)
        return entries.map { entry ->
            val domainBonus = if (targetDomain != null && entry.domain == targetDomain) 4.0 else 0.0
            val score = ArabicNlpEngine.scoreSemanticMatch(
                queryIndexed = queryIndexed,
                titleIndexed = entry.titleIndexed,
                keywordsIndexed = entry.keywordsIndexed,
                bodyIndexed = entry.bodyIndexed,
                domainAffinity = domainBonus,
                queryEmbedding = queryEmbedding,
                entryEmbedding = entry.denseEmbedding
            )
            entry to score
        }.sortedByDescending { it.second }
    }

    private fun domainArabicLabel(domain: String): String = when (domain) {
        "fatwa" -> IslamicIntent.FATWA.arabicLabel
        "dreams" -> IslamicIntent.DREAMS.arabicLabel
        "letters" -> IslamicIntent.LETTER.arabicLabel
        "khawatir" -> IslamicIntent.KHATIRA.arabicLabel
        "khutbah" -> IslamicIntent.KHUTBAH.arabicLabel
        "lecture" -> IslamicIntent.LECTURE.arabicLabel
        else -> IslamicIntent.CHAT_QA.arabicLabel
    }

    private fun defaultFollowUps(intent: IslamicIntent): List<String> = when (intent) {
        IslamicIntent.FATWA -> listOf("أعطني دليلًا آخر من القرآن والسنة", "اختصر الحكم في نقاط")
        IslamicIntent.DREAMS -> listOf("ما آداب الرؤيا الصالحة والكابوس؟", "اذكر أدعية التحصين قبل النوم")
        IslamicIntent.LETTER -> listOf("اجعل الرسالة موجهة للوالدين", "اجعل الرسالة قصيرة ومختصرة")
        IslamicIntent.KHATIRA -> listOf("أضف دعاءً في الخاتمة", "اكتب خاطرة أخرى عن التفاؤل")
        IslamicIntent.KHUTBAH -> listOf("أضف آية قرآنية وحديثًا آخرين", "لخّص الخطبة في نقاط قصيرة")
        IslamicIntent.LECTURE -> listOf("أضف محاور تطبيقية عملية", "حوّل الموضوع إلى خطبة جمعة")
        else -> listOf("اذكر دليلًا قرآنيًا آخر", "اكتب خاطرة إيمانية حول هذا المعنى")
    }
}
