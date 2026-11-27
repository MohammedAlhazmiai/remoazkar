package com.example.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortTextAzkarCatalogTest {

    @Test
    fun shortTextAzkarCatalogIncludesAllRequestedDhikrPhrases() {
        val texts = ShortTextAzkarCatalog.items.map { it.text }
        val required = listOf(
            "سبحان الله",
            "الحمد لله",
            "الله أكبر",
            "لا إله إلا الله محمد رسول الله",
            "اذكروا الله",
            "صلّوا على رسول الله",
            "اللهم صلِّ على نبينا محمد",
            "أستغفر الله",
            "اللهم إنا نسألك الجنة ونعوذ بك من النار",
            "سبحان الله وبحمده، سبحان الله العظيم",
            "سبحان الله وبحمده",
            "سبحان الله العظيم"
        )
        required.forEach { expected ->
            assertTrue(
                "Expected ShortTextAzkarCatalog to include: $expected",
                texts.any { it.contains(expected) }
            )
        }
    }

    @Test
    fun shortTextAzkarCatalogNeverContainsMorningEveningChapterLabel() {
        val all = ShortTextAzkarCatalog.items.flatMap { listOf(it.title, it.text) }
        assertFalse(all.any { it.contains("أذكار الصباح والمساء") })
        assertFalse(all.any { it.contains("أذكار النوم") })
        assertTrue(NotificationHelper.NOTIFICATION_ID_HUMAN_TEXT_AZKAR != 7041)
    }
}
