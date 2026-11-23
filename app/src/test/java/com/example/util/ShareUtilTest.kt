package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ShareUtilTest {

    @Test
    fun formatReadableTextStripsDoubleAsterisksAndNormalizesBlankLines() {
        val raw = "**سجود السهو وأحكامه**\n\n\n\n• **من القرآن الكريم:** ﴿...﴾\n\n  "
        val cleaned = ShareUtil.formatReadableText(raw)
        assertEquals("سجود السهو وأحكامه\n\n• من القرآن الكريم: ﴿...﴾", cleaned)
    }

    @Test
    fun formatReadableTextHandlesBlankInputSafely() {
        assertEquals("", ShareUtil.formatReadableText("   \n\t  "))
    }
}
