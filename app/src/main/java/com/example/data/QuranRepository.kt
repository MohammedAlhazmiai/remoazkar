package com.example.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

data class SurahInfo(
    val number: Int,
    val name: String,
    val englishName: String,
    val revelationType: String,
    val ayahCount: Int,
    val pageStart: Int
)

data class Ayah(
    val surah: Int,
    val number: Int,
    val text: String,
    val juz: Int,
    val page: Int,
    val hizb: Int,
    val plain: String
) {
    val key: String get() = "$surah:$number"
}

/**
 * مستودع القرآن الكريم الكامل: 114 سورة و 6236 آية مخزّنة محليًا في assets/quran.json
 * مع بيانات الجزء والصفحة والحزب، وبحث نصّي كامل بدون إنترنت.
 */
object QuranRepository {

    private var loaded = false

    /** حالة قابلة للملاحظة من Compose حتى تُحدَّث الشاشات تلقائيًا بعد اكتمال التحميل. */
    var isLoaded by mutableStateOf(false)
        private set
    val surahs = ArrayList<SurahInfo>(114)
    val ayahs = ArrayList<Ayah>(6236)
    private val surahIndex = HashMap<Int, List<Ayah>>()

    private val DIACRITICS = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u08F0-\\u08FF\\u0640]")

    @Synchronized
    fun ensureLoaded(context: Context) {
        if (loaded) return
        try {
            val raw = context.assets.open("quran.json").bufferedReader().use { it.readText() }
            val root = JSONObject(raw)

            val sArr = root.getJSONArray("surahs")
            for (i in 0 until sArr.length()) {
                val o = sArr.getJSONObject(i)
                surahs.add(
                    SurahInfo(
                        number = o.getInt("number"),
                        name = o.getString("name"),
                        englishName = o.getString("englishName"),
                        revelationType = o.getString("revelationType"),
                        ayahCount = o.getInt("ayahCount"),
                        pageStart = o.getInt("pageStart")
                    )
                )
            }

            val aArr = root.getJSONArray("ayahs")
            val grouped = HashMap<Int, MutableList<Ayah>>()
            for (i in 0 until aArr.length()) {
                val o = aArr.getJSONObject(i)
                val s = o.getInt("s")
                val ayah = Ayah(
                    surah = s,
                    number = o.getInt("n"),
                    text = o.getString("t").replace("\uFEFF", "").trim(),
                    juz = o.getInt("j"),
                    page = o.getInt("p"),
                    hizb = o.getInt("h"),
                    plain = normalize(o.optString("x", o.getString("t")))
                )
                ayahs.add(ayah)
                grouped.getOrPut(s) { mutableListOf() }.add(ayah)
            }
            grouped.forEach { (k, v) -> surahIndex[k] = v }
            loaded = true
            isLoaded = true
        } catch (error: Exception) {
            // لا نترك قوائم جزئية ولا نسمح لخطأ قراءة أصل محلي بإنهاء العملية.
            surahs.clear()
            ayahs.clear()
            surahIndex.clear()
            loaded = true
            isLoaded = true
            Log.e("QuranRepository", "Unable to load bundled Quran data", error)
        }
    }

    fun normalize(input: String): String {
        var t = input.replace("\uFEFF", "")
        t = DIACRITICS.replace(t, "")
        t = t.replace('\u0671', '\u0627')
        t = t.replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
            .replace('ى', 'ي').replace('ة', 'ه')
        return t.replace(Regex("\\s+"), " ").trim()
    }

    fun surah(number: Int): SurahInfo? = surahs.firstOrNull { it.number == number }

    fun ayahsOf(surah: Int): List<Ayah> = surahIndex[surah] ?: emptyList()

    fun byPage(page: Int): List<Ayah> = ayahs.filter { it.page == page }

    fun byJuz(juz: Int): List<Ayah> = ayahs.filter { it.juz == juz }

    fun byHizb(hizb: Int): List<Ayah> = ayahs.filter { it.hizb == hizb }

    fun search(context: Context, query: String, limit: Int = 300): List<Ayah> {
        ensureLoaded(context)
        val q = normalize(query)
        if (q.length < 2) return emptyList()
        val out = ArrayList<Ayah>()
        for (a in ayahs) {
            if (a.plain.contains(q)) {
                out.add(a)
                if (out.size >= limit) break
            }
        }
        return out
    }

    fun pageCount(): Int = 604
    fun juzCount(): Int = 30
    fun hizbCount(): Int = 240

    /** أول صفحة في الجزء */
    fun pageOfJuz(juz: Int): Int = ayahs.firstOrNull { it.juz == juz }?.page ?: 1
    fun pageOfHizb(hizb: Int): Int = ayahs.firstOrNull { it.hizb == hizb }?.page ?: 1
}
