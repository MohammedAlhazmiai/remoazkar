package com.example.data

/** Human-recorded clips supplied for periodic azkar playback. */
data class HumanAzkarTrack(
    val index: Int,
    val resourceName: String,
    val title: String
)

object HumanAzkarAudioCatalog {
    val tracks: List<HumanAzkarTrack> = (1..23).map { index ->
        val suffix = index.toString().padStart(2, '0')
        HumanAzkarTrack(
            index = index,
            resourceName = "human_azkar_$suffix",
            title = "مقطع بشري $suffix"
        )
    }

    fun trackAt(index: Int): HumanAzkarTrack = tracks[index.mod(tracks.size)]
}
