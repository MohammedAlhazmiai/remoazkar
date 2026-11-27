package com.example.data

import android.content.Context

data class ShortTextZikr(
    val title: String,
    val text: String
)

/**
 * قائمة الأذكار النصية القصيرة التي تظهر في إشعار نصي منفصل في الأعلى
 * بالتزامن مع نطق الأذكار الصوتية البشرية، وفي التذكيرات الدورية العامة
 * (بعيدًا عن أذكار الصباح والمساء التي تظهر في أوقات قراءتها المحددة فقط).
 */
object ShortTextAzkarCatalog {

    private const val PREFS_NAME = "rim_short_text_azkar_prefs"
    private const val KEY_NEXT_INDEX = "next_text_zikr_index"

    val items: List<ShortTextZikr> = listOf(
        ShortTextZikr("تسبيح", "سبحان الله"),
        ShortTextZikr("تحميد", "الحمد لله"),
        ShortTextZikr("تكبير", "الله أكبر"),
        ShortTextZikr("كلمة التوحيد", "لا إله إلا الله محمد رسول الله"),
        ShortTextZikr("تذكير بالذكر", "اذكروا الله"),
        ShortTextZikr("الصلاة على النبي ﷺ", "صلّوا على رسول الله ﷺ"),
        ShortTextZikr("الصلاة على النبي ﷺ", "اللهم صلِّ على نبينا محمد"),
        ShortTextZikr("استغفار", "أستغفر الله"),
        ShortTextZikr("دعاء جامع", "اللهم إنا نسألك الجنة ونعوذ بك من النار"),
        ShortTextZikr("كلمتان خفيفتان على اللسان", "سبحان الله وبحمده، سبحان الله العظيم"),
        ShortTextZikr("تسبيح وبحمده", "سبحان الله وبحمده"),
        ShortTextZikr("تعظيم الله", "سبحان الله العظيم"),
        ShortTextZikr("كنز من كنوز الجنة", "لا حول ولا قوة إلا بالله"),
        ShortTextZikr("استغفار وتوبة", "أستغفر الله العظيم وأتوب إليه"),
        ShortTextZikr("توكل على الله", "حسبي الله لا إله إلا هو عليه توكلت وهو رب العرش العظيم"),
        ShortTextZikr("استغاثة ودعاء", "يا حي يا قيوم برحمتك أستغيث")
    )

    fun nextItem(context: Context): ShortTextZikr {
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val idx = sp.getInt(KEY_NEXT_INDEX, 0).coerceIn(0, items.lastIndex)
        sp.edit().putInt(KEY_NEXT_INDEX, (idx + 1) % items.size).apply()
        return items[idx]
    }
}
