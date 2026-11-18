package com.example.util

import android.content.Context
import android.content.Intent

object ShareUtil {
    fun share(context: Context, text: String, subject: String = "ريمواذكار") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, "مشاركة").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

object FavKeys {
    fun hisn(chapterId: Int, index: Int) = "zikr:$chapterId:$index"
    fun ayah(surah: Int, number: Int) = "ayah:$surah:$number"
    fun surah(number: Int) = "surah:$number"
    fun custom(id: Long) = "custom:$id"
}
