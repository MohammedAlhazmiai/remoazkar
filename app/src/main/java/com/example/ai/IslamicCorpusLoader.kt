package com.example.ai

import android.content.Context
import android.util.Log
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okio.buffer
import okio.source
import java.io.InputStream

data class CorpusEntry(
    val id: String,
    val domain: String,
    val title: String,
    val keywords: List<String>,
    val synonyms: List<String>,
    val content: String,
    val quranEvidence: String,
    val hadithEvidence: String,
    val scholarlyNote: String,
    val followUps: List<String>,
    val titleIndexed: IndexedText,
    val keywordsIndexed: IndexedText,
    val bodyIndexed: IndexedText,
    val denseEmbedding: FloatArray = FloatArray(0)
) {
    override fun equals(other: Any?): Boolean = (other is CorpusEntry) && id == other.id && domain == other.domain
    override fun hashCode(): Int = id.hashCode() * 31 + domain.hashCode()
}

@JsonClass(generateAdapter = true)
internal data class RawCorpusDocument(
    val entries: List<RawCorpusEntry> = emptyList()
)

@JsonClass(generateAdapter = true)
internal data class RawCorpusEntry(
    val id: String = "",
    val domain: String = "chat_qa",
    val title: String = "",
    val keywords: List<String> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val content: String = "",
    val quranEvidence: String = "",
    val hadithEvidence: String = "",
    val scholarlyNote: String = "",
    val followUps: List<String> = emptyList()
)

/** Loads and indexes the curated offline Islamic AI knowledge corpus assets once into memory. */
object IslamicCorpusLoader {
    private const val TAG = "IslamicCorpusLoader"
    private val ASSET_FILES = listOf("islamic_ai_corpus.json", "islamic_ai_corpus_extra.json")

    private val moshi: Moshi by lazy { Moshi.Builder().build() }
    private val adapter by lazy { moshi.adapter(RawCorpusDocument::class.java) }

    @Volatile
    private var cachedEntries: List<CorpusEntry>? = null

    fun ensureLoaded(context: Context): List<CorpusEntry> {
        cachedEntries?.let { return it }
        synchronized(this) {
            cachedEntries?.let { return it }
            val loaded = mutableListOf<CorpusEntry>()
            for (assetName in ASSET_FILES) {
                runCatching {
                    context.assets.open(assetName).use { stream ->
                        loaded += parseStream(stream)
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Failed loading $assetName", error)
                }
            }
            val result = loaded.toList()
            cachedEntries = result
            return result
        }
    }

    /** Exposed for unit tests to feed asset streams directly without an Android Context. */
    fun parseStreams(streams: List<InputStream>): List<CorpusEntry> =
        streams.flatMap { stream -> stream.use { parseStream(it) } }

    private fun parseStream(stream: InputStream): List<CorpusEntry> {
        val doc = stream.source().buffer().use { buffered ->
            adapter.fromJson(buffered)
        } ?: return emptyList()

        return doc.entries.mapIndexed { index, raw ->
            val cleanTitle = raw.title.trim()
            val cleanContent = raw.content.trim()
            val cleanQuran = raw.quranEvidence.trim()
            val cleanHadith = raw.hadithEvidence.trim()
            val cleanNote = raw.scholarlyNote.trim()
            val cleanKeywords = raw.keywords.map { it.trim() }.filter { it.isNotBlank() }
            val cleanSynonyms = raw.synonyms.map { it.trim() }.filter { it.isNotBlank() }
            val cleanFollowUps = raw.followUps.map { it.trim() }.filter { it.isNotBlank() }

            val combinedKeywords = (cleanKeywords + cleanSynonyms).joinToString(" ")
            val combinedBody = "$cleanContent $cleanQuran $cleanHadith $cleanNote"
            val semanticDocIndexed = ArabicNlpEngine.indexText("$cleanTitle $combinedKeywords $cleanContent")

            CorpusEntry(
                id = raw.id.ifBlank { "entry_$index" },
                domain = raw.domain.ifBlank { "chat_qa" },
                title = cleanTitle,
                keywords = cleanKeywords,
                synonyms = cleanSynonyms,
                content = cleanContent,
                quranEvidence = cleanQuran,
                hadithEvidence = cleanHadith,
                scholarlyNote = cleanNote,
                followUps = cleanFollowUps,
                titleIndexed = ArabicNlpEngine.indexText(cleanTitle),
                keywordsIndexed = ArabicNlpEngine.indexText(combinedKeywords),
                bodyIndexed = ArabicNlpEngine.indexText(combinedBody),
                denseEmbedding = NeuralArabicLanguageCore.encodeFromIndexed(
                    indexed = semanticDocIndexed,
                    titleBoostText = "$cleanTitle $combinedKeywords"
                )
            )
        }
    }
}
