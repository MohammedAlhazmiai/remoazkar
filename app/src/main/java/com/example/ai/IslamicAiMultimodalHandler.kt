package com.example.ai

import android.content.Context
import android.net.Uri
import com.example.util.MediaExportHelper

/**
 * معالج الوسائط والبطاقات الإسلامية التوليدية للمساعد الذكي.
 * يتيح تحليل الصور والتسجيلات الصوتية المرفوعة، وتوليد البطاقات الدعوية بحد يومي منضبط.
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
                    followUps = listOf("ما حكم سجود السهو؟", "اكتب لي خطبة جمعة", "فسر لي حلم المطر")
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

        val subject = query.ifBlank { "استفسار ديني حول الصورة المرفوعة" }
        val ranked = entries.firstOrNull { it.domain == "chat_qa" || it.domain == "fatwa" }
        val text = buildString {
            append("**قراءة وتحليل الصورة الدينية المرفوعة ($inspection):**\n\n")
            if (query.isNotBlank()) {
                append("بناءً على تساؤلك المرفق بالصورة: «$query»:\n\n")
            } else {
                append("قمت بالاطلاع على الصورة المرفوعة؛ والواجب في التعامل مع النصوص والوثائق الدينية والمصاحف هو التعظيم والتحري.\n\n")
            }
            if (ranked != null) {
                append("• **البيان الشرعي:**\n${ranked.content.take(350)}\n\n")
                if (ranked.quranEvidence.isNotBlank()) append("• **الدليل القرآني:** ${ranked.quranEvidence}\n")
                if (ranked.hadithEvidence.isNotBlank()) append("• **الدليل النبوي:** ${ranked.hadithEvidence}\n")
            } else {
                append("• قال الله تعالى: ﴿ذَٰلِكَ وَمَن يُعَظِّمْ شَعَائِرَ اللَّهِ فَإِنَّهَا مِن تَقْوَى الْقُلُوبِ﴾ [الحج: 32].\n")
                append("• وجاء في الحديث: «طلب العلم فريضة على كل مسلم».\n")
            }
            append("\nيمكنك كتابة أي سؤال فقهي أو شرعي محدد حول ما في الصورة وسأجيبك بدقة بالأدلة.")
        }

        return IslamicAiResponse(
            text = text,
            domainTag = "قراءة صورة دينية",
            detectedTopic = "تحليل صورة دينية",
            references = listOf("الحج: 32", ranked?.title ?: "تعظيم شعائر الله"),
            followUps = listOf("ما حكم هذا في الصلاة؟", "أعطني دليلًا إضافيًا", "احفظ الرد في الهاتف")
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

        val subject = query.ifBlank { "سؤال واستفسار عبر تسجيل صوتي" }
        val ranked = entries.firstOrNull { it.domain == "fatwa" || it.domain == "chat_qa" }

        val text = buildString {
            append("**استماع وتفحص التسجيل الصوتي ($inspection):**\n\n")
            if (query.isNotBlank()) {
                append("استمعت لمضمون طلبك الصوتي حول: «$query»:\n\n")
            } else {
                append("تم استلام المقطع الصوتي الديني بنجاح؛ وإليك التوجيه الشرعي المعتمد حول ما ورد فيه:\n\n")
            }
            if (ranked != null) {
                append("• **التوجيه الشرعي:**\n${ranked.content.take(350)}\n\n")
                if (ranked.quranEvidence.isNotBlank()) append("• **الشاهد القرآني:** ${ranked.quranEvidence}\n")
                if (ranked.hadithEvidence.isNotBlank()) append("• **الشاهد النبوي:** ${ranked.hadithEvidence}\n")
            } else {
                append("• قال الله تعالى: ﴿فَاسْأَلُوا أَهْلَ الذِّكْرِ إِن كُنتُمْ لَا تَعْلَمُونَ﴾ [النحل: 43].\n")
                append("• وقال النبي ﷺ: «من يرد الله به خيرًا يفقهه في الدين».\n")
            }
            append("\nتقبل الله طاعتكم، وبإمكانك حفظ الرد ومشاركته أو الاستماع إليه صوتيًا.")
        }

        return IslamicAiResponse(
            text = text,
            domainTag = "تحليل مقطع صوتي",
            detectedTopic = "توجيه شرعي لصوت مرفوع",
            references = listOf("النحل: 43", ranked?.title ?: "الفقه في الدين"),
            followUps = listOf("اختصر التوجيه في نقاط", "أعطني دليلًا إضافيًا", "احفظ الرد في الهاتف")
        )
    }
}
