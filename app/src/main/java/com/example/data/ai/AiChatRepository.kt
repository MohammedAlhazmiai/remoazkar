package com.example.data.ai

import kotlinx.coroutines.flow.Flow

class AiChatRepository(private val dao: AiChatDao) {

    val conversations: Flow<List<AiConversationEntity>> = dao.observeConversations()

    fun observeMessages(conversationId: Long): Flow<List<AiMessageEntity>> =
        dao.observeMessages(conversationId)

    suspend fun getLatestConversation(): AiConversationEntity? =
        dao.getLatestConversation()

    suspend fun createConversation(
        title: String = "محادثة جديدة",
        categoryTag: String = "محادثة إسلامية"
    ): Long {
        val now = System.currentTimeMillis()
        return dao.insertConversation(
            AiConversationEntity(
                title = title,
                categoryTag = categoryTag,
                previewText = "",
                messageCount = 0,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun getRecentMessagesAscending(conversationId: Long, limit: Int = 8): List<AiMessageEntity> =
        dao.getRecentMessagesDesc(conversationId, limit).reversed()

    suspend fun saveExchange(
        conversationId: Long,
        userText: String,
        assistantText: String,
        categoryTag: String,
        referencesSummary: String,
        followUps: List<String>
    ) {
        val now = System.currentTimeMillis()
        val existing = dao.getConversationById(conversationId)
        dao.insertMessage(
            AiMessageEntity(
                conversationId = conversationId,
                role = ROLE_USER,
                content = userText,
                timestamp = now
            )
        )
        dao.insertMessage(
            AiMessageEntity(
                conversationId = conversationId,
                role = ROLE_ASSISTANT,
                content = assistantText,
                categoryTag = categoryTag,
                referencesSummary = referencesSummary,
                followUpsSerialized = followUps.joinToString(FOLLOWUP_SEPARATOR),
                timestamp = now + 1L
            )
        )

        val currentTitle = existing?.title.orEmpty()
        val derivedTitle = if (currentTitle.isBlank() || currentTitle == "محادثة جديدة") {
            userText.trim().replace('\n', ' ').take(42).let { snippet ->
                if (userText.trim().length > 42) "$snippet…" else snippet
            }.ifBlank { "محادثة إسلامية" }
        } else {
            currentTitle
        }
        val nextCount = (existing?.messageCount ?: 0) + 2
        val preview = assistantText.lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.take(80)
            .orEmpty()

        dao.updateConversationMeta(
            id = conversationId,
            title = derivedTitle,
            categoryTag = categoryTag,
            previewText = preview,
            messageCount = nextCount,
            updatedAt = now + 1L
        )
    }

    suspend fun deleteConversation(id: Long) {
        dao.deleteConversationById(id)
    }

    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
        const val FOLLOWUP_SEPARATOR = "\u001F"

        fun parseFollowUps(serialized: String): List<String> =
            if (serialized.isBlank()) emptyList()
            else serialized.split(FOLLOWUP_SEPARATOR).filter { it.isNotBlank() }
    }
}
