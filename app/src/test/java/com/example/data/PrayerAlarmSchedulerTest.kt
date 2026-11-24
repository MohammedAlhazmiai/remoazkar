package com.example.data

import com.example.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class PrayerAlarmSchedulerTest {

    @Test
    fun selectNextPrayerTriggerHandlesLeadMinutesBeforePrayer() {
        val today = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 24, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val tomorrow = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }

        val dhuhrToday = ResolvedPrayerTime(PrayerType.DHUHR, 12, 15, isManualOverride = false, enabled = true)
        val dhuhrTomorrow = ResolvedPrayerTime(PrayerType.DHUHR, 12, 16, isManualOverride = false, enabled = true)

        // At 12:00 with 10-minute lead, reminder fires today at 12:05
        val resToday = PrayerAlarmScheduler.selectNextPrayerTrigger(
            todayCal = today,
            todayItem = dhuhrToday,
            tomorrowCal = tomorrow,
            tomorrowItem = dhuhrTomorrow,
            nowMillis = today.timeInMillis,
            leadMillis = 10 * 60_000L
        )
        assertNotNull(resToday)
        val expectedToday = (today.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 5)
        }.timeInMillis
        assertEquals(expectedToday, resToday!!.second)

        // At 12:06 (after 12:05 reminder already passed), next reminder fires tomorrow at 12:06 (12:16 - 10m)
        val nowAfterLead = today.timeInMillis + 6 * 60_000L
        val resTomorrow = PrayerAlarmScheduler.selectNextPrayerTrigger(
            todayCal = today,
            todayItem = dhuhrToday,
            tomorrowCal = tomorrow,
            tomorrowItem = dhuhrTomorrow,
            nowMillis = nowAfterLead,
            leadMillis = 10 * 60_000L
        )
        assertNotNull(resTomorrow)
        val expectedTomorrow = (tomorrow.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 6)
        }.timeInMillis
        assertEquals(expectedTomorrow, resTomorrow!!.second)
    }

    @Test
    fun adhanStartRawResourceIsPackaged() {
        assertTrue(R.raw.adhan_start != 0)
    }
}
