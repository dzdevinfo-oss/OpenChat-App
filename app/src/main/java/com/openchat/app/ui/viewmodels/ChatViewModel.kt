package com.openchat.app.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openchat.app.data.model.AiModel
import com.openchat.app.data.model.ApiProvider
import com.openchat.app.data.model.Message
import com.openchat.app.data.model.Session
import com.openchat.app.data.repository.AiApiRepository
import com.openchat.app.data.repository.ChatRepository
import com.openchat.app.data.repository.ProviderRepository
import com.openchat.app.data.repository.SettingsRepository
import com.openchat.app.data.repository.MemoryRepository
import com.openchat.app.voice.VoiceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val providerRepository: ProviderRepository,
    private val aiApiRepository: AiApiRepository,
    private val settingsRepository: SettingsRepository,
    private val memoryRepository: MemoryRepository,
    private val voiceManager: VoiceManager
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _currentStreamingContent = MutableStateFlow("")
    val currentStreamingContent: StateFlow<String> = _currentStreamingContent.asStateFlow()

    private val _currentStreamingThinking = MutableStateFlow("")
    val currentStreamingThinking: StateFlow<String> = _currentStreamingThinking.asStateFlow()

    private val _currentSession = MutableStateFlow<Session?>(null)
    val currentSession: StateFlow<Session?> = _currentSession.asStateFlow()

    private val _selectedModel = MutableStateFlow<AiModel?>(null)
    val selectedModel: StateFlow<AiModel?> = _selectedModel.asStateFlow()

    private val _selectedProvider = MutableStateFlow<ApiProvider?>(null)
    val selectedProvider: StateFlow<ApiProvider?> = _selectedProvider.asStateFlow()

    private var sessionMessagesJob: Job? = null
    private var streamingJob: Job? = null
    
    val allSessions: StateFlow<List<Session>> = chatRepository.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        
    val availableModels: StateFlow<List<AiModel>> = providerRepository.getAllModels()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeProviders: StateFlow<List<ApiProvider>> = providerRepository.getActiveProviders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            availableModels.collect { models ->
                if (_selectedModel.value == null && models.isNotEmpty()) {
                    _selectedModel.value = models.find { it.modelId == "gemini-2.5-pro-preview-05-06" } ?: models.first()
                    _selectedModel.value?.let { model ->
                        _selectedProvider.value = providerRepository.getProviderById(model.providerId)
                    }
                }
            }
        }
    }

    fun selectModel(model: AiModel) {
        viewModelScope.launch {
            _selectedModel.value = model
            _selectedProvider.value = providerRepository.getProviderById(model.providerId)
            _currentSession.value?.let { session ->
                chatRepository.updateSession(session.copy(modelId = model.id, providerId = model.providerId))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.shutdown()
    }

    fun speak(text: String) {
        voiceManager.speak(text)
    }

    fun stopSpeaking() {
        voiceManager.stop()
    }

    fun loadSession(sessionId: String) {
        if (sessionId == "new") {
            newSession()
            return
        }
        viewModelScope.launch {
            val session = chatRepository.getSessionById(sessionId)
            _currentSession.value = session
            session?.let {
                val model = providerRepository.getAllModels().firstOrNull()?.find { m -> m.id == it.modelId }
                if (model != null) {
                    _selectedModel.value = model
                    _selectedProvider.value = providerRepository.getProviderById(model.providerId)
                }
                observeSessionMessages(it.id)
            }
        }
    }

    fun newSession() {
        _currentSession.value = null
        _messages.value = emptyList()
        sessionMessagesJob?.cancel()
    }

    private fun observeSessionMessages(sessionId: String) {
        sessionMessagesJob?.cancel()
        sessionMessagesJob = viewModelScope.launch {
            chatRepository.getMessagesBySessionId(sessionId).collect { msgs ->
                // Sort descending for reversed LazyColumn
                _messages.value = msgs.sortedByDescending { it.timestamp }
            }
        }
    }

    private suspend fun buildSystemPrompt(sessionSystemPrompt: String?): String? {
        var basePrompt = sessionSystemPrompt ?: settingsRepository.globalSystemPrompt.value
        
        if (settingsRepository.isMemoryEnabled.value) {
            val memories = memoryRepository.getAllMemories().first()
            if (memories.isNotEmpty()) {
                val memoryText = memories.joinToString("\n") { "- \${it.content}" }
                val memoryContext = "Information you know about the user:\\n\$memoryText"
                basePrompt = if (basePrompt.isBlank()) memoryContext else "\$basePrompt\\n\\n\$memoryContext"
            }
        }
        
        return basePrompt.takeIf { it.isNotBlank() }
    }

    fun sendMessage(content: String, attachments: List<Uri>) {
        if (content.isBlank() && attachments.isEmpty()) return
        val currentContextModel = _selectedModel.value ?: return
        val currentContextProvider = _selectedProvider.value ?: return

        viewModelScope.launch {
            var session = _currentSession.value
            if (session == null) {
                // Determine a quick title
                val titlePreview = if (content.length > 20) content.take(20) + "..." else content
                session = Session(
                    id = UUID.randomUUID().toString(),
                    title = titlePreview,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    modelId = currentContextModel.id,
                    providerId = currentContextProvider.id,
                    systemPrompt = null,
                    isPinned = false,
                    workspaceId = null
                )
                chatRepository.insertSession(session)
                _currentSession.value = session
                observeSessionMessages(session.id)
            }

            // User Message
            val userMsg = Message(
                id = UUID.randomUUID().toString(),
                sessionId = session.id,
                role = "user",
                content = content,
                timestamp = System.currentTimeMillis(),
                isStreaming = false,
                tokenCount = null,
                attachments = "[]", // TODO format attachments based on URIs
                thinkingContent = null
            )
            chatRepository.insertMessage(userMsg)
            chatRepository.updateSession(session.copy(updatedAt = System.currentTimeMillis()))

            // Assistant placeholder message
            val assistantMsgId = UUID.randomUUID().toString()
            val initialAssistantMsg = Message(
                id = assistantMsgId,
                sessionId = session.id,
                role = "assistant",
                content = "",
                timestamp = System.currentTimeMillis(),
                isStreaming = true,
                tokenCount = null,
                attachments = "[]",
                thinkingContent = null
            )
            chatRepository.insertMessage(initialAssistantMsg)

            _isStreaming.value = true
            _currentStreamingContent.value = ""
            _currentStreamingThinking.value = ""

            // Send to AI API
            streamingJob = viewModelScope.launch {
                try {
                    val messagesToSend = chatRepository.getLastNMessages(session.id, 50).sortedBy { it.timestamp }
                    val finalPrompt = buildSystemPrompt(session.systemPrompt)
                    aiApiRepository.sendStreamingMessage(
                        provider = currentContextProvider,
                        model = currentContextModel,
                        messages = messagesToSend,
                        systemPrompt = finalPrompt,
                        onToken = { token ->
                            _currentStreamingContent.value += token
                            launch {
                                chatRepository.updateMessage(initialAssistantMsg.copy(content = _currentStreamingContent.value, thinkingContent = _currentStreamingThinking.value))
                            }
                        },
                        onThinking = { thinkingToken ->
                             _currentStreamingThinking.value += thinkingToken
                             launch {
                                chatRepository.updateMessage(initialAssistantMsg.copy(content = _currentStreamingContent.value, thinkingContent = _currentStreamingThinking.value))
                            }
                        },
                        onComplete = {
                            launch {
                                chatRepository.updateMessage(
                                    initialAssistantMsg.copy(
                                        content = _currentStreamingContent.value,
                                        thinkingContent = _currentStreamingThinking.value,
                                        isStreaming = false
                                    )
                                )
                                _isStreaming.value = false
                                _currentStreamingContent.value = ""
                                _currentStreamingThinking.value = ""
                            }
                        },
                        onError = { error ->
                            error.printStackTrace()
                            launch {
                                val errorContent = _currentStreamingContent.value + "\n\n⚠️ Error: ${error.message}"
                                chatRepository.updateMessage(
                                    initialAssistantMsg.copy(
                                        content = errorContent,
                                        isStreaming = false
                                    )
                                )
                                _isStreaming.value = false
                                _currentStreamingContent.value = ""
                                _currentStreamingThinking.value = ""
                            }
                        }
                    )
                } catch (e: Exception) {
                    _isStreaming.value = false
                    e.printStackTrace()
                }
            }
        }
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        _isStreaming.value = false
        // Update the last message to mark as not streaming
        viewModelScope.launch {
            val msgs = _messages.value
            val streamingMsg = msgs.find { it.isStreaming }
            if (streamingMsg != null) {
                chatRepository.updateMessage(streamingMsg.copy(isStreaming = false))
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.deleteMessage(messageId)
        }
    }

    fun editAndResend(messageId: String, newContent: String) {
        // Find message, update it, remove subsequent messages, resend
        viewModelScope.launch {
            val msg = _messages.value.find { it.id == messageId } ?: return@launch
            if (msg.role != "user") return@launch
            
            chatRepository.updateMessage(msg.copy(content = newContent, timestamp = System.currentTimeMillis()))
            
            val msgsAfter = _messages.value.filter { it.timestamp > msg.timestamp }
            msgsAfter.forEach { chatRepository.deleteMessage(it.id) }
            
            val currentContextModel = _selectedModel.value ?: return@launch
            val currentContextProvider = _selectedProvider.value ?: return@launch
            val session = _currentSession.value ?: return@launch
            
            val assistantMsgId = UUID.randomUUID().toString()
            val initialAssistantMsg = Message(
                id = assistantMsgId,
                sessionId = session.id,
                role = "assistant",
                content = "",
                timestamp = System.currentTimeMillis(),
                isStreaming = true,
                tokenCount = null,
                attachments = "[]",
                thinkingContent = null
            )
            chatRepository.insertMessage(initialAssistantMsg)

            _isStreaming.value = true
            _currentStreamingContent.value = ""
            _currentStreamingThinking.value = ""
            
            val finalPrompt = buildSystemPrompt(session.systemPrompt)
            streamingJob = viewModelScope.launch {
                val messagesToSend = chatRepository.getLastNMessages(session.id, 50).sortedBy { it.timestamp }
                aiApiRepository.sendStreamingMessage(
                    provider = currentContextProvider,
                    model = currentContextModel,
                    messages = messagesToSend,
                    systemPrompt = finalPrompt,
                    onToken = { token ->
                        _currentStreamingContent.value += token
                        launch { chatRepository.updateMessage(initialAssistantMsg.copy(content = _currentStreamingContent.value, thinkingContent = _currentStreamingThinking.value)) }
                    },
                    onThinking = { thinkingToken ->
                         _currentStreamingThinking.value += thinkingToken
                         launch { chatRepository.updateMessage(initialAssistantMsg.copy(content = _currentStreamingContent.value, thinkingContent = _currentStreamingThinking.value)) }
                    },
                    onComplete = {
                        launch {
                            chatRepository.updateMessage(initialAssistantMsg.copy(content = _currentStreamingContent.value, thinkingContent = _currentStreamingThinking.value, isStreaming = false))
                            _isStreaming.value = false
                        }
                    },
                    onError = {
                        _isStreaming.value = false
                    }
                )
            }
        }
    }

    fun regenerateLastResponse() {
        viewModelScope.launch {
            val lastMsg = _messages.value.firstOrNull() ?: return@launch
            if (lastMsg.role == "assistant") {
                chatRepository.deleteMessage(lastMsg.id)
                // Just trigger a new assistant message generation based on the history
                val lastUserMsg = _messages.value.firstOrNull { it.role == "user" }
                if (lastUserMsg != null) {
                     // We don't want to re-insert the user message, we just want to run the generation
                     val currentContextModel = _selectedModel.value ?: return@launch
                     val currentContextProvider = _selectedProvider.value ?: return@launch
                     val session = _currentSession.value ?: return@launch
                     
                     val assistantMsgId = UUID.randomUUID().toString()
                     val initialAssistantMsg = Message(
                         id = assistantMsgId,
                         sessionId = session.id,
                         role = "assistant",
                         content = "",
                         timestamp = System.currentTimeMillis(),
                         isStreaming = true,
                         tokenCount = null,
                         attachments = "[]",
                         thinkingContent = null
                     )
                     chatRepository.insertMessage(initialAssistantMsg)

                     _isStreaming.value = true
                     _currentStreamingContent.value = ""
                     _currentStreamingThinking.value = ""
                     
                     val finalPrompt = buildSystemPrompt(session.systemPrompt)
                     streamingJob = viewModelScope.launch {
                         val messagesToSend = chatRepository.getLastNMessages(session.id, 50).sortedBy { it.timestamp }
                         aiApiRepository.sendStreamingMessage(
                             provider = currentContextProvider,
                             model = currentContextModel,
                             messages = messagesToSend,
                             systemPrompt = finalPrompt,
                             onToken = { token ->
                                 _currentStreamingContent.value += token
                                 launch { chatRepository.updateMessage(initialAssistantMsg.copy(content = _currentStreamingContent.value)) }
                             },
                             onThinking = { thinkingToken ->
                                  _currentStreamingThinking.value += thinkingToken
                                  launch { chatRepository.updateMessage(initialAssistantMsg.copy(content = _currentStreamingContent.value, thinkingContent = _currentStreamingThinking.value)) }
                             },
                             onComplete = {
                                 launch {
                                     chatRepository.updateMessage(initialAssistantMsg.copy(content = _currentStreamingContent.value, thinkingContent = _currentStreamingThinking.value, isStreaming = false))
                                     _isStreaming.value = false
                                 }
                             },
                             onError = {
                                 _isStreaming.value = false
                             }
                         )
                     }
                }
            }
        }
    }
}
