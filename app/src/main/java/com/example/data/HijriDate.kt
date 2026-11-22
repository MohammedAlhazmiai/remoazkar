package com.example.data

import android.content.Context
import android.icu.util.IslamicCalendar
import android.icu.util.TimeZone as IcuTimeZone
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * تحويل التاريخ الميلادي إلى الهجري بتوقيت اليمن (Asia/Aden) وفق تقويم أم القرى
 * مع إمكانية التعديل اليدوي وحفظ فرق الأيام، والمناسبات الإسلامية.
 */
data class HijriDate(val year: Int, val month: Int, val day: Int) {
    val monthName: String get() = HijriCalendar.HIJRI_MONTHS[(month - 1).coerceIn(0, 11)]
    val formatted: String get() = "$day $monthName $year هـ"
}

object HijriCalendar {

    private const val YEMEN_TZ_ID = "Asia/Aden"
    private const val PREFS_NAME = "rim_hijri_prefs"
    private const val KEY_OFFSET_DAYS = "offset_days"
    private const val MIN_OFFSET = -3650
    private const val MAX_OFFSET = 3650

    /** فرق الأيام المعدّل يدويًا من قِبل المستخدم في صفحة التقويم. */
    var offsetDays by mutableIntStateOf(0)
        private set

    private var offsetInitialized = false

    val HIJRI_MONTHS = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    fun initOffset(context: Context) {
        if (offsetInitialized) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        offsetDays = prefs.getInt(KEY_OFFSET_DAYS, 0).coerceIn(MIN_OFFSET, MAX_OFFSET)
        offsetInitialized = true
    }

    fun setOffsetDays(context: Context, days: Int) {
        val safeDays = days.coerceIn(MIN_OFFSET, MAX_OFFSET)
        offsetDays = safeDays
        offsetInitialized = true
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_OFFSET_DAYS, safeDays)
            .apply()
    }

    fun fromGregorian(year: Int, month: Int, day: Int): HijriDate {
        val icuResult = runCatching {
            val gCal = GregorianCalendar(TimeZone.getTimeZone(YEMEN_TZ_ID)).apply {
                clear()
                set(year, month - 1, day, 12, 0, 0)
            }
            val iCal = IslamicCalendar(IcuTimeZone.getTimeZone(YEMEN_TZ_ID)).apply {
                calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
                timeInMillis = gCal.timeInMillis
            }
            HijriDate(
                year = iCal.get(android.icu.util.Calendar.YEAR),
                month = (iCal.get(android.icu.util.Calendar.MONTH) + 1).coerceIn(1, 12),
                day = iCal.get(android.icu.util.Calendar.DAY_OF_MONTH).coerceIn(1, 30)
            )
        }.getOrNull()
        if (icuResult != null) return icuResult

        return fallbackFromGregorian(year, month, day)
    }

    /** اليوم الهجري الحالي بتوقيت اليمن مع احتساب التعديل اليدوي المحفوظ. */
    fun today(): HijriDate = todayWithOffset(offsetDays)

    /** اليوم الهجري التلقائي بتوقيت اليمن بدون أي تعديل يدوي. */
    fun baseToday(): HijriDate = todayWithOffset(0)

    private fun todayWithOffset(extraDays: Int): HijriDate {
        val g = todayGregorianYemen(extraDays)
        return fromGregorian(g.year, g.month, g.day)
    }

    fun todayGregorianYemen(extraDays: Int = 0): GregorianDate {
        val c = Calendar.getInstance(TimeZone.getTimeZone(YEMEN_TZ_ID))
        if (extraDays != 0) {
            c.add(Calendar.DAY_OF_MONTH, extraDays)
        }
        return GregorianDate(
            year = c.get(Calendar.YEAR),
            month = c.get(Calendar.MONTH) + 1,
            day = c.get(Calendar.DAY_OF_MONTH)
        )
    }

    /** ضبط التاريخ الهجري ليوافق تاريخًا محددًا أدخله المستخدم وحفظ الفرق تلقائيًا. */
    fun setCustomToday(context: Context, hYear: Int, hMonth: Int, hDay: Int): Boolean {
        if (hMonth !in 1..12 || hDay !in 1..30 || hYear !in 1300..1600) return false
        val targetJD = hijriToJD(hYear, hMonth, 1) + (hDay - 1)
        val gToday = todayGregorianYemen(0)
        val todayJD = gregorianToJD(gToday.year, gToday.month, gToday.day)
        val diff = targetJD - todayJD
        if (diff !in MIN_OFFSET..MAX_OFFSET) return false
        setOffsetDays(context, diff)
        return true
    }

    private fun fallbackFromGregorian(year: Int, month: Int, day: Int): HijriDate {
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

    /** تاريخ ميلادي. */
    data class GregorianDate(val year: Int, val month: Int, val day: Int) {
        val formatted: String get() = "$day / $month / $year م"
    }

    /**
     * تحويل تاريخ هجري إلى تاريخ ميلادي بتوقيت اليمن وفق تقويم أم القرى.
     * يعيد null إذا كان التاريخ غير صالح.
     */
    fun toGregorian(hYear: Int, hMonth: Int, hDay: Int): GregorianDate? {
        if (hMonth !in 1..12 || hDay !in 1..30 || hYear !in 1300..1600) return null
        val icuGregorian = runCatching {
            val iCal = IslamicCalendar(IcuTimeZone.getTimeZone(YEMEN_TZ_ID)).apply {
                calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
                clear()
                set(hYear, hMonth - 1, 1, 12, 0, 0)
            }
            val gCal = GregorianCalendar(TimeZone.getTimeZone(YEMEN_TZ_ID)).apply {
                timeInMillis = iCal.timeInMillis
                add(Calendar.DAY_OF_MONTH, hDay - 1)
            }
            val y = gCal.get(Calendar.YEAR)
            val m = gCal.get(Calendar.MONTH) + 1
            val d = gCal.get(Calendar.DAY_OF_MONTH)
            if (y in 1900..2200 && m in 1..12 && d in 1..31) GregorianDate(y, m, d) else null
        }.getOrNull()
        if (icuGregorian != null) return icuGregorian

        val jd = tabularHijriToJD(hYear, hMonth, hDay)
        val (y, m, d) = jdToGregorian(jd)
        if (y !in 1900..2200 || m !in 1..12 || d !in 1..31) return null
        return GregorianDate(y, m, d)
    }

    /** اليوم اليولياني لتاريخ هجري وفق تقويم أم القرى مع مسار احتياطي حسابي. */
    fun hijriToJD(year: Int, month: Int, day: Int): Int {
        val g = if (year in 1300..1600 && month in 1..12 && day in 1..30) {
            runCatching {
                val iCal = IslamicCalendar(IcuTimeZone.getTimeZone(YEMEN_TZ_ID)).apply {
                    calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
                    clear()
                    set(year, month - 1, 1, 12, 0, 0)
                }
                val gCal = GregorianCalendar(TimeZone.getTimeZone(YEMEN_TZ_ID)).apply {
                    timeInMillis = iCal.timeInMillis
                    add(Calendar.DAY_OF_MONTH, day - 1)
                }
                gregorianToJD(
                    gCal.get(Calendar.YEAR),
                    gCal.get(Calendar.MONTH) + 1,
                    gCal.get(Calendar.DAY_OF_MONTH)
                )
            }.getOrNull()
        } else null
        return g ?: tabularHijriToJD(year, month, day)
    }

    private fun tabularHijriToJD(year: Int, month: Int, day: Int): Int =
        (11 * year + 3) / 30 + 354 * year + 30 * month - (month - 1) / 2 + day + 1948440 - 385

    /** تحويل اليوم اليولياني إلى تاريخ ميلادي. */
    fun jdToGregorian(jd: Int): Triple<Int, Int, Int> {
        var l = jd + 68569
        val n = (4 * l) / 146097
        l -= (146097 * n + 3) / 4
        val i = (4000 * (l + 1)) / 1461001
        l -= (1461 * i) / 4 - 31
        val j = (80 * l) / 2447
        val day = l - (2447 * j) / 80
        l = j / 11
        val month = j + 2 - 12 * l
        val year = 100 * (n - 49) + i + l
        return Triple(year, month, day)
    }

    /** عدد أيام الشهر الهجري وفق تقويم أم القرى. */
    fun hijriMonthLength(year: Int, month: Int): Int {
        val icuLength = runCatching {
            val iCal = IslamicCalendar(IcuTimeZone.getTimeZone(YEMEN_TZ_ID)).apply {
                calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
                clear()
                set(year, month - 1, 1, 12, 0, 0)
            }
            iCal.getActualMaximum(android.icu.util.Calendar.DAY_OF_MONTH)
        }.getOrNull()
        if (icuLength != null && icuLength in 29..30) return icuLength

        val start = tabularHijriToJD(year, month, 1)
        val next = if (month >= 12) tabularHijriToJD(year + 1, 1, 1) else tabularHijriToJD(year, month + 1, 1)
        return next - start
    }

    /** اليوم اليولياني لتاريخ ميلادي (عام). */
    fun gregorianToJD(year: Int, month: Int, day: Int): Int {
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
            Triple(6, 1, "غرة شهر جمادى الآخرة" to "بداية شهر جمادى الآخرة.")
        )
        for ((m, d, info) in defs) {
            var days = hijriDayDiff(from, HijriDate(from.year, m, d))
            if (days < 0) days = hijriDayDiff(from, HijriDate(from.year + 1, m, d))
            all.add(Occasion(info.first, info.second, days))
        }
        return all.sortedBy { it.daysUntil }
    }

    private fun hijriDayDiff(a: HijriDate, b: HijriDate): Int =
        hijriToJD(b.year, b.month, b.day) - hijriToJD(a.year, a.month, a.day)
}
