package com.openchat.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openchat.app.data.model.AiModel
import com.openchat.app.data.model.ApiProvider
import com.openchat.app.data.repository.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CustomModelsViewModel @Inject constructor(
    private val providerRepository: ProviderRepository
) : ViewModel() {

    val models: StateFlow<List<AiModel>> = providerRepository.getAllModels()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeProviders: StateFlow<List<ApiProvider>> = providerRepository.getActiveProviders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addModel(modelId: String, displayName: String, providerId: String, censorMode: String, onResult: (Boolean, String) -> Unit) {
        if (modelId.isBlank() || displayName.isBlank() || providerId.isBlank()) {
            onResult(false, "All fields are required.")
            return
        }

        val existingModels = models.value
        if (existingModels.any { it.modelId.equals(modelId, ignoreCase = true) && it.providerId == providerId }) {
            // Might allow same model id on different providers, but not same provider
            onResult(false, "Model ID already exists for this provider.")
            return
        }

        viewModelScope.launch {
            try {
                val newModel = AiModel(
                    id = UUID.randomUUID().toString(),
                    modelId = modelId,
                    displayName = displayName,
                    providerId = providerId,
                    isBuiltIn = false,
                    censorMode = censorMode,
                    contextWindow = null,
                    supportsVision = true, // default or let user choose later
                    supportsStreaming = true
                )
                providerRepository.insertModel(newModel)
                onResult(true, "Custom model added ✓")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to add model")
            }
        }
    }

    fun updateModel(model: AiModel, newModelId: String, newDisplayName: String, newProviderId: String, newCensorMode: String, onResult: (Boolean, String) -> Unit) {
        if (newModelId.isBlank() || newDisplayName.isBlank() || newProviderId.isBlank()) {
            onResult(false, "All fields are required.")
            return
        }

        viewModelScope.launch {
            try {
                val updatedModel = model.copy(
                    modelId = newModelId,
                    displayName = newDisplayName,
                    providerId = newProviderId,
                    censorMode = newCensorMode
                )
                providerRepository.updateModel(updatedModel)
                onResult(true, "Model updated ✓")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to update model")
            }
        }
    }

    fun deleteModel(id: String) {
        viewModelScope.launch {
            providerRepository.deleteModel(id)
        }
    }
}
