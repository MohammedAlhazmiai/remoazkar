package com.example.data

import android.content.Context

/**
 * يحفظ عدّاد التكرار المخصّص لكل ذكر داخل صفحات الأذكار (الصباح/المساء وبقية الأبواب).
 *
 * القيمة الافتراضية تأتي من ملف hisn.json، ويمكن للمستخدم تعديلها أو إضافتها لأي حقل،
 * ثم تُحفظ هنا محليًا على الجهاز وتبقى بعد إغلاق التطبيق.
 */
object AzkarRepeatStore {

    private const val PREFS = "rim_azkar_repeat_overrides"

    private fun key(chapterId: Int, index: Int): String = "repeat_${chapterId}_$index"

    /** يعيد عدد التكرار المخصّص إن وُجد، وإلا القيمة الافتراضية. */
    fun effective(context: Context, chapterId: Int, index: Int, default: Int): Int {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(key(chapterId, index), -1)
        return if (stored > 0) stored else default.coerceAtLeast(1)
    }

    /** يضبط عدد تكرار مخصّصًا (1 فأكثر). */
    fun set(context: Context, chapterId: Int, index: Int, value: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(key(chapterId, index), value.coerceIn(1, 10_000))
            .apply()
    }

    /** يعيد الذكر إلى عدده الافتراضي القادم من الملف. */
    fun reset(context: Context, chapterId: Int, index: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(key(chapterId, index))
            .apply()
    }
}
