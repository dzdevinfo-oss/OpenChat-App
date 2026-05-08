package com.openchat.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openchat.app.data.model.Memory
import com.openchat.app.data.repository.ChatRepository
import com.openchat.app.data.repository.SettingsRepository
import com.openchat.app.data.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val chatRepository: ChatRepository,
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    val globalSystemPrompt: StateFlow<String> = settingsRepository.globalSystemPrompt
    val isMemoryEnabled: StateFlow<Boolean> = settingsRepository.isMemoryEnabled
    val theme: StateFlow<String> = settingsRepository.theme
    
    val memories: StateFlow<List<Memory>> = memoryRepository.getAllMemories()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val fontSize: StateFlow<Int> = settingsRepository.fontSize
    val bubbleStyle: StateFlow<String> = settingsRepository.bubbleStyle
    val showTimestamps: StateFlow<Boolean> = settingsRepository.showTimestamps
    val temperature: StateFlow<Float> = settingsRepository.temperature
    val maxTokens: StateFlow<Int> = settingsRepository.maxTokens
    val streamingEnabled: StateFlow<Boolean> = settingsRepository.streamingEnabled
    val extendedThinking: StateFlow<Boolean> = settingsRepository.extendedThinking
    val thinkingBudget: StateFlow<Int> = settingsRepository.thinkingBudget
    val autoInjectWorkspace: StateFlow<Boolean> = settingsRepository.autoInjectWorkspace
    val maxContextFiles: StateFlow<Int> = settingsRepository.maxContextFiles
    val autoSaveArtifacts: StateFlow<Boolean> = settingsRepository.autoSaveArtifacts
    val isFirstLaunch: StateFlow<Boolean> = settingsRepository.isFirstLaunch

    fun setFontSize(size: Int) { settingsRepository.setFontSize(size) }
    fun setBubbleStyle(style: String) { settingsRepository.setBubbleStyle(style) }
    fun setShowTimestamps(show: Boolean) { settingsRepository.setShowTimestamps(show) }
    fun setTemperature(temp: Float) { settingsRepository.setTemperature(temp) }
    fun setMaxTokens(tokens: Int) { settingsRepository.setMaxTokens(tokens) }
    fun setStreamingEnabled(enabled: Boolean) { settingsRepository.setStreamingEnabled(enabled) }
    fun setExtendedThinking(enabled: Boolean) { settingsRepository.setExtendedThinking(enabled) }
    fun setThinkingBudget(budget: Int) { settingsRepository.setThinkingBudget(budget) }
    fun setAutoInjectWorkspace(enabled: Boolean) { settingsRepository.setAutoInjectWorkspace(enabled) }
    fun setMaxContextFiles(files: Int) { settingsRepository.setMaxContextFiles(files) }
    fun setAutoSaveArtifacts(enabled: Boolean) { settingsRepository.setAutoSaveArtifacts(enabled) }
    fun setFirstLaunchFinished() { settingsRepository.setFirstLaunchFinished() }

    fun exportAllData(): android.net.Uri {
        // Implement ZIP export logic
        return android.net.Uri.EMPTY 
    }

    fun importData(uri: android.net.Uri) {
        // Implement ZIP import logic
    }

    fun updateSystemPrompt(prompt: String) {
        settingsRepository.setGlobalSystemPrompt(prompt)
    }

    fun toggleMemory(enabled: Boolean) {
        settingsRepository.setMemoryEnabled(enabled)
    }
    
    fun setTheme(theme: String) {
        settingsRepository.setTheme(theme)
    }

    fun clearAllHistory(onComplete: () -> Unit) {
        viewModelScope.launch {
            chatRepository.deleteAllSessions()
            onComplete()
        }
    }

    fun deleteMemory(memoryId: String) {
        viewModelScope.launch {
            memoryRepository.deleteMemory(memoryId)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            memoryRepository.clearAllMemories()
        }
    }
}
