package com.example.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [SurahEntity::class], version = 1, exportSchema = false)
abstract class QuranDatabase : RoomDatabase() {

    abstract fun quranDao(): QuranDao

    companion object {
        @Volatile
        private var INSTANCE: QuranDatabase? = null

        fun getDatabase(context: Context): QuranDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuranDatabase::class.java,
                    "quran_database"
                )
                    .addCallback(QuranDatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class QuranDatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database.quranDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: QuranDao) {
            val initialSurahs = listOf(
                SurahEntity(1, "الفاتحة", "Al-Fatihah", "مكية", 7, "https://server8.mp3quran.net/afs/001.mp3", "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\nالْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ\nالرَّحْمَٰنِ الرَّحِيمِ\nمَالِكِ يَوْمِ الدِّينِ\nإيَّاكَ نَعْبُدُ وَإيَّاكَ نَسْتَعِينُ\nاهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ\nصِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ", 1),
                SurahEntity(2, "البقرة", "Al-Baqarah", "مدنية", 286, "https://server8.mp3quran.net/afs/002.mp3", "الم\nذَلِكَ الْكِتَابُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًى لِلْمُتَّقِينَ\nالَّذِينَ يُؤْمِنُونَ بِالْغَيْبِ وَيُقِيمُونَ الصَّلَاةَ وَمِمَّا رَزَقْنَاهُمْ يُنْفِقُونَ\nوَالَّذِينَ يُؤْمِنُونَ بِمَا أُنْزِلَ إِلَيْكَ وَمَا أُنْزِلَ مِنْ قَبْلِكَ وَبِالْآخِرَةِ هُمْ يُوقِنُونَ", 2),
                SurahEntity(3, "آل عمران", "Ali 'Imran", "مدنية", 200, "https://server8.mp3quran.net/afs/003.mp3", "الم\nاللَّهُ لَا إِلَهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ\nنَزَّلَ عَلَيْكَ الْكِتَابَ بِالْحَقِّ مُصَدِّقًا لِمَا بَيْنَ يَدَيْهِ", 50),
                SurahEntity(18, "الكهف", "Al-Kahf", "مكية", 110, "https://server8.mp3quran.net/afs/018.mp3", "الْحَمْدُ لِلَّهِ الَّذِي أَنْزَلَ عَلَى عَبْدِهِ الْكِتَابَ وَلَمْ يَجْعَلْ لَهُ عِوَجًا\nقَيِّمًا لِيُنْذِرَ بَأْسًا شَدِيدًا مِنْ لَدُنْهُ وَيُبَشِّرَ الْمُؤْمِنِينَ", 293),
                SurahEntity(36, "يس", "Ya-Sin", "مكية", 83, "https://server8.mp3quran.net/afs/036.mp3", "يس\nوَالْقُرْآنِ الْحَكِيمِ\nإِنَّكَ لَمِنَ الْمُرْسَلِينَ\nعَلَى صِرَاطٍ مُسْتَقِيمٍ", 440),
                SurahEntity(55, "الرحمن", "Ar-Rahman", "مدنية", 78, "https://server8.mp3quran.net/afs/055.mp3", "الرَّحْمَٰنُ\nعَلَّمَ الْقُرْآنَ\nخَلَقَ الْإِنْسَانَ\nعَلَّمَهُ الْبَيَانَ", 531),
                SurahEntity(56, "الواقعة", "Al-Waqi'ah", "مكية", 96, "https://server8.mp3quran.net/afs/056.mp3", "إِذَا وَقَعَتِ الْوَاقِعَةُ\nلَيْسَ لِوَقْعَتِهَا كَاذِبَةٌ\nخَافِضَةٌ رَافِعَةٌ", 534),
                SurahEntity(67, "الملك", "Al-Mulk", "مكية", 30, "https://server8.mp3quran.net/afs/067.mp3", "تَبَارَكَ الَّذِي بِيَدِهِ الْمُلْكُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ\nالَّذِي خَلَقَ الْمَوْتَ وَالْحَيَاةَ لِيَبْلُوَكُمْ", 562),
                SurahEntity(112, "الإخلاص", "Al-Ikhlas", "مكية", 4, "https://server8.mp3quran.net/afs/112.mp3", "قُلْ هُوَ اللَّهُ أَحَدٌ\nاللَّهُ الصَّمَدُ\nلَمْ يَلِدْ وَلَمْ يُولَدْ\nوَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ", 604)
            )
            dao.insertAll(initialSurahs)
        }
    }
}
