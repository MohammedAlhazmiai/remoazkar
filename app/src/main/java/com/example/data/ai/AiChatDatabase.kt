package com.example.data.ai

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AiConversationEntity::class, AiMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AiChatDatabase : RoomDatabase() {
    abstract fun aiChatDao(): AiChatDao

    companion object {
        @Volatile
        private var INSTANCE: AiChatDatabase? = null

        fun getInstance(context: Context): AiChatDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AiChatDatabase::class.java,
                    "islamic_ai_chat.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
