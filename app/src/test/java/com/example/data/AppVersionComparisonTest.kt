package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppVersionComparisonTest {
    @Test
    fun comparesNumericComponentsRatherThanText() {
        assertEquals(1, compareDottedVersions("1.10", "1.9"))
        assertEquals(-1, compareDottedVersions("1.6", "1.7"))
        assertEquals(0, compareDottedVersions("1.6.0", "v1.6"))
    }

    @Test
    fun rejectsMalformedVersions() {
        assertNull(compareDottedVersions("release-latest", "1.6"))
        assertNull(compareDottedVersions("1.7", "1.x"))
    }
}
