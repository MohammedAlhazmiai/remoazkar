package com.example.data

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * ميزة "خطة الورد اليومي" — غير موجودة في التطبيقات المشابهة:
 * خطة روحية يومية مع تتبّع تقدّم رقمي لكل مهمة،
 * وسلسلة أيام متتالية (Streak) تُحفّز على الاستمرار،
 * وتُصفَّر تلقائيًا كل يوم جديد مع الاحتفاظ بالسلسلة.
 */
data class WirdTask(
    val id: String,
    val title: String,
    val hint: String,
    val target: Int,
    val unit: String
)

object DailyWirdStore {

    private const val PREFS = "rim_wird_plan"
    private const val KEY_PROGRESS = "progress"
    private const val KEY_STREAK = "streak"
    private const val KEY_LAST_DONE = "last_done_date"

    val tasks = listOf(
        WirdTask("quran", "ورد القرآن", "اقرأ صفحة واحدة على الأقل بتدبّر", 1, "صفحة"),
        WirdTask("morning", "أذكار الصباح", "باب كامل من حصن المسلم", 1, "باب"),
        WirdTask("evening", "أذكار المساء", "باب كامل من حصن المسلم", 1, "باب"),
        WirdTask("istighfar", "الاستغفار", "استغفر الله العظيم وأتوب إليه", 100, "مرة"),
        WirdTask("salawat", "الصلاة على النبي", "اللهم صلِّ وسلِّم على نبينا محمد", 100, "مرة"),
        WirdTask("kahf", "سورة الكهف", "تُستحبّ يوم الجمعة خاصة", 1, "سورة")
    )

    fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun yesterdayKey(): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun todayProgress(context: Context): Map<String, Int> {
        val raw = prefs(context).getString(KEY_PROGRESS, null) ?: return emptyMap()
        return try {
            val root = JSONObject(raw)
            val day = root.optJSONObject(todayKey()) ?: return emptyMap()
            val out = HashMap<String, Int>()
            day.keys().forEach { k -> out[k] = day.optInt(k, 0) }
            out
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun setValue(context: Context, taskId: String, value: Int) {
        val p = prefs(context)
        val root = try {
            JSONObject(p.getString(KEY_PROGRESS, null) ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
        val day = root.optJSONObject(todayKey()) ?: JSONObject()
        day.put(taskId, value.coerceAtLeast(0))
        root.put(todayKey(), day)
        p.edit().putString(KEY_PROGRESS, root.toString()).apply()
        refreshStreak(context)
    }

    fun increment(context: Context, taskId: String, delta: Int = 1) {
        val current = todayProgress(context)[taskId] ?: 0
        setValue(context, taskId, current + delta)
    }

    fun percent(context: Context): Int {
        val progress = todayProgress(context)
        var done = 0
        var total = 0
        tasksToday().forEach { t ->
            total += t.target
            done += (progress[t.id] ?: 0).coerceAtMost(t.target)
        }
        if (total == 0) return 0
        return ((done * 100) / total).coerceIn(0, 100)
    }

    fun isComplete(context: Context): Boolean = percent(context) >= 100

    fun streak(context: Context): Int {
        val p = prefs(context)
        val last = p.getString(KEY_LAST_DONE, null)
        val streak = p.getInt(KEY_STREAK, 0)
        return if (last == todayKey() || last == yesterdayKey()) streak else 0
    }

    private fun refreshStreak(context: Context) {
        if (!isComplete(context)) return
        val p = prefs(context)
        val last = p.getString(KEY_LAST_DONE, null)
        if (last == todayKey()) return
        val newStreak = if (last == yesterdayKey()) p.getInt(KEY_STREAK, 0) + 1 else 1
        p.edit()
            .putInt(KEY_STREAK, newStreak)
            .putString(KEY_LAST_DONE, todayKey())
            .apply()
    }

    fun isFriday(): Boolean =
        Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY

    fun tasksToday(): List<WirdTask> = tasks.filter { it.id != "kahf" || isFriday() }
}
