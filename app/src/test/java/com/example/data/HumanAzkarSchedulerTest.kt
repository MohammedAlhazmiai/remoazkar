package com.example.data

import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HumanAzkarSchedulerTest {
    @Test
    fun exactAlarmAccessIsNotRequiredBeforeAndroid12() {
        assertTrue(
            HumanAzkarScheduler.exactAlarmAccessAllowsPlayback(
                Build.VERSION_CODES.S - 1,
                hasExactAlarmAccess = false
            )
        )
    }

    @Test
    fun exactAlarmAccessIsRequiredFromAndroid12Onward() {
        assertFalse(
            HumanAzkarScheduler.exactAlarmAccessAllowsPlayback(
                Build.VERSION_CODES.S,
                hasExactAlarmAccess = false
            )
        )
        assertTrue(
            HumanAzkarScheduler.exactAlarmAccessAllowsPlayback(
                Build.VERSION_CODES.S,
                hasExactAlarmAccess = true
            )
        )
    }
}
