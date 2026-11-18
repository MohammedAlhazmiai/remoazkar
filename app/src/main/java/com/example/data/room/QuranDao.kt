package com.example.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {
    @Query("SELECT * FROM surahs ORDER BY number ASC")
    fun getAllSurahs(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surahs WHERE name LIKE '%' || :query || '%' OR englishName LIKE '%' || :query || '%' ORDER BY number ASC")
    fun searchSurahs(query: String): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surahs WHERE number = :number")
    suspend fun getSurahByNumber(number: Int): SurahEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(surahs: List<SurahEntity>)

    @Query("SELECT COUNT(*) FROM surahs")
    suspend fun getCount(): Int
}
