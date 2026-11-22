package com.example.data

/**
 * تصنيف فقرات الرقية الشرعية داخل كل حالة من حالات قسم «حسب الحالة».
 */
enum class ConditionPassageKind(val titleAr: String) {
    QURAN("القرآن الكريم"),
    SUNNAH("الأدعية النبوية الصحيحة"),
    METHOD("الهدي والطريقة الشرعية")
}

/**
 * فقرة رقية كاملة بنصها المضبوط بالشكل ومصدرها وعدد التكرار.
 */
data class ConditionRuqyahPassage(
    val title: String,
    val text: String,
    val kind: ConditionPassageKind,
    val repeat: Int = 1,
    val source: String = ""
)

/**
 * الرقية الشرعية حسب الحالة — محتوى متكامل لكل حالة من الحالات السبع
 * يشمل الآيات القرآنية الكاملة، والأدعية النبوية الصحيحة، والطريقة الشرعية للعلاج.
 */
data class RuqyahCondition(
    val id: Int,
    val title: String,
    val description: String,
    val itemIds: List<Int>,
    val passages: List<ConditionRuqyahPassage>,
    val advice: String
)

internal fun ruqyahDataPassage(
    id: Int,
    kind: ConditionPassageKind,
    repeatOverride: Int? = null,
    titleOverride: String? = null
): ConditionRuqyahPassage {
    val item = RuqyahData.item(id)
        ?: error("Missing RuqyahData item id=$id")
    return ConditionRuqyahPassage(
        title = titleOverride ?: item.title,
        text = item.text,
        kind = kind,
        repeat = repeatOverride ?: item.repeat,
        source = item.source
    )
}

object RuqyahConditionsData {

    val conditions: List<RuqyahCondition> by lazy {
        ruqyahConditionsPart1 + ruqyahConditionsPart2
    }

    fun byId(id: Int): RuqyahCondition? = conditions.firstOrNull { it.id == id }

    fun quranPassages(condition: RuqyahCondition): List<ConditionRuqyahPassage> =
        condition.passages.filter { it.kind == ConditionPassageKind.QURAN }

    fun sunnahPassages(condition: RuqyahCondition): List<ConditionRuqyahPassage> =
        condition.passages.filter { it.kind == ConditionPassageKind.SUNNAH }

    fun methodPassages(condition: RuqyahCondition): List<ConditionRuqyahPassage> =
        condition.passages.filter { it.kind == ConditionPassageKind.METHOD }

    /** النص الكامل الشامل لحالة معيّنة لقراءته بالروبوت أو نسخه ومشاركته دفعة واحدة. */
    fun fullText(condition: RuqyahCondition): String = buildString {
        append(condition.title).append("\n\n")
        append(condition.description).append("\n\n")
        condition.passages.forEach { passage ->
            append("• ").append(passage.title)
            if (passage.repeat > 1) append(" (تُقرأ ${passage.repeat} مرات)")
            append(":\n")
            append(passage.text)
            if (passage.source.isNotBlank()) {
                append("\n[").append(passage.source).append("]")
            }
            append("\n\n")
        }
        append("خلاصة الإرشادات: ").append(condition.advice)
    }
}
