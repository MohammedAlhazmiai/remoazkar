package com.example.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import java.io.File

/**
 * خلفية الصفحات.
 *
 * لكل صفحة في التطبيق خلفية مستقلة تمامًا: يمكن للمستخدم اختيار صورة مختلفة لكل صفحة،
 * أو تعيين نفس الصورة لعدة صفحات بتحديد أكثر من مربع ✔، أو إزالة خلفية صفحة دون غيرها.
 * تُنسخ الصور إلى ذاكرة التطبيق الداخلية (filesDir) حتى لا تنتهي صلاحية مسار المعرض،
 * وتُحفظ قائمة الصفحات المفعّلة في SharedPreferences.
 */
object AppBackgroundStore {

    private const val PREFS = "rim_app_background"
    private const val KEY_ENABLED_PAGES = "enabled_pages"
    private const val KEY_SCRIM = "background_scrim"
    private const val FILE_PREFIX = "bg_page_"
    private const val LOG_TAG = "AppBackgroundStore"

    const val DEFAULT_SCRIM = 35

    /** صفحة واحدة قابلة لتخصيص الخلفية. */
    data class BackgroundPage(val key: String, val title: String)

    /**
     * كل صفحات التطبيق التي يمكن تغيير خلفيتها. الترتيب هو ترتيب ظهورها في قائمة الاختيار.
     */
    val pages: List<BackgroundPage> = listOf(
        BackgroundPage("home", "الرئيسية"),
        BackgroundPage("quran", "القرآن الكريم"),
        BackgroundPage("quran_reader", "قراءة المصحف"),
        BackgroundPage("hisn", "حصن المسلم"),
        BackgroundPage("hisn_chapter", "باب من حصن المسلم"),
        BackgroundPage("tasbeeh", "المسبحة"),
        BackgroundPage("reminders", "التذكيرات"),
        BackgroundPage("hijri", "التقويم الهجري"),
        BackgroundPage("custom_azkar", "أذكاري الخاصة"),
        BackgroundPage("ai_robot", "القارئ الذكي"),
        BackgroundPage("wird", "الورد اليومي"),
        BackgroundPage("ruqyah", "الرقية الشرعية"),
        BackgroundPage("adiya", "الأدعية"),
        BackgroundPage("spiritual_companion", "الرفيق الروحي"),
        BackgroundPage("khatmah", "خطة الختمة"),
        BackgroundPage("names_of_allah", "أسماء الله الحسنى"),
        BackgroundPage("prophet_duas", "أدعية النبي ﷺ"),
        BackgroundPage("audio_azkar", "الأذكار الصوتية"),
        BackgroundPage("path_to_success", "طريق الناجحين"),
        BackgroundPage("age_calculator", "حاسبة العمر"),
        BackgroundPage("search", "البحث"),
        BackgroundPage("favorites", "المفضلة"),
        BackgroundPage("exclusive", "الميزات الحصرية"),
        BackgroundPage("advanced_settings", "الإعدادات المتقدمة"),
        BackgroundPage("about", "عن التطبيق"),
        BackgroundPage("support_developer", "دعم المطور"),
        BackgroundPage("dev_link", "صفحات المطور")
    )

    /** يُزاد في كل تغيير حتى تُعيد الواجهة قراءة الصور. */
    var revision by mutableIntStateOf(0)
        private set

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun fileNameFor(pageKey: String) = FILE_PREFIX + sanitize(pageKey) + ".img"

    private fun sanitize(key: String) = key.replace(Regex("[^A-Za-z0-9_]"), "_")

    private fun file(context: Context, pageKey: String) = File(context.filesDir, fileNameFor(pageKey))

    /** تحويل مسار التنقل (route) إلى مفتاح صفحة معروف. */
    fun pageKeyForRoute(route: String?): String? {
        val raw = route?.substringBefore('/')?.substringBefore('?') ?: return null
        return when (raw) {
            "splash" -> null
            "quran_reader" -> "quran_reader"
            "hisn_chapter" -> "hisn_chapter"
            "dev_link" -> "dev_link"
            else -> if (pages.any { it.key == raw }) raw else null
        }
    }

    fun titleFor(pageKey: String): String =
        pages.firstOrNull { it.key == pageKey }?.title ?: pageKey

    /** مسار الصورة المحفوظة للصفحة إن وُجدت فعلًا على القرص. */
    fun imagePath(context: Context, pageKey: String): String? {
        val target = file(context, pageKey)
        return if (target.exists() && target.length() > 0) target.absolutePath else null
    }

    fun hasImage(context: Context, pageKey: String): Boolean = imagePath(context, pageKey) != null

    private fun enabledPages(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_ENABLED_PAGES, emptySet()) ?: emptySet()

    /** هل خلفية هذه الصفحة مفعّلة ولها صورة؟ */
    fun isEnabled(context: Context, pageKey: String): Boolean =
        enabledPages(context).contains(pageKey) && imagePath(context, pageKey) != null

    /** هل توجد أي خلفية مفعّلة في التطبيق؟ */
    fun hasAnyEnabled(context: Context): Boolean =
        enabledPages(context).any { imagePath(context, it) != null }

    fun isPageSelected(context: Context, pageKey: String): Boolean =
        enabledPages(context).contains(pageKey)

    fun scrimPercent(context: Context): Int =
        prefs(context).getInt(KEY_SCRIM, DEFAULT_SCRIM).coerceIn(0, 85)

    fun setPageEnabled(context: Context, pageKey: String, enabled: Boolean) {
        val current = enabledPages(context).toMutableSet()
        if (enabled) current.add(pageKey) else current.remove(pageKey)
        prefs(context).edit().putStringSet(KEY_ENABLED_PAGES, current).apply()
        revision++
    }

    fun setScrimPercent(context: Context, percent: Int) {
        prefs(context).edit().putInt(KEY_SCRIM, percent.coerceIn(0, 85)).apply()
        revision++
    }

    /**
     * ينسخ الصورة المختارة إلى ذاكرة التطبيق كخلفية للصفحة المحددة ويعيد نجاح العملية.
     */
    fun save(context: Context, pageKey: String, uri: Uri): Boolean {
        return try {
            val target = file(context, pageKey)
            val input = context.contentResolver.openInputStream(uri) ?: return false
            input.use { source ->
                target.outputStream().use { sink -> source.copyTo(sink) }
            }
            // التأكد أن الملف صورة صالحة قبل اعتمادها كخلفية.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(target.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                target.delete()
                return false
            }
            val current = enabledPages(context).toMutableSet()
            current.add(pageKey)
            prefs(context).edit().putStringSet(KEY_ENABLED_PAGES, current).apply()
            revision++
            true
        } catch (error: Exception) {
            Log.e(LOG_TAG, "Unable to save background image for $pageKey", error)
            false
        }
    }

    fun clearPage(context: Context, pageKey: String) {
        runCatching { file(context, pageKey).delete() }
        val current = enabledPages(context).toMutableSet()
        current.remove(pageKey)
        prefs(context).edit().putStringSet(KEY_ENABLED_PAGES, current).apply()
        revision++
    }

    fun clearAll(context: Context) {
        pages.forEach { page -> runCatching { file(context, page.key).delete() } }
        prefs(context).edit().remove(KEY_ENABLED_PAGES).apply()
        revision++
    }
}
