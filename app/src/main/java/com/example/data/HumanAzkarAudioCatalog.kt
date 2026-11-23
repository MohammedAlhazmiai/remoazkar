package com.example.data

import androidx.annotation.RawRes
import com.example.R

/** Human-recorded clips supplied for periodic azkar playback. */
data class HumanAzkarTrack(
    val index: Int,
    val resourceName: String,
    val title: String,
    @RawRes val rawResId: Int
)

object HumanAzkarAudioCatalog {
    private val baseTracks: List<HumanAzkarTrack> = listOf(
        HumanAzkarTrack(1, "human_azkar_01", "مقطع بشري 01", R.raw.human_azkar_01),
        HumanAzkarTrack(2, "human_azkar_02", "مقطع بشري 02", R.raw.human_azkar_02),
        HumanAzkarTrack(3, "human_azkar_03", "مقطع بشري 03", R.raw.human_azkar_03),
        HumanAzkarTrack(4, "human_azkar_04", "مقطع بشري 04", R.raw.human_azkar_04),
        HumanAzkarTrack(5, "human_azkar_05", "مقطع بشري 05", R.raw.human_azkar_05),
        HumanAzkarTrack(6, "human_azkar_06", "مقطع بشري 06", R.raw.human_azkar_06),
        HumanAzkarTrack(7, "human_azkar_07", "مقطع بشري 07", R.raw.human_azkar_07),
        HumanAzkarTrack(8, "human_azkar_08", "مقطع بشري 08", R.raw.human_azkar_08),
        HumanAzkarTrack(9, "human_azkar_09", "مقطع بشري 09", R.raw.human_azkar_09),
        HumanAzkarTrack(10, "human_azkar_10", "مقطع بشري 10", R.raw.human_azkar_10),
        HumanAzkarTrack(11, "human_azkar_11", "مقطع بشري 11", R.raw.human_azkar_11),
        HumanAzkarTrack(12, "human_azkar_12", "مقطع بشري 12", R.raw.human_azkar_12),
        HumanAzkarTrack(13, "human_azkar_13", "مقطع بشري 13", R.raw.human_azkar_13),
        HumanAzkarTrack(14, "human_azkar_14", "مقطع بشري 14", R.raw.human_azkar_14),
        HumanAzkarTrack(15, "human_azkar_15", "مقطع بشري 15", R.raw.human_azkar_15),
        HumanAzkarTrack(16, "human_azkar_16", "مقطع بشري 16", R.raw.human_azkar_16),
        HumanAzkarTrack(17, "human_azkar_17", "مقطع بشري 17", R.raw.human_azkar_17),
        HumanAzkarTrack(18, "human_azkar_18", "مقطع بشري 18", R.raw.human_azkar_18),
        HumanAzkarTrack(19, "human_azkar_19", "مقطع بشري 19", R.raw.human_azkar_19),
        HumanAzkarTrack(20, "human_azkar_20", "مقطع بشري 20", R.raw.human_azkar_20),
        HumanAzkarTrack(21, "human_azkar_21", "مقطع بشري 21", R.raw.human_azkar_21),
        HumanAzkarTrack(22, "human_azkar_22", "مقطع بشري 22", R.raw.human_azkar_22),
        HumanAzkarTrack(23, "human_azkar_23", "مقطع بشري 23", R.raw.human_azkar_23)
    )

    private val addedTracks: List<HumanAzkarTrack> = listOf(
        HumanAzkarTrack(24, "human_azkar_29", "صلِّ على محمد ﷺ", R.raw.human_azkar_29),
        HumanAzkarTrack(25, "human_azkar_30", "الله أكبر", R.raw.human_azkar_30),
        HumanAzkarTrack(26, "human_azkar_31", "أستغفر الله", R.raw.human_azkar_31),
        HumanAzkarTrack(27, "human_azkar_32", "اللهم صلِّ على محمد", R.raw.human_azkar_32),
        HumanAzkarTrack(28, "human_azkar_33", "الحمد لله", R.raw.human_azkar_33),
        HumanAzkarTrack(29, "human_azkar_34", "أستغفر الله وأتوب إليه", R.raw.human_azkar_34),
        HumanAzkarTrack(30, "human_azkar_35", "لا إله إلا الله", R.raw.human_azkar_35),
        HumanAzkarTrack(31, "human_azkar_36", "الحمد لله رب العالمين", R.raw.human_azkar_36),
        HumanAzkarTrack(32, "human_azkar_37", "اللهم صلِّ على محمد وعلى آله وصحبه وسلم", R.raw.human_azkar_37),
        HumanAzkarTrack(33, "human_azkar_38", "لا حول ولا قوة إلا بالله", R.raw.human_azkar_38),
        HumanAzkarTrack(34, "human_azkar_39", "صلِّ على خاتم المرسلين", R.raw.human_azkar_39),
        HumanAzkarTrack(35, "human_azkar_40", "صلِّ على خاتم الأنبياء", R.raw.human_azkar_40),
        HumanAzkarTrack(36, "human_azkar_41", "صلِّ على أشرف الخلق", R.raw.human_azkar_41),
        HumanAzkarTrack(37, "human_azkar_42", "صلِّ على الأنبياء والمرسلين", R.raw.human_azkar_42),
        HumanAzkarTrack(38, "human_azkar_43", "صلِّ على النبي", R.raw.human_azkar_43),
        HumanAzkarTrack(39, "human_azkar_44", "صلِّ على الحبيب", R.raw.human_azkar_44),
        HumanAzkarTrack(40, "human_azkar_45", "صلِّ على محمد بن عبد الله", R.raw.human_azkar_45),
        HumanAzkarTrack(41, "human_azkar_46", "صلِّ على النبيين", R.raw.human_azkar_46),
        HumanAzkarTrack(42, "human_azkar_47", "صلِّ على رسول الله", R.raw.human_azkar_47),
        HumanAzkarTrack(43, "human_azkar_48", "صلِّ على محمد", R.raw.human_azkar_48),
        HumanAzkarTrack(44, "human_azkar_49", "سبحان الله", R.raw.human_azkar_49),
        HumanAzkarTrack(45, "human_azkar_50", "صلِّ على سيدنا محمد", R.raw.human_azkar_50),
        HumanAzkarTrack(46, "human_azkar_51", "سبحان الله وبحمده", R.raw.human_azkar_51),
        HumanAzkarTrack(47, "human_azkar_52", "سبحان الله وبحمده، سبحان الله العظيم", R.raw.human_azkar_52)
    )

    val tracks: List<HumanAzkarTrack> = baseTracks + addedTracks

    fun trackAt(index: Int): HumanAzkarTrack = tracks[index.mod(tracks.size)]

    fun trackByResourceName(resourceName: String): HumanAzkarTrack? =
        tracks.firstOrNull { it.resourceName == resourceName }
}
