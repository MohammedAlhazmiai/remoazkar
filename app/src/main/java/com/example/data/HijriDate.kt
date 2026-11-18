package com.example.data

import java.util.Calendar

/**
 * تحويل التاريخ الميلادي إلى الهجري (خوارزمية الكويت) مع المناسبات الإسلامية.
 */
data class HijriDate(val year: Int, val month: Int, val day: Int) {
    val monthName: String get() = HijriCalendar.HIJRI_MONTHS[(month - 1).coerceIn(0, 11)]
    val formatted: String get() = "$day $monthName $year هـ"
}

object HijriCalendar {

    val HIJRI_MONTHS = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    fun fromGregorian(year: Int, month: Int, day: Int): HijriDate {
        val jd = gregorianToJD(year, month, day)
        var l = jd - 1948440 + 10632
        val n = (l - 1) / 10631
        l = l - 10631 * n + 354
        val j = ((10985 - l) / 5316) * ((50 * l) / 17719) + (l / 5670) * ((43 * l) / 15238)
        l = l - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
        val hMonth = (24 * l) / 709
        val hDay = l - (709 * hMonth) / 24
        val hYear = 30 * n + j - 30
        return HijriDate(hYear, hMonth.coerceIn(1, 12), hDay.coerceIn(1, 30))
    }

    fun today(): HijriDate {
        val c = Calendar.getInstance()
        return fromGregorian(
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun gregorianToJD(year: Int, month: Int, day: Int): Int {
        var y = year
        var m = month
        if (m < 3) {
            y -= 1
            m += 12
        }
        val a = y / 100
        val b = 2 - a + a / 4
        return (365.25 * (y + 4716)).toInt() +
            (30.6001 * (m + 1)).toInt() + day + b - 1524
    }

    data class Occasion(val title: String, val description: String, val daysUntil: Int)

    /** المناسبات الإسلامية القادمة */
    fun upcomingOccasions(from: HijriDate = today()): List<Occasion> {
        val all = mutableListOf<Occasion>()
        val defs = listOf(
            Triple(1, 1, "رأس السنة الهجرية" to "بداية عام هجري جديد، وقت للمحاسبة والتجديد."),
            Triple(1, 10, "يوم عاشوراء" to "صيام يوم عاشوراء يكفّر سنة ماضية."),
            Triple(3, 12, "المولد النبوي" to "ذكرى مولد النبي ﷺ، وقت للصلاة عليه."),
            Triple(7, 27, "الإسراء والمعراج" to "ذكرى رحلة الإسراء والمعراج."),
            Triple(8, 15, "ليلة النصف من شعبان" to "ليلة مباركة، يُستحب فيها الدعاء والقيام."),
            Triple(9, 1, "بداية شهر رمضان" to "شهر القرآن والصيام والقيام."),
            Triple(9, 27, "ليلة القدر (المرجّحة)" to "ليلة خير من ألف شهر."),
            Triple(10, 1, "عيد الفطر المبارك" to "تكبيرات العيد وزكاة الفطر."),
            Triple(12, 9, "يوم عرفة" to "أفضل أيام السنة، صيامه يكفّر سنتين."),
            Triple(12, 10, "عيد الأضحى المبارك" to "يوم الحج الأكبر والأضاحي."),
            Triple(12, 1, "عشر ذي الحجة" to "أفضل عشر ليالٍ في السنة."),
            Triple(6, 1, "غرة شهر الله المحرم" to "أول الشهور الحرم.")
        )
        for ((m, d, info) in defs) {
            var days = hijriDayDiff(from, HijriDate(from.year, m, d))
            if (days < 0) days = hijriDayDiff(from, HijriDate(from.year + 1, m, d))
            all.add(Occasion(info.first, info.second, days))
        }
        return all.sortedBy { it.daysUntil }
    }

    private fun hijriDayDiff(a: HijriDate, b: HijriDate): Int {
        val da = a.year * 354 + a.month * 29 + a.day
        val db = b.year * 354 + b.month * 29 + b.day
        return db - da
    }
}
