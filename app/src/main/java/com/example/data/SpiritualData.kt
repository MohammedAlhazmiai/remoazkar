package com.example.data

data class MoodRecommendation(
    val mood: String,
    val emoji: String,
    val title: String,
    val description: String,
    val recommendedZikr: String
)

object SpiritualRepository {
    val moods = listOf(
        MoodRecommendation("قلق أو توتر", "🌿", "سكينة وطمأنينة", "أذكار تبدد القلق وتشرح الصدر", "ألا بذكر الله تطمئن القلوب"),
        MoodRecommendation("شكر وامتنان", "✨", "زيادة النعم", "أذكار الحمد والشكر لله تعالى", "لئن شكرتم لأزيدنكم - الحمد لله رب العالمين"),
        MoodRecommendation("حاجة أو ضيق", "🤲", "تفريج الكرب", "أدعية الكرب والدعاء المستجاب", "لا إله إلا أنت سبحانك إني كنت من الظالمين"),
        MoodRecommendation("استغفار وتوبة", "🤍", "مغفرة الذنوب", "سيد الاستغفار وأذكار التوبة", "أستغفر الله العظيم الذي لا إله إلا هو الحي القيوم وأتوب إليه"),
        MoodRecommendation("طلب الرعاية والحفظ", "🛡️", "حصن حصين", "أذكار الحفظ والتحصين الشامل", "أعوذ بكلمات الله التامات من شر ما خلق")
    )
}
