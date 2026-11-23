package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.CorpusEntry
import com.example.ai.IslamicAiModel
import com.example.ai.IslamicCorpusLoader
import com.example.data.HisnRepository
import com.example.data.QuranRepository
import com.example.data.ai.AiChatDatabase
import com.example.data.ai.AiChatRepository
import com.example.data.ai.AiConversationEntity
import com.example.data.ai.AiMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class IslamicAiUiState(
    val conversations: List<AiConversationEntity> = emptyList(),
    val activeConversationId: Long? = null,
    val messages: List<AiMessageEntity> = emptyList(),
    val isGenerating: Boolean = false,
    val isModelReady: Boolean = false,
    val trainedEntriesCount: Int = 0,
    val dismissedSupportMessageIds: Set<Long> = emptySet()
)

@OptIn(ExperimentalCoroutinesApi::class)
class IslamicAiViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AiChatRepository(
        AiChatDatabase.getInstance(application).aiChatDao()
    )

    private val activeConversationIdFlow = MutableStateFlow<Long?>(null)
    private val isGeneratingFlow = MutableStateFlow(false)
    private val modelReadyFlow = MutableStateFlow(false)
    private val entriesCountFlow = MutableStateFlow(0)
    private val dismissedSupportIdsFlow = MutableStateFlow<Set<Long>>(emptySet())

    private var loadedEntries: List<CorpusEntry> = emptyList()
    @Volatile
    private var userChangedConversation = false

    private val messagesFlow = activeConversationIdFlow.flatMapLatest { convId ->
        if (convId == null) flowOf(emptyList()) else repository.observeMessages(convId)
    }

    val uiState: StateFlow<IslamicAiUiState> = combine(
        repository.conversations,
        activeConversationIdFlow,
        messagesFlow,
        isGeneratingFlow,
        modelReadyFlow,
        entriesCountFlow,
        dismissedSupportIdsFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        IslamicAiUiState(
            conversations = values[0] as List<AiConversationEntity>,
            activeConversationId = values[1] as Long?,
            messages = values[2] as List<AiMessageEntity>,
            isGenerating = values[3] as Boolean,
            isModelReady = values[4] as Boolean,
            trainedEntriesCount = values[5] as Int,
            dismissedSupportMessageIds = values[6] as Set<Long>
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = IslamicAiUiState()
    )

    init {
        viewModelScope.launch {
            runCatching {
                val entries = withContext(Dispatchers.IO) {
                    IslamicAiModel.prepareAssets(getApplication())
                    IslamicCorpusLoader.ensureLoaded(getApplication())
                }
                loadedEntries = entries
                entriesCountFlow.value = entries.size
                modelReadyFlow.value = true

                // استعادة أحدث محادثة محفوظة تلقائيًا ما لم يختر المستخدم محادثة جديدة أثناء التهيئة
                if (!userChangedConversation && activeConversationIdFlow.value == null) {
                    val latest = withContext(Dispatchers.IO) { repository.getLatestConversation() }
                    if (!userChangedConversation && activeConversationIdFlow.value == null && latest != null) {
                        activeConversationIdFlow.value = latest.id
                    }
                }
            }.onFailure { error ->
                Log.e("IslamicAiViewModel", "Failed initializing offline AI model", error)
            }
        }
    }

    fun startNewConversation() {
        userChangedConversation = true
        activeConversationIdFlow.value = null
    }

    fun selectConversation(conversationId: Long) {
        userChangedConversation = true
        activeConversationIdFlow.value = conversationId
    }

    fun dismissSupportPrompt(messageId: Long) {
        dismissedSupportIdsFlow.value = dismissedSupportIdsFlow.value + messageId
    }

    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            if (activeConversationIdFlow.value == conversationId) {
                activeConversationIdFlow.value = null
            }
            repository.deleteConversation(conversationId)
        }
    }

    companion object {
        private val EXCLUDED_SUPPORT_TAGS = setOf(
            "ترحيب وتعريف",
            "دردشة يومية ودية",
            "دردشة واستدلال ذكي",
            "مواساة ودعم إيماني",
            "نصيحة إيمانية وعملية",
            "شكر وثناء",
            "متابعة السياق",
            "ملخص موجز",
            "دعاء مأثور"
        )

        /**
         * يضبط ظهور رسالة دعم المطور لتظهر أحيانًا فقط عند إتمام مهام فعلية مختارة
         * (مثل المهمة الثانية ثم كل ثلاث مهام مكتملة)، وليس بعد كل طلب أو رسالة دردشة أو تحية.
         */
        fun shouldShowSupportPrompt(
            messages: List<AiMessageEntity>,
            messageIndex: Int,
            dismissedMessageIds: Set<Long>
        ): Boolean {
            val msg = messages.getOrNull(messageIndex) ?: return false
            if (msg.role != AiChatRepository.ROLE_ASSISTANT) return false
            if (msg.id in dismissedMessageIds) return false
            if (msg.categoryTag in EXCLUDED_SUPPORT_TAGS) return false

            val completedTaskOrdinal = messages
                .take(messageIndex + 1)
                .count { it.role == AiChatRepository.ROLE_ASSISTANT && it.categoryTag !in EXCLUDED_SUPPORT_TAGS }

            return completedTaskOrdinal >= 2 && (completedTaskOrdinal - 2) % 3 == 0
        }
    }

    fun sendMessage(
        rawInput: String,
        attachmentType: String = "",
        attachmentUri: String = ""
    ) {
        val clean = rawInput.trim()
        if (clean.isEmpty() && attachmentUri.isEmpty()) return
        if (isGeneratingFlow.value) return
        userChangedConversation = true

        val displayUserText = if (clean.isNotBlank()) {
            clean
        } else if (attachmentType == "image") {
            "طلب استفسار حول صورة مرفوعة"
        } else if (attachmentType == "audio") {
            "طلب استفسار عبر تسجيل صوتي مرفوع"
        } else {
            "طلب"
        }

        viewModelScope.launch {
            isGeneratingFlow.value = true
            try {
                val (convId, response) = withContext(Dispatchers.IO) {
                    if (loadedEntries.isEmpty()) {
                        QuranRepository.ensureLoaded(getApplication())
                        HisnRepository.ensureLoaded(getApplication())
                        loadedEntries = IslamicCorpusLoader.ensureLoaded(getApplication())
                        entriesCountFlow.value = loadedEntries.size
                        modelReadyFlow.value = true
                    }

                    val targetConvId = activeConversationIdFlow.value
                        ?: repository.createConversation(
                            title = displayUserText.replace('\n', ' ').take(42),
                            categoryTag = "محادثة إسلامية"
                        )
                    activeConversationIdFlow.value = targetConvId

                    val recentHistory = repository.getRecentMessagesAscending(targetConvId, limit = 8)
                    val reply = IslamicAiModel.generateReply(
                        query = clean,
                        entries = loadedEntries,
                        recentMessages = recentHistory,
                        context = getApplication(),
                        userAttachmentType = attachmentType,
                        userAttachmentUri = attachmentUri
                    )

                    repository.saveExchange(
                        conversationId = targetConvId,
                        userText = displayUserText,
                        assistantText = reply.text,
                        categoryTag = reply.domainTag,
                        referencesSummary = reply.references.joinToString(" • "),
                        followUps = reply.followUps,
                        userAttachmentType = attachmentType,
                        userAttachmentUri = attachmentUri,
                        assistantAttachmentType = reply.attachmentType,
                        assistantAttachmentUri = reply.attachmentUri
                    )
                    targetConvId to reply
                }
                activeConversationIdFlow.value = convId
                response.domainTag
            } catch (error: Exception) {
                Log.e("IslamicAiViewModel", "Error generating or saving offline AI response", error)
            } finally {
                isGeneratingFlow.value = false
            }
        }
    }
}
