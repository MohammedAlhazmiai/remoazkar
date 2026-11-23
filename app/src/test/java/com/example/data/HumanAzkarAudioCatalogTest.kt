package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HumanAzkarAudioCatalogTest {
    @Test
    fun catalogIncludesAll47TracksWithValidUniqueResources() {
        val tracks = HumanAzkarAudioCatalog.tracks
        assertEquals(47, tracks.size)
        assertEquals((1..47).toList(), tracks.map { it.index })
        assertEquals(tracks.size, tracks.map { it.resourceName }.toSet().size)
        assertEquals(tracks.size, tracks.map { it.rawResId }.toSet().size)
        assertTrue(tracks.all { it.rawResId != 0 && it.title.isNotBlank() })
    }

    @Test
    fun trackLookupAndWrapAroundResolveNewAndExistingTracks() {
        assertEquals("human_azkar_01", HumanAzkarAudioCatalog.trackAt(47).resourceName)
        assertEquals("human_azkar_29", HumanAzkarAudioCatalog.trackAt(23).resourceName)
        val lastTrack = HumanAzkarAudioCatalog.trackByResourceName("human_azkar_52")
        assertNotNull(lastTrack)
        assertEquals(47, lastTrack?.index)
    }
}
