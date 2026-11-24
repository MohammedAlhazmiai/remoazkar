package com.example.ai

import android.content.Context
import android.net.Uri
import com.example.util.MediaExportHelper

/**
 * معالج الوسائط والبطاقات الإسلامية التوليدية للمساعد الذكي.
 * يعمل في الوضع المحلي عند انقطاع الإنترنت أو عند طلب تصميم البطاقات الدعوية بحد يومي منضبط.
 */
object IslamicAiMultimodalHandler {

    fun isGenericMediaInquiry(q: String): Boolean {
        val norm = ArabicNlpEngine.normalize(q)
        return norm.isBlank() || norm.contains("صوره") || norm.contains("الصوره") ||
            norm.contains("صوت") || norm.contains("التسجيل") || norm.contains("المقطع") ||
            norm.contains("اقرا") || norm.contains("اسمع") || norm.contains("حلل") ||
            norm.contains("شوف") || norm.contains("رايك")
    }

    fun composeImageDesignReply(
        query: String,
        context: Context?,
        entries: List<CorpusEntry>
    ): IslamicAiResponse {
        val norm = ArabicNlpEngine.normalize(query)
        val isJumuah = norm.contains("جمعه")
        val isQuran = norm.contains("ايه") || norm.contains("قران")
        val isHadith = norm.contains("حديث")
        val isEid = norm.contains("عيد") || norm.contains("رمضان")

        val categoryTitle = when {
            isJumuah -> "جمعة مباركة"
            isQuran -> "آية كريمة"
            isHadith -> "حديث شريف"
            isEid -> "تهنئة مباركة"
            else -> "زاد المسلم"
        }

        val mainText = when {
            isJumuah -> "«اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ»\n\nنورٌ ما بين الجمعتين، وساعة إجابة لا يُرد فيها سائل."
            isQuran -> "﴿وَإِذَا سَأَلَكَ عِبَادِي عَنِّي فَإِنِّي قَرِيبٌ أُجِيبُ دَعْوَةَ الدَّاعِ إِذَا دَعَانِ﴾"
            isHadith -> "«اتَّقِ اللَّهَ حَيْثُمَا كُنْتَ، وَأَتْبِعِ السَّيِّئَةَ الْحَسَنَةَ تَمْحُهَا، وَخَالِقِ النَّاسَ بِخُلُقٍ حَسَنٍ»"
            isEid -> "تقبل الله منا ومنكم صالح الأعمال، وجعل أيامكم عامرةً بالطاعة والمسرات."
            else -> "«حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ»\n\nمن توكل على الله كفاه، ومن استعان به هداه ووفقه."
        }

        val reference = when {
            isJumuah -> "سنن أبي داود • سورة الكهف"
            isQuran -> "سورة البقرة: 186"
            isHadith -> "رواه الترمذي وقال حديث حسن"
            isEid -> "زاد المسلم"
            else -> "سورة آل عمران: 173"
        }

        val theme = if (isJumuah || isQuran) "EMERALD" else if (isEid) "ROYAL_BLUE" else "MAROON"

        if (context != null) {
            if (!IslamicImageQuotaManager.canGenerateToday(context)) {
                return IslamicAiResponse(
                    text = "عذرًا أخي الكريم، لقد بلغت الحد اليومي الأقصى المسموح به لتوليد البطاقات والصور الدعوية لهذا اليوم (${IslamicImageQuotaManager.DAILY_LIMIT} بطاقات يوميًا) لترشيد موارد الهاتف.\n\nيتجدد رصيدك تلقائيًا غدًا بإذن الله. ويسعدني الاستمرار في خدمتك لكافة الفتاوى والاستفسارات والخطب والرسائل دون أي قيود.",
                    domainTag = IslamicIntent.IMAGE_DESIGN.arabicLabel,
                    detectedTopic = "اكتمال الحصة اليومية للبطاقات",
                    references = emptyList(),
                    followUps = listOf("ما حكم سجود السهو؟", "اكتب لي خطبة جمعة", "فسر لي حلم الذبح")
                )
            }

            val cardFile = IslamicCardGenerator.generateCard(
                context,
                IslamicCardRequest(categoryTitle, mainText, reference, theme)
            )
            val remaining = IslamicImageQuotaManager.recordGeneration(context)

            return IslamicAiResponse(
                text = "**تم بحمد الله تصميم البطاقة الدعوية الإسلامية بنجاح:**\n\n" +
                    "• **العنوان:** $categoryTitle\n" +
                    "• **المحتوى:** $mainText\n" +
                    "• **المصدر:** $reference\n\n" +
                    "تم إنشاء وتجهيز الصورة بدقة عالية داخل هاتفك (المتبقي من حصتك اليومية: $remaining بطاقات).\n" +
                    "يمكنك الضغط على زر **«حفظ في الهاتف»** لحفظها مباشرة في معرض الصور بهاتفك أو مشاركتها عبر واتساب وتطبيقات التواصل.",
                domainTag = IslamicIntent.IMAGE_DESIGN.arabicLabel,
                detectedTopic = categoryTitle,
                references = listOf(reference),
                followUps = listOf("صمم بطاقة آية قرآنية", "صمم بطاقة حديث شريف", "صمم بطاقة جمعة مباركة"),
                attachmentType = "card",
                attachmentUri = cardFile.absolutePath
            )
        }

        return IslamicAiResponse(
            text = "**مخطط تصميم البطاقة الدعوية الإسلامية:**\n\n• **العنوان:** $categoryTitle\n• **المحتوى:** $mainText\n• **المصدر:** $reference",
            domainTag = IslamicIntent.IMAGE_DESIGN.arabicLabel,
            detectedTopic = categoryTitle,
            references = listOf(reference),
            followUps = listOf("صمم بطاقة آية قرآنية", "صمم بطاقة حديث شريف")
        )
    }

    fun composeImageAnalysisReply(
        query: String,
        uriString: String,
        context: Context?,
        entries: List<CorpusEntry>
    ): IslamicAiResponse {
        val inspection = if (context != null && uriString.isNotBlank()) {
            MediaExportHelper.inspectImage(context, Uri.parse(uriString))
        } else {
            "صورة مرفوعة من الهاتف"
        }

        val matchedEntry = findBestEntryForQuery(query, entries)
        val text = buildString {
            append("**استلام الصورة المرفقة ($inspection) في الوضع غير المتصل بالإنترنت:**\n\n")
            append("تم استلام ملف الصورة بنجاح. يتطلب الفهم البصري الدقيق لمحتوى الصورة وتفاصيلها اتصالًا بالإنترنت لتشغيل نموذج الرؤية السحابي (Gemini Vision)، أما أثناء العمل بدون إنترنت فإليك المساعدة المتاحة محليًا:\n\n")
            if (matchedEntry != null) {
                append("• **بناءً على سؤالك («$query») — ${matchedEntry.title}:**\n${matchedEntry.content}\n\n")
                if (matchedEntry.quranEvidence.isNotBlank()) append("• **الدليل القرآني:** ${matchedEntry.quranEvidence}\n")
                if (matchedEntry.hadithEvidence.isNotBlank()) append("• **الدليل النبوي:** ${matchedEntry.hadithEvidence}\n")
            } else {
                append("• إذا كانت الصورة تحتوي على نص قرآني أو مسألة شرعية، اكتب فحوى ما فيها أو اتصل بالإنترنت وأعد إرسالها ليقرأها المساعد الذكي تلقائيًا بالكامل.\n")
                append("• قال الله تعالى: ﴿ذَٰلِكَ وَمَن يُعَظِّمْ شَعَائِرَ اللَّهِ فَإِنَّهَا مِن تَقْوَى الْقُلُوبِ﴾ [الحج: 32].")
            }
        }.trim()

        return IslamicAiResponse(
            text = text,
            domainTag = "قراءة صورة دينية",
            detectedTopic = matchedEntry?.title ?: "معالجة صورة مرفوعة (وضع محلي)",
            references = listOfNotNull(matchedEntry?.title, "الحج: 32"),
            followUps = listOf("اكتب نص المسألة الموجودة في الصورة", "صمم بطاقة دعوية إسلامية")
        )
    }

    fun composeAudioAnalysisReply(
        query: String,
        uriString: String,
        context: Context?,
        entries: List<CorpusEntry>
    ): IslamicAiResponse {
        val inspection = if (context != null && uriString.isNotBlank()) {
            MediaExportHelper.inspectAudio(context, Uri.parse(uriString))
        } else {
            "تسجيل صوتي مرفوع من الهاتف"
        }

        val matchedEntry = findBestEntryForQuery(query, entries)
        val text = buildString {
            append("**استلام الملف الصوتي ($inspection) في الوضع غير المتصل بالإنترنت:**\n\n")
            append("تم حفظ وقراءة بيانات الملف الصوتي بنجاح. يتطلب الاستماع الذكي لمحتوى التسجيل الصوتي وفهم التلاوة أو السؤال بالعامية داخل الملف اتصالًا بالإنترنت لتشغيل نموذج الصوت السحابي (Gemini Multimodal).\n\n")
            if (matchedEntry != null) {
                append("• **بناءً على النص المرافق («$query») — ${matchedEntry.title}:**\n${matchedEntry.content}\n\n")
                if (matchedEntry.quranEvidence.isNotBlank()) append("• **الشاهد القرآني:** ${matchedEntry.quranEvidence}\n")
                if (matchedEntry.hadithEvidence.isNotBlank()) append("• **الشاهد النبوي:** ${matchedEntry.hadithEvidence}\n")
            } else {
                append("• أثناء عدم توفر الإنترنت يمكنك الضغط على **زر المايك** للتحدث مباشرة وتحويل صوتك إلى نص فوري يفهمه المساعد المحلي فورًا، أو توصيل الإنترنت وإعادة إرسال التسجيل الصوتي.\n")
                append("• قال الله تعالى: ﴿فَاسْأَلُوا أَهْلَ الذِّكْرِ إِن كُنتُمْ لَا تَعْلَمُونَ﴾ [النحل: 43].")
            }
        }.trim()

        return IslamicAiResponse(
            text = text,
            domainTag = "تحليل مقطع صوتي",
            detectedTopic = matchedEntry?.title ?: "معالجة ملف صوتي (وضع محلي)",
            references = listOfNotNull(matchedEntry?.title, "النحل: 43"),
            followUps = listOf("اكتب خلاصة السؤال الصوتي نصيًا", "ما آداب تلاوة القرآن والاستماع إليه؟")
        )
    }

    private fun findBestEntryForQuery(query: String, entries: List<CorpusEntry>): CorpusEntry? {
        if (query.isBlank() || isGenericMediaInquiry(query)) return null
        val qIdx = ArabicNlpEngine.indexText(query)
        val qVec = NeuralArabicLanguageCore.encodeFromIndexed(qIdx)
        return entries.map { entry ->
            entry to ArabicNlpEngine.scoreSemanticMatch(
                queryIndexed = qIdx,
                titleIndexed = entry.titleIndexed,
                keywordsIndexed = entry.keywordsIndexed,
                bodyIndexed = entry.bodyIndexed,
                queryEmbedding = qVec,
                entryEmbedding = entry.denseEmbedding
            )
        }.maxByOrNull { it.second }?.takeIf { it.second >= 5.5 }?.first
    }
}
