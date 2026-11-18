package com.example.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey
    val number: Int,
    val name: String,
    val englishName: String,
    val revelationType: String,
    val numberOfAyahs: Int,
    val audioUrl: String,
    val versesSerialized: String, // comma or newline separated verses
    val pageNumber: Int
)
