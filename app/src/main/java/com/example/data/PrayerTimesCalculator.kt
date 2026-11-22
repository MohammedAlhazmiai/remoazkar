package com.example.data

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

enum class PrayerType(
    val id: String,
    val arabicName: String,
    val notificationTitle: String,
    val notificationBody: String,
    val requestCode: Int
) {
    FAJR(
        id = "fajr",
        arabicName = "الفجر",
        notificationTitle = "صلاة الفجر",
        notificationBody = "حيّ على الفلاح — أقم صلاة الفجر، الصلاة خير من النوم.",
        requestCode = 5101
    ),
    DHUHR(
        id = "dhuhr",
        arabicName = "الظهر",
        notificationTitle = "صلاة الظهر",
        notificationBody = "حيّ على الصلاة — حان الآن موعد صلاة الظهر.",
        requestCode = 5102
    ),
    ASR(
        id = "asr",
        arabicName = "العصر",
        notificationTitle = "صلاة العصر",
        notificationBody = "حافظوا على الصلوات والصلاة الوسطى — حان موعد صلاة العصر.",
        requestCode = 5103
    ),
    MAGHRIB(
        id = "maghrib",
        arabicName = "المغرب",
        notificationTitle = "صلاة المغرب",
        notificationBody = "اللهم إني أسألك بحق هذا الأذان — حان موعد صلاة المغرب.",
        requestCode = 5104
    ),
    ISHA(
        id = "isha",
        arabicName = "العشاء",
        notificationTitle = "صلاة العشاء",
        notificationBody = "حيّ على الفلاح — حان الآن موعد صلاة العشاء.",
        requestCode = 5105
    )
}

enum class PrayerCalculationMethod(
    val id: String,
    val arabicTitle: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaMinutesAfterMaghrib: Int? = null
) {
    UMM_AL_QURA("umm_al_qura", "تقويم أم القرى (اليمن والخليج)", 18.5, 0.0, 90),
    MWL("mwl", "رابطة العالم الإسلامي", 18.0, 17.0, null),
    EGYPTIAN("egyptian", "الهيئة المصرية العامة للمساحة", 19.5, 17.5, null),
    KARACHI("karachi", "جامعة العلوم الإسلامية بكراتشي", 18.0, 18.0, null),
    ISNA("isna", "الجمعية الإسلامية لأمريكا الشمالية", 15.0, 15.0, null);

    companion object {
        fun fromId(id: String?): PrayerCalculationMethod =
            entries.firstOrNull { it.id == id } ?: UMM_AL_QURA
    }
}

enum class AsrMadhab(
    val id: String,
    val arabicTitle: String,
    val shadowFactor: Double
) {
    SHAFI("shafi", "الجمهور (الشافعي والمالكي والحنبلي)", 1.0),
    HANAFI("hanafi", "المذهب الحنفي", 2.0);

    companion object {
        fun fromId(id: String?): AsrMadhab =
            entries.firstOrNull { it.id == id } ?: SHAFI
    }
}

data class CalculatedDayPrayers(
    val sunriseHour: Int,
    val sunriseMinute: Int,
    val times: Map<PrayerType, Pair<Int, Int>>
)

/**
 * محرّك حساب مواقيت الصلوات الخمس واتجاه القبلة فلكيًا بدون إنترنت.
 * يعتمد معادلات موقع الشمس الفلكية (Julian Day / Declination / Equation of Time)
 * ومعادلة الدائرة العظمى لاتجاه الكعبة المشرفة.
 */
object PrayerTimesCalculator {

    private const val KAABA_LAT = 21.422487
    private const val KAABA_LON = 39.826206

    fun calculateDay(
        date: Calendar,
        latitude: Double,
        longitude: Double,
        method: PrayerCalculationMethod,
        madhab: AsrMadhab
    ): CalculatedDayPrayers {
        val year = date.get(Calendar.YEAR)
        val month = date.get(Calendar.MONTH) + 1
        val day = date.get(Calendar.DAY_OF_MONTH)
        val tzOffsetHours = date.timeZone.getOffset(date.timeInMillis) / 3600000.0

        val jd = julianDate(year, month, day) - longitude / (15.0 * 24.0)

        var fajr = 5.0
        var sunrise = 6.0
        var dhuhr = 12.0
        var asr = 15.5
        var maghrib = 18.0
        var isha = 19.5

        // تكراران لتحسين دقة زاوية الشمس عند كل وقت صلاة
        for (iteration in 0..1) {
            val fajrDay = if (iteration == 0) 5.0 / 24.0 else fajr / 24.0
            val sunriseDay = if (iteration == 0) 6.0 / 24.0 else sunrise / 24.0
            val dhuhrDay = if (iteration == 0) 12.0 / 24.0 else dhuhr / 24.0
            val asrDay = if (iteration == 0) 15.0 / 24.0 else asr / 24.0
            val sunsetDay = if (iteration == 0) 18.0 / 24.0 else maghrib / 24.0
            val ishaDay = if (iteration == 0) 19.5 / 24.0 else isha / 24.0

            dhuhr = computeMidDay(jd + dhuhrDay)
            sunrise = computeSunAngleTime(jd + sunriseDay, latitude, 0.833, ccw = true)
            maghrib = computeSunAngleTime(jd + sunsetDay, latitude, 0.833, ccw = false)
            fajr = computeSunAngleTime(jd + fajrDay, latitude, method.fajrAngle, ccw = true)
            asr = computeAsrTime(jd + asrDay, latitude, madhab.shadowFactor)
            isha = if (method.ishaMinutesAfterMaghrib != null) {
                maghrib + method.ishaMinutesAfterMaghrib / 60.0
            } else {
                computeSunAngleTime(jd + ishaDay, latitude, method.ishaAngle, ccw = false)
            }
        }

        val shift = tzOffsetHours - longitude / 15.0
        val fajrLocal = normalizeHour(fajr + shift)
        val sunriseLocal = normalizeHour(sunrise + shift)
        val dhuhrLocal = normalizeHour(dhuhr + shift)
        val asrLocal = normalizeHour(asr + shift)
        val maghribLocal = normalizeHour(maghrib + shift)
        val ishaLocal = normalizeHour(
            if (method.ishaMinutesAfterMaghrib != null) {
                maghribLocal + method.ishaMinutesAfterMaghrib / 60.0
            } else {
                isha + shift
            }
        )

        val sr = toHourMinute(sunriseLocal)
        return CalculatedDayPrayers(
            sunriseHour = sr.first,
            sunriseMinute = sr.second,
            times = mapOf(
                PrayerType.FAJR to toHourMinute(fajrLocal),
                PrayerType.DHUHR to toHourMinute(dhuhrLocal),
                PrayerType.ASR to toHourMinute(asrLocal),
                PrayerType.MAGHRIB to toHourMinute(maghribLocal),
                PrayerType.ISHA to toHourMinute(ishaLocal)
            )
        )
    }

    /** اتجاه القبلة بالدرجات (0..360) من الشمال الجغرافي الحقيقي باتجاه عقارب الساعة. */
    fun computeQiblaBearing(latitude: Double, longitude: Double): Double {
        val lat1 = Math.toRadians(latitude)
        val lon1 = Math.toRadians(longitude)
        val lat2 = Math.toRadians(KAABA_LAT)
        val lon2 = Math.toRadians(KAABA_LON)
        val dLon = lon2 - lon1
        val y = sin(dLon)
        val x = cos(lat1) * tan(lat2) - sin(lat1) * cos(dLon)
        val deg = Math.toDegrees(atan2(y, x))
        return (deg % 360.0 + 360.0) % 360.0
    }

    /** المسافة التقريبية إلى الكعبة المشرفة بالكيلومتر (صيغة هافرسين). */
    fun computeDistanceToKaabaKm(latitude: Double, longitude: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(KAABA_LAT - latitude)
        val dLon = Math.toRadians(KAABA_LON - longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(KAABA_LAT)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sinDeg(g) + 0.020 * sinDeg(2 * g))
        val e = 23.439 - 0.00000036 * d

        val ra = atan2Deg(cosDeg(e) * sinDeg(l), cosDeg(l)) / 15.0
        val eqt = q / 15.0 - fixHour(ra)
        val decl = asinDeg(sinDeg(e) * sinDeg(l))
        return decl to eqt
    }

    private fun computeMidDay(jd: Double): Double {
        val (_, eqt) = sunPosition(jd)
        return fixHour(12.0 - eqt)
    }

    private fun computeSunAngleTime(
        jd: Double,
        latitude: Double,
        angleDeg: Double,
        ccw: Boolean
    ): Double {
        val (decl, _) = sunPosition(jd)
        val noon = computeMidDay(jd)
        val numerator = -sinDeg(angleDeg) - sinDeg(decl) * sinDeg(latitude)
        val denominator = cosDeg(decl) * cosDeg(latitude)
        val ratio = (numerator / denominator).coerceIn(-0.9999, 0.9999)
        val t = acosDeg(ratio) / 15.0
        return noon + if (ccw) -t else t
    }

    private fun computeAsrTime(jd: Double, latitude: Double, shadowFactor: Double): Double {
        val (decl, _) = sunPosition(jd)
        val angle = -arccotDeg(shadowFactor + tanDeg(kotlin.math.abs(latitude - decl)))
        return computeSunAngleTime(jd, latitude, angle, ccw = false)
    }

    private fun toHourMinute(hourDecimal: Double): Pair<Int, Int> {
        val totalMinutes = (normalizeHour(hourDecimal) * 60.0).roundToInt().mod(24 * 60)
        return (totalMinutes / 60) to (totalMinutes % 60)
    }

    private fun normalizeHour(h: Double): Double {
        var out = h % 24.0
        if (out < 0) out += 24.0
        return out
    }

    private fun fixAngle(a: Double): Double {
        var res = a % 360.0
        if (res < 0) res += 360.0
        return res
    }

    private fun fixHour(a: Double): Double {
        var res = a % 24.0
        if (res < 0) res += 24.0
        return res
    }

    private fun sinDeg(d: Double) = sin(d * PI / 180.0)
    private fun cosDeg(d: Double) = cos(d * PI / 180.0)
    private fun tanDeg(d: Double) = tan(d * PI / 180.0)
    private fun asinDeg(x: Double) = asin(x.coerceIn(-1.0, 1.0)) * 180.0 / PI
    private fun acosDeg(x: Double) = acos(x.coerceIn(-1.0, 1.0)) * 180.0 / PI
    private fun atan2Deg(y: Double, x: Double) = atan2(y, x) * 180.0 / PI
    private fun arccotDeg(x: Double) = atan2(1.0, x) * 180.0 / PI
}
