package com.example.data.ai

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AiConversationEntity::class, AiMessageEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AiChatDatabase : RoomDatabase() {
    abstract fun aiChatDao(): AiChatDao

    companion object {
        @Volatile
        private var INSTANCE: AiChatDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ai_messages ADD COLUMN attachmentType TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE ai_messages ADD COLUMN attachmentUri TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AiChatDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AiChatDatabase::class.java,
                    "islamic_ai_chat.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
