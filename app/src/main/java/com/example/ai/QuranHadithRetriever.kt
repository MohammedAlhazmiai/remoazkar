package com.example.ai

import com.example.data.AdiyaData
import com.example.data.DuaItem
import com.example.data.HisnRepository
import com.example.data.QuranRepository

data class EvidenceCitation(
    val label: String,
    val text: String,
    val source: String
)

/**
 * Retrieves verbatim Quranic verses, Prophetic Hadiths, and Duas directly from the app's
 * bundled offline repositories (`QuranRepository`, `AdiyaData`, `HisnRepository`).
 */
object QuranHadithRetriever {

    fun findQuranCitations(
        query: String,
        limit: Int = 2,
        excludeSources: Set<String> = emptySet()
    ): List<EvidenceCitation> {
        if (QuranRepository.ayahs.isEmpty()) return emptyList()
        val indexed = ArabicNlpEngine.indexText(query)
        val stems = indexed.stems.filter { it.length >= 2 }
        if (stems.isEmpty()) return emptyList()

        val scored = ArrayList<Pair<Double, EvidenceCitation>>()
        for (ayah in QuranRepository.ayahs) {
            // Skip excessively short disconnected-letter opening ayahs unless explicitly queried
            if (ayah.plain.length < 12) continue
            val surahName = QuranRepository.surah(ayah.surah)?.name ?: "سورة ${ayah.surah}"
            val source = "$surahName: ${ayah.number}"
            if (source in excludeSources) continue

            val ayahWords = ayah.plain.split(' ')
            var hits = 0
            var exactTokenHits = 0
            for (word in ayahWords) {
                val wStem = ArabicNlpEngine.lightStem(word)
                if (wStem in stems) hits++
                if (word in indexed.tokens) exactTokenHits++
            }
            if (hits == 0 && exactTokenHits == 0) continue

            var score = hits * 3.0 + exactTokenHits * 2.5
            // Prefer concise, clear verses for citation readability
            if (ayah.text.length in 30..240) score += 2.0
            scored += score to EvidenceCitation(
                label = "القرآن الكريم",
                text = "﴿${ayah.text}﴾",
                source = source
            )
        }

        return scored
            .sortedByDescending { it.first }
            .distinctBy { it.second.source }
            .take(limit)
            .map { it.second }
    }

    fun findHadithAndDuaCitations(
        query: String,
        limit: Int = 2,
        excludeSources: Set<String> = emptySet()
    ): List<EvidenceCitation> {
        val indexed = ArabicNlpEngine.indexText(query)
        if (indexed.stems.isEmpty()) return emptyList()

        val scored = ArrayList<Pair<Double, EvidenceCitation>>()
        for (item: DuaItem in AdiyaData.items) {
            val source = if (item.source.isNotBlank()) "${item.title} (${item.source})" else item.title
            if (source in excludeSources || item.title in excludeSources) continue
            val titleIdx = ArabicNlpEngine.indexText(item.title)
            val bodyIdx = ArabicNlpEngine.indexText(item.text)
            val s = ArabicNlpEngine.scoreSemanticMatch(
                queryIndexed = indexed,
                titleIndexed = titleIdx,
                keywordsIndexed = titleIdx,
                bodyIndexed = bodyIdx
            )
            if (s >= 4.0) {
                scored += s to EvidenceCitation(
                    label = if (item.category == AdiyaData.CAT_ARBAEEN) "الحديث الشريف" else item.category,
                    text = item.text,
                    source = source
                )
            }
        }

        for (chapter in HisnRepository.chapters) {
            if (chapter.title in excludeSources) continue
            val titleIdx = ArabicNlpEngine.indexText(chapter.title)
            val firstText = chapter.items.firstOrNull()?.text.orEmpty()
            val bodyIdx = ArabicNlpEngine.indexText(firstText)
            val s = ArabicNlpEngine.scoreSemanticMatch(
                queryIndexed = indexed,
                titleIndexed = titleIdx,
                keywordsIndexed = titleIdx,
                bodyIndexed = bodyIdx
            )
            if (s >= 6.0 && firstText.isNotBlank()) {
                scored += s to EvidenceCitation(
                    label = "حصن المسلم",
                    text = firstText.take(280),
                    source = chapter.title
                )
            }
        }

        return scored
            .sortedByDescending { it.first }
            .distinctBy { it.second.source }
            .take(limit)
            .map { it.second }
    }
}
