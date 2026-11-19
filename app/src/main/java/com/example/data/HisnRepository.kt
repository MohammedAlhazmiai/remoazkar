package com.example.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

data class HisnItem(
    val text: String,
    val footnote: String,
    val repeat: Int
)

data class HisnChapter(
    val id: Int,
    val title: String,
    val items: List<HisnItem>
) {
    val totalRepeats: Int get() = items.sumOf { it.repeat.coerceAtLeast(1) }
    val preview: String get() = items.firstOrNull()?.text?.take(140) ?: ""
}

/**
 * كامل حصن المسلم: 132 بابًا و291 ذكرًا مع التكرارات والحواشي.
 * يُقرأ من assets/hisn.json عند أول استخدام.
 */
object HisnRepository {

    private var loaded = false

    /** حالة قابلة للملاحظة من Compose حتى تُحدَّث الشاشات تلقائيًا بعد اكتمال التحميل. */
    var isLoaded by mutableStateOf(false)
        private set
    val chapters = ArrayList<HisnChapter>(140)
    private val index = HashMap<Int, HisnChapter>()

    private val DIACRITICS = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u08F0-\\u08FF\\u0640]")

    @Synchronized
    fun ensureLoaded(context: Context) {
        if (loaded) return
        try {
            val json = context.assets.open("hisn.json").bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            val arr = root.optJSONArray("chapters")
                ?: throw IllegalStateException("Missing 'chapters' in bundled hisn.json")
            chapters.clear()
            index.clear()
            for (i in 0 until arr.length()) {
                val ch = arr.getJSONObject(i)
                val itemsArr = ch.optJSONArray("items")
                val items = ArrayList<HisnItem>(itemsArr?.length() ?: 0)
                if (itemsArr != null) {
                    for (j in 0 until itemsArr.length()) {
                        val it0 = itemsArr.getJSONObject(j)
                        items.add(
                            HisnItem(
                                text = it0.optString("t"),
                                footnote = it0.optString("f"),
                                repeat = it0.optInt("r", 1).coerceAtLeast(1)
                            )
                        )
                    }
                }
                val chapter = HisnChapter(
                    id = ch.optInt("id", i + 1),
                    title = ch.optString("title"),
                    items = items
                )
                chapters.add(chapter)
                index[chapter.id] = chapter
            }
            loaded = true
            isLoaded = true
        } catch (error: Exception) {
            // إفراغ أي نتيجة جزئية والسماح ببقاء التطبيق قابلاً للفتح.
            chapters.clear()
            index.clear()
            loaded = true
            isLoaded = true
            Log.e("HisnRepository", "Unable to load bundled Hisn data", error)
        }
    }

    fun chapter(id: Int): HisnChapter? = index[id]

    fun chapterByTitle(title: String): HisnChapter? =
        chapters.firstOrNull { it.title.trim() == title.trim() }

    fun normalize(input: String): String {
        var s = input.replace(DIACRITICS, "")
        s = s.replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
        s = s.replace('ى', 'ي').replace('ة', 'ه')
        return s.replace(Regex("\\s+"), " ").trim()
    }

    fun search(context: Context, query: String, limit: Int = 200): List<Pair<HisnChapter, HisnItem>> {
        ensureLoaded(context)
        val q = normalize(query)
        if (q.isEmpty()) return emptyList()
        val out = ArrayList<Pair<HisnChapter, HisnItem>>()
        for (ch in chapters) {
            if (normalize(ch.title).contains(q)) {
                ch.items.forEach { out.add(ch to it) }
            } else {
                for (item in ch.items) {
                    if (normalize(item.text).contains(q)) out.add(ch to item)
                }
            }
            if (out.size >= limit) break
        }
        return out.take(limit)
    }

    /** الأبواب المميزة التي تظهر في الرئيسية */
    fun featuredIds(): List<Int> {
        val wanted = listOf(
            "أذكار الصباح والمساء",
            "أذكار النوم",
            "أذكار الاستيقاظ من النوم",
            "الأذكار بعد السلام من الصلاة",
            "دعاء صلاة الاستخارة",
            "دعاء الهم والحزن",
            "دعاء الكرب"
        )
        val out = ArrayList<Int>()
        for (w in wanted) chapterByTitle(w)?.let { out.add(it.id) }
        return out
    }

    fun totalItems(): Int = chapters.sumOf { it.items.size }
}
