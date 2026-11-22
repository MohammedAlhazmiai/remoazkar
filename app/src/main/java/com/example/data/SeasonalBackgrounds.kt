package com.example.data

import androidx.annotation.DrawableRes
import com.example.R
import java.util.Calendar
import java.util.TimeZone

/**
 * خلفيات موسمية ذكية لشاشة التطبيق الرئيسية تتغيّر تلقائيًا حسب الشهر الهجري والمناسبة الدينية.
 */
data class SeasonalOccasionTheme(
    val id: String,
    val title: String,
    val subtitle: String,
    @DrawableRes val drawables: List<Int>
)

object SeasonalBackgrounds {

    private const val YEMEN_TZ_ID = "Asia/Aden"

    fun currentYemenHour(): Int =
        Calendar.getInstance(TimeZone.getTimeZone(YEMEN_TZ_ID)).get(Calendar.HOUR_OF_DAY)

    fun currentSeason(): SeasonalOccasionTheme? = resolve(
        hijriDate = HijriCalendar.today(),
        hourOfDay = currentYemenHour()
    )

    /**
     * يحدد الخلفية الموسمية المناسبة للتاريخ الهجري والساعة المحددين،
     * أو يعيد null في الأيام العادية ليعود التطبيق إلى خلفياته الإسلامية الافتراضية.
     */
    fun resolve(
        hijriDate: HijriDate,
        hourOfDay: Int = 12,
        ramadanLength: Int = HijriCalendar.hijriMonthLength(hijriDate.year, 9)
    ): SeasonalOccasionTheme? {
        val month = hijriDate.month
        val day = hijriDate.day
        val safeRamadanLen = ramadanLength.coerceIn(29, 30)

        return when (month) {
            // شهر شعبان:
            // أول الشهر (1..20): «اللهم بلغنا رمضان»
            // نهاية الشهر (21..آخر يوم): «مرحبًا بقدوم شهر رمضان»
            8 -> {
                if (day <= 20) {
                    SeasonalOccasionTheme(
                        id = "shaban_start",
                        title = "شهر شعبان المبارك",
                        subtitle = "اللهم بلغنا رمضان وبارك لنا في شعبان",
                        drawables = listOf(R.drawable.img_bg_shaban_start)
                    )
                } else {
                    SeasonalOccasionTheme(
                        id = "shaban_end",
                        title = "مرحبًا بقدوم شهر رمضان",
                        subtitle = "أيام قليلة تفصلنا عن الشهر الفضيل — استعدوا بالطاعات",
                        drawables = listOf(R.drawable.img_bg_shaban_end)
                    )
                }
            }

            // شهر رمضان:
            // أول الشهر (1..20): خلفيتا ترحيب وتهنئة بشهر رمضان
            // آخر عشر أيام (21..قبل آخر ليلة): العشر الأواخر وخواتم مباركة واللهم بلغنا ليلة القدر
            // آخر ليلة من رمضان (من غروب آخر يوم رمضان، الساعة >= 18): تهنئة عيد الفطر المبارك
            9 -> {
                when {
                    day >= safeRamadanLen && hourOfDay >= 18 -> eidAlFitrTheme()
                    day >= 21 -> SeasonalOccasionTheme(
                        id = "ramadan_last10",
                        title = "العشر الأواخر من رمضان",
                        subtitle = "خواتم مباركة • اللهم بلغنا ليلة القدر",
                        drawables = listOf(R.drawable.img_bg_ramadan_last10)
                    )
                    else -> SeasonalOccasionTheme(
                        id = "ramadan_welcome",
                        title = "شهر رمضان المبارك",
                        subtitle = "أهلًا بشهر القرآن والرحمة والمغفرة والعتق من النار",
                        drawables = listOf(
                            R.drawable.img_bg_ramadan_welcome_1,
                            R.drawable.img_bg_ramadan_welcome_2
                        )
                    )
                }
            }

            // شهر شوال:
            // من 1 إلى 10 شوال: تهنئة عيد الفطر المبارك، ومن 11 شوال يعود للخلفيات الافتراضية
            10 -> {
                if (day <= 10) eidAlFitrTheme() else null
            }

            // شهر ذو الحجة:
            // من 1 إلى 9 ذي الحجة: خلفيتان لفضائل عشر ذي الحجة ومناسبة الحج
            // من 10 إلى 20 ذي الحجة: تهنئة عيد الأضحى المبارك، ومن 21 يعود للخلفيات الافتراضية
            12 -> {
                when {
                    day in 1..9 -> SeasonalOccasionTheme(
                        id = "dhul_hijjah_ten",
                        title = "عشر ذي الحجة وموسم الحج",
                        subtitle = "أعظم أيام الدنيا — أكثروا من التكبير والتهليل والتحميد",
                        drawables = listOf(
                            R.drawable.img_bg_dhulhijjah_ten,
                            R.drawable.img_bg_hajj
                        )
                    )
                    day in 10..20 -> SeasonalOccasionTheme(
                        id = "eid_adha",
                        title = "عيد الأضحى المبارك",
                        subtitle = "عيد أضحى مبارك — تقبّل الله منا ومنكم صالح الأعمال",
                        drawables = listOf(R.drawable.img_bg_eid_adha)
                    )
                    else -> null
                }
            }

            else -> null
        }
    }

    private fun eidAlFitrTheme() = SeasonalOccasionTheme(
        id = "eid_fitr",
        title = "عيد الفطر المبارك",
        subtitle = "كل عام وأنتم بخير — تقبّل الله صيامكم وقيامكم",
        drawables = listOf(R.drawable.img_bg_eid_fitr)
    )
}
