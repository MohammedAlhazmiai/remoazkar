package com.example.ui.viewmodel

import android.app.Application
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
    val trainedEntriesCount: Int = 0
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

    private var loadedEntries: List<CorpusEntry> = emptyList()

    private val messagesFlow = activeConversationIdFlow.flatMapLatest { convId ->
        if (convId == null) flowOf(emptyList()) else repository.observeMessages(convId)
    }

    val uiState: StateFlow<IslamicAiUiState> = combine(
        repository.conversations,
        activeConversationIdFlow,
        messagesFlow,
        isGeneratingFlow,
        modelReadyFlow,
        entriesCountFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        IslamicAiUiState(
            conversations = values[0] as List<AiConversationEntity>,
            activeConversationId = values[1] as Long?,
            messages = values[2] as List<AiMessageEntity>,
            isGenerating = values[3] as Boolean,
            isModelReady = values[4] as Boolean,
            trainedEntriesCount = values[5] as Int
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = IslamicAiUiState()
    )

    init {
        viewModelScope.launch {
            val entries = withContext(Dispatchers.IO) {
                IslamicAiModel.prepareAssets(getApplication())
                IslamicCorpusLoader.ensureLoaded(getApplication())
            }
            loadedEntries = entries
            entriesCountFlow.value = entries.size
            modelReadyFlow.value = true

            // استعادة أحدث محادثة محفوظة تلقائيًا لمواصلة الحوار إن وُجدت
            val latest = withContext(Dispatchers.IO) { repository.getLatestConversation() }
            if (activeConversationIdFlow.value == null && latest != null) {
                activeConversationIdFlow.value = latest.id
            }
        }
    }

    fun startNewConversation() {
        activeConversationIdFlow.value = null
    }

    fun selectConversation(conversationId: Long) {
        activeConversationIdFlow.value = conversationId
    }

    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            if (activeConversationIdFlow.value == conversationId) {
                activeConversationIdFlow.value = null
            }
            repository.deleteConversation(conversationId)
        }
    }

    fun sendMessage(rawInput: String) {
        val clean = rawInput.trim()
        if (clean.isEmpty() || isGeneratingFlow.value) return

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
                            title = clean.replace('\n', ' ').take(42),
                            categoryTag = "محادثة إسلامية"
                        )
                    activeConversationIdFlow.value = targetConvId

                    val recentHistory = repository.getRecentMessagesAscending(targetConvId, limit = 8)
                    val reply = IslamicAiModel.generateReply(
                        query = clean,
                        entries = loadedEntries,
                        recentMessages = recentHistory
                    )

                    repository.saveExchange(
                        conversationId = targetConvId,
                        userText = clean,
                        assistantText = reply.text,
                        categoryTag = reply.domainTag,
                        referencesSummary = reply.references.joinToString(" • "),
                        followUps = reply.followUps
                    )
                    targetConvId to reply
                }
                activeConversationIdFlow.value = convId
                // response handled via Room Flow
                response.domainTag
            } finally {
                isGeneratingFlow.value = false
            }
        }
    }
}
