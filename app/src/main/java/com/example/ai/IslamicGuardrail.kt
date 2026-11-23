package com.example.ai

/**
 * يوجّه المساعد إلى إجابة الأسئلة اليومية والدنيوية بمنظور إسلامي مناسب،
 * ويمنع تقديم تعليمات عملية صريحة للاختراق أو الإضرار.
 */
object IslamicGuardrail {

    val APOLOGY_RESPONSE = """
        لا أستطيع المساعدة بخطوات لاختراق حساب أو شبكة أو الإضرار بالآخرين.
        يمكنني مساعدتك في حماية حساباتك وأجهزتك، أو مناقشة الجانب الأخلاقي والشرعي للموضوع بأمان.
    """.trimIndent()

    private val UNSAFE_ACTION_TERMS = listOf(
        "اختراق", "تهكير", "اهكر", "سرقة حساب", "كسر كلمة المرور", "اصنع قنبلة", "تفجير"
    )

    private val SAFE_DISCUSSION_SIGNALS = listOf(
        "ما حكم", "هل يجوز", "حكم الشرع", "فتوى", "حلال ام حرام", "في الاسلام", "شرعا",
        "منظور اسلامي", "نصيحه", "احمي", "حمايه", "تامين", "وقايه", "اتجنب", "امنع"
    )

    /** تقبل الرسائل العادية لتوجيهها محليًا أو إلى Gemini بصياغة إسلامية مناسبة. */
    @Suppress("UNUSED_PARAMETER")
    fun isAllowedQuery(rawQuery: String, hasPriorContext: Boolean = false): Boolean {
        val norm = ArabicNlpEngine.normalize(rawQuery)
        if (norm.isBlank()) return true

        val requestsUnsafeAction = UNSAFE_ACTION_TERMS.any { term ->
            norm.contains(ArabicNlpEngine.normalize(term))
        }
        if (!requestsUnsafeAction) return true

        // يمكن مناقشة حكم هذه الأفعال أو الوقاية منها، لكن لا تُقدَّم خطوات تنفيذية ضارة.
        return SAFE_DISCUSSION_SIGNALS.any { signal ->
            norm.contains(ArabicNlpEngine.normalize(signal))
        }
    }
}
