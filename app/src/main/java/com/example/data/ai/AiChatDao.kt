package com.example.data.ai

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AiChatDao {
    @Query("SELECT * FROM ai_conversations ORDER BY updatedAt DESC")
    fun observeConversations(): Flow<List<AiConversationEntity>>

    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC, id ASC")
    fun observeMessages(conversationId: Long): Flow<List<AiMessageEntity>>

    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY timestamp DESC, id DESC LIMIT :limit")
    suspend fun getRecentMessagesDesc(conversationId: Long, limit: Int): List<AiMessageEntity>

    @Query("SELECT * FROM ai_conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: Long): AiConversationEntity?

    @Query("SELECT * FROM ai_conversations ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestConversation(): AiConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: AiConversationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiMessageEntity): Long

    @Query(
        "UPDATE ai_conversations SET title = :title, categoryTag = :categoryTag, " +
            "previewText = :previewText, messageCount = :messageCount, updatedAt = :updatedAt " +
            "WHERE id = :id"
    )
    suspend fun updateConversationMeta(
        id: Long,
        title: String,
        categoryTag: String,
        previewText: String,
        messageCount: Int,
        updatedAt: Long
    )

    @Query("DELETE FROM ai_conversations WHERE id = :id")
    suspend fun deleteConversationById(id: Long)
}
