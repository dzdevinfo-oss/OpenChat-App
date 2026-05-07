package com.openchat.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openchat.app.data.repository.ChatRepository
import com.openchat.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    val globalSystemPrompt: StateFlow<String> = settingsRepository.globalSystemPrompt
    val isMemoryEnabled: StateFlow<Boolean> = settingsRepository.isMemoryEnabled
    val theme: StateFlow<String> = settingsRepository.theme

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
}
