package com.example.data

import com.example.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SeasonalBackgroundsTest {

    @Test
    fun shaban_start_and_end_resolve_correctly() {
        val start1 = SeasonalBackgrounds.resolve(HijriDate(1447, 8, 1))
        val start20 = SeasonalBackgrounds.resolve(HijriDate(1447, 8, 20))
        val end21 = SeasonalBackgrounds.resolve(HijriDate(1447, 8, 21))
        val end29 = SeasonalBackgrounds.resolve(HijriDate(1447, 8, 29))

        assertNotNull(start1)
        assertEquals("shaban_start", start1?.id)
        assertEquals(listOf(R.drawable.img_bg_shaban_start), start1?.drawables)
        assertEquals("shaban_start", start20?.id)

        assertNotNull(end21)
        assertEquals("shaban_end", end21?.id)
        assertEquals(listOf(R.drawable.img_bg_shaban_end), end21?.drawables)
        assertEquals("shaban_end", end29?.id)
    }

    @Test
    fun ramadan_welcome_last10_and_last_night_eid_transition() {
        val ram1 = SeasonalBackgrounds.resolve(HijriDate(1447, 9, 1), hourOfDay = 10, ramadanLength = 30)
        val ram20 = SeasonalBackgrounds.resolve(HijriDate(1447, 9, 20), hourOfDay = 22, ramadanLength = 30)
        assertEquals("ramadan_welcome", ram1?.id)
        assertEquals(
            listOf(R.drawable.img_bg_ramadan_welcome_1, R.drawable.img_bg_ramadan_welcome_2),
            ram1?.drawables
        )
        assertEquals("ramadan_welcome", ram20?.id)

        val ram21 = SeasonalBackgrounds.resolve(HijriDate(1447, 9, 21), hourOfDay = 8, ramadanLength = 30)
        val ram29Morning = SeasonalBackgrounds.resolve(HijriDate(1447, 9, 29), hourOfDay = 11, ramadanLength = 29)
        assertEquals("ramadan_last10", ram21?.id)
        assertEquals(listOf(R.drawable.img_bg_ramadan_last10), ram21?.drawables)
        assertEquals("ramadan_last10", ram29Morning?.id)

        // آخر ليلة من رمضان (سواء كان رمضان 29 أو 30 يومًا) بعد دخول الليل (>= 18) تتحول لعيد الفطر
        val lastNight29 = SeasonalBackgrounds.resolve(HijriDate(1447, 9, 29), hourOfDay = 19, ramadanLength = 29)
        val lastNight30 = SeasonalBackgrounds.resolve(HijriDate(1447, 9, 30), hourOfDay = 18, ramadanLength = 30)
        assertEquals("eid_fitr", lastNight29?.id)
        assertEquals(listOf(R.drawable.img_bg_eid_fitr), lastNight29?.drawables)
        assertEquals("eid_fitr", lastNight30?.id)
    }

    @Test
    fun shawwal_eid_fitr_through_10_then_returns_to_default() {
        val shw1 = SeasonalBackgrounds.resolve(HijriDate(1447, 10, 1))
        val shw10 = SeasonalBackgrounds.resolve(HijriDate(1447, 10, 10))
        val shw11 = SeasonalBackgrounds.resolve(HijriDate(1447, 10, 11))

        assertEquals("eid_fitr", shw1?.id)
        assertEquals("eid_fitr", shw10?.id)
        assertNull(shw11)
    }

    @Test
    fun dhul_hijjah_ten_days_eid_adha_and_return_to_default() {
        val dh1 = SeasonalBackgrounds.resolve(HijriDate(1447, 12, 1))
        val dh9 = SeasonalBackgrounds.resolve(HijriDate(1447, 12, 9))
        assertEquals("dhul_hijjah_ten", dh1?.id)
        assertEquals(
            listOf(R.drawable.img_bg_dhulhijjah_ten, R.drawable.img_bg_hajj),
            dh1?.drawables
        )
        assertEquals("dhul_hijjah_ten", dh9?.id)

        val dh10 = SeasonalBackgrounds.resolve(HijriDate(1447, 12, 10))
        val dh20 = SeasonalBackgrounds.resolve(HijriDate(1447, 12, 20))
        val dh21 = SeasonalBackgrounds.resolve(HijriDate(1447, 12, 21))
        assertEquals("eid_adha", dh10?.id)
        assertEquals(listOf(R.drawable.img_bg_eid_adha), dh10?.drawables)
        assertEquals("eid_adha", dh20?.id)
        assertNull(dh21)
    }

    @Test
    fun regular_months_return_null_for_default_backgrounds() {
        assertNull(SeasonalBackgrounds.resolve(HijriDate(1447, 1, 1)))
        assertNull(SeasonalBackgrounds.resolve(HijriDate(1447, 6, 15)))
        assertNull(SeasonalBackgrounds.resolve(HijriDate(1447, 7, 27)))
        assertNull(SeasonalBackgrounds.resolve(HijriDate(1447, 11, 15)))
    }
}
