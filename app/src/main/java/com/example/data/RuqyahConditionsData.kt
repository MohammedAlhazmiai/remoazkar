package com.example.data

/**
 * الرقية الشرعية حسب الحالة — يوجّه المستخدم إلى النصوص الأنسب لحالته
 * مع إرشادات مختصرة، معتمدة على نصوص الرقية الثابتة في [RuqyahData].
 */
data class RuqyahCondition(
    val id: Int,
    val title: String,
    val description: String,
    val itemIds: List<Int>,
    val advice: String
)

object RuqyahConditionsData {

    val conditions: List<RuqyahCondition> = listOf(
        RuqyahCondition(
            id = 1,
            title = "العين والحسد",
            description = "أعراض يشعر بها من أصابته عين أو حسد: ثقل مفاجئ، كسل، صداع متكرر، ضيق بلا سبب، ويُستحب المداومة على الرقية والاغتسال.",
            itemIds = listOf(1, 2, 3, 4, 5, 19, 20, 21),
            advice = "اقرأ على نفسك صباحًا ومساءً، واغتسل بماء الرقية إن تيسّر، وأكثر من قول: «حسبي الله لا إله إلا هو عليه توكلت وهو رب العرش العظيم»."
        ),
        RuqyahCondition(
            id = 2,
            title = "السحر والمس",
            description = "من أصابه سحر أو مسّ يشعر بضيق شديد، خوف، أحلام مزعجة، نفور من العبادة، وتقلّب في الحال.",
            itemIds = listOf(1, 2, 3, 4, 5, 6, 7, 22, 23),
            advice = "داوم على الرقية بنفسك أو بمن يثق بعلمه وأمانته، مع التوكل على الله والتوبة وترك المعاصي، ولا تلجأ إلى السحرة بحال."
        ),
        RuqyahCondition(
            id = 3,
            title = "الهم والغم والضيق",
            description = "ضيق الصدر، الحزن المستمر، كثرة التفكير، وقلة الطمأنينة.",
            itemIds = listOf(1, 8, 9, 24, 25),
            advice = "أكثر من الدعاء: «اللهم إني أعوذ بك من الهم والحزن...»، وداوم على الصلاة والذكر، وتوكّل على الله وأحسن الظن به."
        ),
        RuqyahCondition(
            id = 4,
            title = "المرض والشفاء",
            description = "الرقية سبب شرعي للشفاء مع الأخذ بالأسباب الطبية واليقين بأن الشافي هو الله.",
            itemIds = listOf(1, 2, 10, 11, 26),
            advice = "اقرأ على موضع الألم، واجمع بين الرقية والعلاج الطبي، وأكثر من الدعاء بيقين، وتصدّق ولو بالقليل."
        ),
        RuqyahCondition(
            id = 5,
            title = "الحمل والعقم",
            description = "من تأخر عنها الحمل أو تعاني مشكلات في الحمل، تُستحب الرقية والدعاء مع مراجعة الطبيب.",
            itemIds = listOf(1, 12, 13, 27),
            advice = "داوم على دعاء زكريا: «رب لا تذرني فردًا وأنت خير الوارثين»، وأكثر من الاستغفار والصدقة، ولا تيأس من رحمة الله."
        ),
        RuqyahCondition(
            id = 6,
            title = "الخوف والوسواس",
            description = "الخوف غير المبرر، الوسواس القهري، ووساوس الشيطان في العقيدة والعبادة.",
            itemIds = listOf(1, 14, 15, 28),
            advice = "استعذ بالله من الشيطان الرجيم، وأكثر من قراءة المعوذتين وآية الكرسي، ولا تسترسل مع الوسواس، واطلب العلم النافع."
        ),
        RuqyahCondition(
            id = 7,
            title = "الحفظ والحماية",
            description = "أذكار ورقية تُقرأ لحفظ النفس والأهل والمال من كل سوء بإذن الله.",
            itemIds = listOf(1, 16, 17, 18, 21),
            advice = "داوم على أذكار الصباح والمساء، وآية الكرسي قبل النوم، والمعوذتين، وقراءة سورة البقرة في البيت."
        )
    )

    fun byId(id: Int): RuqyahCondition? = conditions.firstOrNull { it.id == id }

    /** النص الكامل لحالة معيّنة لقراءته بالروبوت دفعة واحدة. */
    fun fullText(condition: RuqyahCondition): String {
        val texts = condition.itemIds.mapNotNull { RuqyahData.item(it) }
        return buildString {
            append(condition.title).append(". ")
            append(condition.description).append(" ")
            texts.forEach { item ->
                append(item.title).append(". ").append(item.text).append(". ")
                if (item.repeat > 1) append("تُقرأ ${item.repeat} مرات. ")
            }
            append(condition.advice)
        }
    }
}
