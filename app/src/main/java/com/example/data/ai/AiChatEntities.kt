package com.example.data.ai

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "ai_conversations")
data class AiConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val categoryTag: String = "محادثة عامة",
    val previewText: String = "",
    val messageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "ai_messages",
    foreignKeys = [
        ForeignKey(
            entity = AiConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"])]
)
data class AiMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val conversationId: Long,
    val role: String, // "user" or "assistant"
    val content: String,
    val categoryTag: String = "",
    val referencesSummary: String = "",
    val followUpsSerialized: String = "",
    val attachmentType: String = "", // "image", "audio", "card"
    val attachmentUri: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
