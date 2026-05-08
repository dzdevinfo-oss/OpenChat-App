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
import com.openchat.app.data.repository.WorkspaceRepository
import com.openchat.app.util.WorkspaceActionHandler
import com.openchat.app.voice.VoiceManager
import com.openchat.app.util.MemoryManager
import com.openchat.app.util.AgentManager
import com.openchat.app.util.AgentStatus
import com.openchat.app.util.ImageProcessor
import com.openchat.app.util.FileProcessor
import com.openchat.app.util.MultimodalMessageBuilder
import com.openchat.app.util.VoiceInputManager
import com.openchat.app.ui.components.ArtifactPanel
import com.openchat.app.util.Artifact
import com.openchat.app.util.ArtifactDetector
import com.openchat.app.util.TerminalExecutor
import com.openchat.app.data.model.WorkspaceFile
import com.openchat.app.data.repository.WorkspaceRepository
import java.io.File
import java.util.UUID
import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val workspaceRepository: WorkspaceRepository,
    private val voiceManager: VoiceManager,
    private val memoryManager: MemoryManager,
    private val agentManager: AgentManager,
    private val voiceInputManager: VoiceInputManager,
    private val imageProcessor: ImageProcessor,
    private val fileProcessor: FileProcessor,
    private val workspaceRepository: WorkspaceRepository,
    @ApplicationContext private val context: Context
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

    val agentStatuses: StateFlow<Map<String, AgentStatus>> = agentManager.agentStatuses
    
    private val _currentArtifacts = MutableStateFlow<List<Artifact>>(emptyList())
    val currentArtifacts = _currentArtifacts.asStateFlow()

    private val _selectedArtifact = MutableStateFlow<Artifact?>(null)
    val selectedArtifact = _selectedArtifact.asStateFlow()

    private val _isArtifactPanelOpen = MutableStateFlow(false)
    val isArtifactPanelOpen = _isArtifactPanelOpen.asStateFlow()

    fun showArtifact(artifact: Artifact) {
        _selectedArtifact.value = artifact
        _isArtifactPanelOpen.value = true
    }

    fun closeArtifactPanel() {
        _isArtifactPanelOpen.value = false
    }

    fun scanForArtifacts(messageId: String) {
        val message = _messages.value.find { it.id == messageId } ?: return
        val artifacts = ArtifactDetector.detectArtifacts(message.content)
        if (artifacts.isNotEmpty()) {
            _currentArtifacts.value = artifacts
            // Auto-open first artifact if it's new
            _selectedArtifact.value = artifacts.first()
            _isArtifactPanelOpen.value = true
        }
    }

    val isListening: StateFlow<Boolean> = voiceInputManager.isListening
    val partialTranscription: StateFlow<String> = voiceInputManager.partialText

    private val connectivityManager = context.getSystemService(android.net.ConnectivityManager::class.java)
    private val _isOnline = MutableStateFlow(true)
    val isOnline = _isOnline.asStateFlow()

    init {
        val networkRequest = android.net.NetworkRequest.Builder().build()
        connectivityManager.registerNetworkCallback(networkRequest, object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) { _isOnline.value = true }
            override fun onLost(network: android.net.Network) { _isOnline.value = false }
        })
    }

    private val terminalExecutor = TerminalExecutor(context)
    
    fun saveToWorkspace(language: String, code: String) {
        val sid = currentSession.value?.id ?: return
        val ext = when (language.lowercase()) {
            "python", "py" -> "py"
            "javascript", "js" -> "js"
            "html" -> "html"
            "css" -> "css"
            "bash", "sh" -> "sh"
            else -> "txt"
        }
        val filename = "snippet_${System.currentTimeMillis()}.$ext"
        
        viewModelScope.launch {
            val workspaceDir = File(context.filesDir, "workspace/$sid")
            if (!workspaceDir.exists()) workspaceDir.mkdirs()
            
            val filePath = File(workspaceDir, filename).absolutePath
            val newFile = WorkspaceFile(
                id = UUID.randomUUID().toString(),
                workspaceId = sid,
                sessionId = sid,
                fileName = filename,
                filePath = filePath,
                fileType = ext,
                content = code,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isDeleted = false,
                previousContent = null
            )
            workspaceRepository.insertFile(newFile)
            // Show toast or notification? For now we'll assume it works
        }
    }

    fun getTerminalExecutor() = terminalExecutor

    fun startVoiceInput(onResult: (String) -> Unit) {
        voiceInputManager.startListening(onResult)
    }

    fun stopVoiceInput() {
        voiceInputManager.stopListening()
    }

    fun stopSpeaking() {
        voiceManager.stop()
    }

    init {
        viewModelScope.launch {
            combine(availableModels, settingsRepository.defaultModelId, settingsRepository.defaultProviderId) { models, defModelId, defProvId ->
                Triple(models, defModelId, defProvId)
            }.collect { (models, defModelId, defProvId) ->
                if (_selectedModel.value == null && models.isNotEmpty()) {
                    val fallback = models.find { it.modelId == "gemini-2.5-pro-preview-05-06" } ?: models.first()
                    _selectedModel.value = models.find { it.id == defModelId } ?: fallback
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
            
            // Persist as default for future sessions
            settingsRepository.setDefaultModel(model.id, model.providerId)
            
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

    private suspend fun buildSystemPrompt(session: Session?): String? {
        var basePrompt = session?.systemPrompt ?: settingsRepository.globalSystemPrompt.value
        
        val memoryContext = memoryManager.getMemoryContext()
        if (memoryContext.isNotEmpty()) {
            basePrompt = if (basePrompt.isBlank()) memoryContext else "$basePrompt\n\n$memoryContext"
        }

        session?.let { s ->
            val handler = WorkspaceActionHandler(workspaceRepository, s.id, context.filesDir)
            val workspaceContext = handler.getWorkspaceContext()
            if (workspaceContext.isNotBlank()) {
                basePrompt = if (basePrompt.isBlank()) workspaceContext else "$basePrompt\n\n$workspaceContext"
            }
        }
        
        return basePrompt.takeIf { it.isNotBlank() }
    }

    fun clearAllSessions() {
        viewModelScope.launch {
            chatRepository.clearAllSessions()
        }
    }

    private fun generateSessionTitle(sessionId: String, firstMessage: String) {
        viewModelScope.launch {
            try {
                val currentContextModel = _selectedModel.value ?: return@launch
                val currentContextProvider = _selectedProvider.value ?: return@launch
                
                val titlePrompt = "Give this conversation a 5-word title based on this message: \"$firstMessage\". Just the title, no quotes."
                var titleAccumulator = ""
                
                aiApiRepository.sendStreamingMessage(
                    provider = currentContextProvider,
                    model = currentContextModel,
                    messages = listOf(Message(UUID.randomUUID().toString(), sessionId, "user", titlePrompt, System.currentTimeMillis())),
                    systemPrompt = "You are a helpful assistant that generates short, concise titles for chat sessions.",
                    onToken = { token ->
                        titleAccumulator += token
                    },
                    onThinking = {},
                    onComplete = {
                        launch {
                            val finalTitle = titleAccumulator.trim().ifEmpty { 
                                if (firstMessage.length > 30) firstMessage.take(30) + "..." else firstMessage
                            }
                            _currentSession.value?.let { session ->
                                if (session.id == sessionId) {
                                    val updatedSession = session.copy(title = finalTitle)
                                    chatRepository.updateSession(updatedSession)
                                    _currentSession.value = updatedSession
                                }
                            }
                        }
                    },
                    onError = {}
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleStreamingComplete(sessionId: String) {
        viewModelScope.launch {
            _isStreaming.value = false
            val finalContent = _currentStreamingContent.value
            _currentStreamingContent.value = ""
            _currentStreamingThinking.value = ""
            
            // Process workspace actions
            val handler = WorkspaceActionHandler(workspaceRepository, sessionId, context.filesDir)
            handler.processContent(finalContent)

            // Extract memories
            _selectedModel.value?.let { model ->
                _selectedProvider.value?.let { provider ->
                    memoryManager.extractMemories(sessionId, _messages.value, provider, model)
                }
            }
        }
    }

    fun sendMessage(content: String, attachments: List<Uri>) {
        if (content.isBlank() && attachments.isEmpty()) return
        val currentContextModel = _selectedModel.value ?: return
        val currentContextProvider = _selectedProvider.value ?: return

        viewModelScope.launch {
            var session = _currentSession.value
            val isNewSession = session == null
            if (isNewSession) {
                // Determine a quick title
                val titlePreview = if (content.length > 30) content.take(30) + "..." else if (content.isEmpty()) "File Attachment" else content
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
                chatRepository.insertSession(session!!)
                _currentSession.value = session
                observeSessionMessages(session!!.id)
            }

            var finalContent = content
            val base64Images = mutableListOf<String>()

            // Process attachments
            attachments.forEach { uri ->
                val mimeType = context.contentResolver.getType(uri) ?: ""
                if (mimeType.startsWith("image/")) {
                    imageProcessor.processImage(context, uri)?.let { base64 ->
                        base64Images.add(base64)
                    }
                } else {
                    fileProcessor.processFile(context, uri)?.let { fileInfo ->
                        finalContent = "$finalContent\n\n[File: ${fileInfo.name}]\n${fileInfo.content}\n[/File]"
                    }
                }
            }

            // Stringify attachments (Base64 images) for DB
            val attachmentsJson = if (base64Images.isEmpty()) "[]" else {
                Gson().toJson(base64Images)
            }

            // User Message
            val userMsg = Message(
                id = UUID.randomUUID().toString(),
                sessionId = session!!.id,
                role = "user",
                content = finalContent,
                timestamp = System.currentTimeMillis(),
                isStreaming = false,
                tokenCount = null,
                attachments = attachmentsJson,
                thinkingContent = null
            )
            chatRepository.insertMessage(userMsg)
            chatRepository.updateSession(session!!.copy(updatedAt = System.currentTimeMillis()))

            // Check if user wants to "read" a file
            var additionalContext = ""
            if (content.startsWith("read ", ignoreCase = true)) {
                val fileName = content.substring(5).trim().removePrefix("workspace/")
                val fileContent = WorkspaceActionHandler(workspaceRepository, session.id, context.filesDir).getFileContent(fileName)
                if (fileContent != null) {
                    additionalContext = "\n\n[File: $fileName]\n$fileContent"
                }
            }

            if (isNewSession) {
                // Background update title with AI
                generateSessionTitle(session!!.id, content)
            }

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
                    val messagesToSend = chatRepository.getLastNMessages(session.id, 50).sortedBy { it.timestamp }.toMutableList()
                    
                    if (additionalContext.isNotEmpty() && messagesToSend.isNotEmpty()) {
                        val last = messagesToSend.last()
                        messagesToSend[messagesToSend.size - 1] = last.copy(content = last.content + additionalContext)
                    }
                    
                    val finalPrompt = buildSystemPrompt(session)
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
                                handleStreamingComplete(session.id)
                                
                                // Auto-read if preference or context suggests
                                if (settingsRepository.autoRead.value) {
                                    voiceManager.speak(
                                        _currentStreamingContent.value,
                                        speed = settingsRepository.ttsSpeed.value,
                                        pitch = settingsRepository.ttsPitch.value
                                    )
                                }
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
            
            val finalPrompt = buildSystemPrompt(session)
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
                            handleStreamingComplete(session.id)
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
                     
                     val finalPrompt = buildSystemPrompt(session)
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
                                     handleStreamingComplete(session.id)
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

    fun launchAgent(sessionId: String, task: String) {
        val model = _selectedModel.value ?: return
        val provider = _selectedProvider.value ?: return
        agentManager.launchAgent(sessionId, task, provider, model)
    }

    fun stopAgent(sessionId: String) {
        agentManager.stopAgent(sessionId)
    }
}
