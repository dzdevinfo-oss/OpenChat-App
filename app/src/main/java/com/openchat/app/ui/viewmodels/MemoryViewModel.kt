package com.openchat.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openchat.app.data.model.Memory
import com.openchat.app.data.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    val allMemories: StateFlow<List<Memory>> = memoryRepository.getAllMemories()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addMemory(content: String) {
        viewModelScope.launch {
            memoryRepository.insertMemory(
                Memory(
                    id = UUID.randomUUID().toString(),
                    content = content,
                    createdAt = System.currentTimeMillis(),
                    sessionId = null,
                    isActive = true
                )
            )
        }
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch {
            memoryRepository.deleteMemory(id)
        }
    }
}
