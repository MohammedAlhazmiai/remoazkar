package com.example.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.example.R
import java.io.File
import kotlin.random.Random

/**
 * خلفية الصفحات وإدارة الخلفيات الإسلامية المدمجة.
 *
 * - توفّر خلفيات إسلامية دينية مناسبة لكل صفحات التطبيق مع إمكانية تفعيلها أو إيقافها.
 * - تدعم التبديل التلقائي لخلفية الشاشة الرئيسية إلى خلفية مختلفة عند الرجوع إليها من أي صفحة.
 * - تحفظ لكل صفحة خيار تعيين صورة مخصصة من معرض الهاتف أو اختيار أحد القوالب الإسلامية.
 */
object AppBackgroundStore {

    private const val PREFS = "rim_app_background"
    private const val KEY_ENABLED_PAGES = "enabled_pages"
    private const val KEY_SCRIM = "background_scrim"
    private const val KEY_ISLAMIC_BG_ENABLED = "islamic_bg_enabled"
    private const val KEY_BUILTIN_PREFIX = "builtin_bg_"
    private const val FILE_PREFIX = "bg_page_"
    private const val LOG_TAG = "AppBackgroundStore"

    const val DEFAULT_SCRIM = 42

    /** صفحة واحدة قابلة لتخصيص الخلفية. */
    data class BackgroundPage(val key: String, val title: String)

    /** خلفية إسلامية مدمجة في التطبيق. */
    data class BuiltInIslamicBackground(
        val id: String,
        val title: String,
        @DrawableRes val drawableRes: Int
    )

    /**
     * مجموعة الخلفيات الإسلامية الدينية المولّدة والمصممة لصفحات التطبيق.
     */
    val builtInBackgrounds: List<BuiltInIslamicBackground> = listOf(
        BuiltInIslamicBackground("mosque", "أروقة المسجد", R.drawable.img_islamic_bg_mosque),
        BuiltInIslamicBackground("arabesque", "زخرفة إسلامية", R.drawable.img_islamic_bg_arabesque),
        BuiltInIslamicBackground("night", "هلال وليلة", R.drawable.img_islamic_bg_night),
        BuiltInIslamicBackground("lantern", "فانوس النور", R.drawable.img_islamic_bg_lantern),
        BuiltInIslamicBackground("courtyard", "نور المحراب", R.drawable.img_islamic_bg_courtyard),
        BuiltInIslamicBackground("sanctuary", "الروضة الهادئة", R.drawable.home_background)
    )

    /**
     * كل صفحات التطبيق التي يمكن تغيير خلفيتها. الترتيب هو ترتيب ظهورها في قائمة الاختيار.
     */
    val pages: List<BackgroundPage> = listOf(
        BackgroundPage("home", "الرئيسية"),
        BackgroundPage("prayer_times", "مواقيت الصلاة"),
        BackgroundPage("qibla", "اتجاه القبلة"),
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

    /** فهرس الخلفية الإسلامية المعروضة حاليًا على الشاشة الرئيسية. */
    var currentHomeBgIndex by mutableIntStateOf(0)
        private set

    @DrawableRes
    fun currentHomeDrawable(): Int =
        builtInBackgrounds.getOrElse(currentHomeBgIndex.mod(builtInBackgrounds.size)) {
            builtInBackgrounds.first()
        }.drawableRes

    /**
     * يبدّل خلفية الشاشة الرئيسية إلى خلفية إسلامية مختلفة حتمًا عن الخلفية السابقة.
     */
    fun rotateHomeBackground() {
        val count = builtInBackgrounds.size
        if (count <= 1) return
        val candidates = (0 until count).filter { it != currentHomeBgIndex }
        currentHomeBgIndex = candidates[Random.nextInt(candidates.size)]
    }

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

    /** هل خلفية هذه الصفحة مفعّلة ولها صورة من المعرض؟ */
    fun isEnabled(context: Context, pageKey: String): Boolean =
        enabledPages(context).contains(pageKey) && imagePath(context, pageKey) != null

    /** هل توجد أي خلفية مخصصة مفعّلة في التطبيق؟ */
    fun hasAnyEnabled(context: Context): Boolean =
        enabledPages(context).any { imagePath(context, it) != null }

    fun isPageSelected(context: Context, pageKey: String): Boolean =
        enabledPages(context).contains(pageKey)

    fun isIslamicBackgroundsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ISLAMIC_BG_ENABLED, true)

    fun setIslamicBackgroundsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ISLAMIC_BG_ENABLED, enabled).apply()
        revision++
    }

    /** حفظ اختيار خلفية إسلامية مدمجة لصفحة معيّنة (أو null للعودة للتلقائي). */
    fun setBuiltInForPage(context: Context, pageKey: String, bgId: String?) {
        val editor = prefs(context).edit()
        if (bgId == null) {
            editor.remove(KEY_BUILTIN_PREFIX + pageKey)
        } else {
            editor.putString(KEY_BUILTIN_PREFIX + pageKey, bgId)
            // إذا كان المستخدم يختار قالبًا إسلاميًا مدمجًا نزيل تعطيل الخلفية الإسلامية العامة
            editor.putBoolean(KEY_ISLAMIC_BG_ENABLED, true)
        }
        editor.apply()
        revision++
    }

    /** الخلفية الإسلامية الافتراضية المناسبة لكل صفحة من صفحات التطبيق. */
    @DrawableRes
    fun defaultDrawableForPage(pageKey: String?): Int? {
        return when (pageKey) {
            null, "home" -> null
            "quran", "quran_reader", "khatmah" -> R.drawable.img_islamic_bg_arabesque
            "hisn", "hisn_chapter", "custom_azkar", "audio_azkar" -> R.drawable.img_islamic_bg_mosque
            "tasbeeh", "wird", "spiritual_companion" -> R.drawable.img_islamic_bg_lantern
            "hijri", "reminders", "age_calculator" -> R.drawable.img_islamic_bg_night
            "prayer_times" -> R.drawable.img_islamic_bg_mosque
            "qibla" -> R.drawable.img_islamic_bg_courtyard
            "adiya", "ruqyah", "prophet_duas", "names_of_allah", "path_to_success" -> R.drawable.img_islamic_bg_courtyard
            "advanced_settings" -> R.drawable.img_islamic_bg_arabesque
            "ai_robot", "exclusive" -> R.drawable.img_islamic_bg_night
            "favorites", "search" -> R.drawable.img_islamic_bg_courtyard
            "about", "support_developer", "dev_link" -> R.drawable.img_islamic_bg_mosque
            else -> R.drawable.img_islamic_bg_courtyard
        }
    }

    @DrawableRes
    fun effectiveBuiltInDrawableForPage(context: Context, pageKey: String?): Int? {
        if (pageKey == null || pageKey == "home") return null
        val overrideId = prefs(context).getString(KEY_BUILTIN_PREFIX + pageKey, null)
        if (overrideId != null) {
            builtInBackgrounds.firstOrNull { it.id == overrideId }?.let { return it.drawableRes }
        }
        return defaultDrawableForPage(pageKey)
    }

    /**
     * هل ينبغي تفعيل طبقة الخلفية الشفافة لهذه الصفحة حاليًا؟
     * الأولوية: صورة المعرض المخصصة -> لون الصفحة الصلب المخصص -> الخلفية الإسلامية المدمجة.
     */
    fun shouldRenderBackgroundLayer(
        context: Context,
        pageKey: String?,
        hasCustomPageColor: Boolean
    ): Boolean {
        if (pageKey == null || pageKey == "home") return false
        if (isEnabled(context, pageKey)) return true
        if (hasCustomPageColor) return false
        return isIslamicBackgroundsEnabled(context) && effectiveBuiltInDrawableForPage(context, pageKey) != null
    }

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
        prefs(context).edit()
            .putStringSet(KEY_ENABLED_PAGES, current)
            .remove(KEY_BUILTIN_PREFIX + pageKey)
            .apply()
        revision++
    }

    fun clearAll(context: Context) {
        val editor = prefs(context).edit().remove(KEY_ENABLED_PAGES)
        pages.forEach { page ->
            runCatching { file(context, page.key).delete() }
            editor.remove(KEY_BUILTIN_PREFIX + page.key)
        }
        editor.apply()
        revision++
    }
}
