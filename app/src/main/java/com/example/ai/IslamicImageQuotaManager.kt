package com.example.ai

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * مدير الحصة اليومية لتوليد البطاقات والصور الدعوية الإسلامية.
 * يحدد سقفًا يوميًا معقولًا (5 بطاقات يوميًا) لترشيد موارد الجهاز مع تلبية الاحتياج الديني.
 */
object IslamicImageQuotaManager {

    const val DAILY_LIMIT = 5
    private const val PREFS_NAME = "islamic_ai_image_quota"
    private const val KEY_LAST_DATE = "last_generation_date"
    private const val KEY_TODAY_COUNT = "today_generation_count"

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun getTodayCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = todayKey()
        val lastDate = prefs.getString(KEY_LAST_DATE, "")
        return if (lastDate == today) {
            prefs.getInt(KEY_TODAY_COUNT, 0)
        } else {
            0
        }
    }

    fun canGenerateToday(context: Context): Boolean =
        getTodayCount(context) < DAILY_LIMIT

    fun getRemainingQuota(context: Context): Int =
        (DAILY_LIMIT - getTodayCount(context)).coerceAtLeast(0)

    fun recordGeneration(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = todayKey()
        val currentCount = getTodayCount(context)
        val nextCount = currentCount + 1
        prefs.edit()
            .putString(KEY_LAST_DATE, today)
            .putInt(KEY_TODAY_COUNT, nextCount)
            .apply()
        return (DAILY_LIMIT - nextCount).coerceAtLeast(0)
    }
}
