package com.example.data.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiChatRepositoryTest {

    private class FakeAiChatDao : AiChatDao {
        private var nextConvId = 1L
        private var nextMsgId = 1L
        private val conversationsFlow = MutableStateFlow<List<AiConversationEntity>>(emptyList())
        private val messagesFlow = MutableStateFlow<List<AiMessageEntity>>(emptyList())

        override fun observeConversations(): Flow<List<AiConversationEntity>> =
            conversationsFlow.map { list -> list.sortedByDescending { it.updatedAt } }

        override fun observeMessages(conversationId: Long): Flow<List<AiMessageEntity>> =
            messagesFlow.map { list ->
                list.filter { it.conversationId == conversationId }
                    .sortedWith(compareBy<AiMessageEntity> { it.timestamp }.thenBy { it.id })
            }

        override suspend fun getRecentMessagesDesc(conversationId: Long, limit: Int): List<AiMessageEntity> =
            messagesFlow.value
                .filter { it.conversationId == conversationId }
                .sortedWith(compareByDescending<AiMessageEntity> { it.timestamp }.thenByDescending { it.id })
                .take(limit)

        override suspend fun getConversationById(id: Long): AiConversationEntity? =
            conversationsFlow.value.firstOrNull { it.id == id }

        override suspend fun getLatestConversation(): AiConversationEntity? =
            conversationsFlow.value.maxByOrNull { it.updatedAt }

        override suspend fun insertConversation(conversation: AiConversationEntity): Long {
            val assigned = if (conversation.id == 0L) nextConvId++ else conversation.id
            val entity = conversation.copy(id = assigned)
            conversationsFlow.value = conversationsFlow.value.filterNot { it.id == assigned } + entity
            return assigned
        }

        override suspend fun insertMessage(message: AiMessageEntity): Long {
            val assigned = if (message.id == 0L) nextMsgId++ else message.id
            val entity = message.copy(id = assigned)
            messagesFlow.value = messagesFlow.value + entity
            return assigned
        }

        override suspend fun updateConversationMeta(
            id: Long,
            title: String,
            categoryTag: String,
            previewText: String,
            messageCount: Int,
            updatedAt: Long
        ) {
            conversationsFlow.value = conversationsFlow.value.map { item ->
                if (item.id == id) {
                    item.copy(
                        title = title,
                        categoryTag = categoryTag,
                        previewText = previewText,
                        messageCount = messageCount,
                        updatedAt = updatedAt
                    )
                } else {
                    item
                }
            }
        }

        override suspend fun deleteConversationById(id: Long) {
            conversationsFlow.value = conversationsFlow.value.filterNot { it.id == id }
            messagesFlow.value = messagesFlow.value.filterNot { it.conversationId == id }
        }
    }

    @Test
    fun createSaveResumeAndDeleteConversationLifecycleWorksCorrectly() = runBlocking {
        val dao = FakeAiChatDao()
        val repo = AiChatRepository(dao)

        val convId = repo.createConversation(title = "محادثة جديدة", categoryTag = "محادثة إسلامية")
        assertTrue(convId > 0L)

        repo.saveExchange(
            conversationId = convId,
            userText = "ما حكم سجود السهو في الصلاة عند نسيان التشهد الأول؟",
            assistantText = "**سجود السهو وأحكامه في الصلاة**\nمن نسي التشهد الأول يسجد سجدتين قبل السلام.",
            categoryTag = "فتوى ومسألة فقهية",
            referencesSummary = "صحيح البخاري • صحيح مسلم",
            followUps = listOf("أعطني دليلًا آخر", "اختصر الرد")
        )

        val latest = repo.getLatestConversation()
        assertNotNull(latest)
        assertEquals(convId, latest?.id)
        assertEquals(2, latest?.messageCount)
        assertTrue(latest?.title?.contains("ما حكم سجود السهو") == true)
        assertTrue(latest?.title?.endsWith("…") == true)

        val ascending = repo.getRecentMessagesAscending(convId, limit = 8)
        assertEquals(2, ascending.size)
        assertEquals(AiChatRepository.ROLE_USER, ascending[0].role)
        assertEquals(AiChatRepository.ROLE_ASSISTANT, ascending[1].role)
        assertEquals(
            listOf("أعطني دليلًا آخر", "اختصر الرد"),
            AiChatRepository.parseFollowUps(ascending[1].followUpsSerialized)
        )

        val observedMessages = repo.observeMessages(convId).first()
        assertEquals(2, observedMessages.size)

        repo.deleteConversation(convId)
        assertNull(repo.getLatestConversation())
        assertTrue(repo.observeMessages(convId).first().isEmpty())
    }
}
