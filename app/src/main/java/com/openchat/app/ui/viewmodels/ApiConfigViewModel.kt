package com.openchat.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class ApiConfigViewModel @Inject constructor(
    private val providerRepository: ProviderRepository
) : ViewModel() {

    val providers: StateFlow<List<ApiProvider>> = providerRepository.getAllProviders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addProvider(name: String, url: String, apiKey: String, onResult: (Boolean, String) -> Unit) {
        if (name.isBlank() || url.isBlank() || apiKey.isBlank()) {
            onResult(false, "All fields are required.")
            return
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            onResult(false, "URL must start with http:// or https://")
            return
        }

        val existingProviders = providers.value
        if (existingProviders.any { it.name.equals(name, ignoreCase = true) }) {
            onResult(false, "Provider name already exists.")
            return
        }

        viewModelScope.launch {
            try {
                val newProvider = ApiProvider(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    baseUrl = url,
                    encryptedApiKey = "", // Set in repository
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                )
                providerRepository.insertProvider(newProvider, apiKey)
                onResult(true, "Provider added ✓")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to add provider")
            }
        }
    }

    fun updateProvider(provider: ApiProvider, newName: String, newUrl: String, newApiKey: String?, onResult: (Boolean, String) -> Unit) {
        if (newName.isBlank() || newUrl.isBlank()) {
            onResult(false, "Name and URL are required.")
            return
        }

        if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) {
            onResult(false, "URL must start with http:// or https://")
            return
        }

        viewModelScope.launch {
            try {
                val updatedProvider = provider.copy(name = newName, baseUrl = newUrl, isActive = true)
                providerRepository.updateProvider(updatedProvider, newApiKey?.takeIf { it.isNotBlank() })
                onResult(true, "Provider updated ✓")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to update provider")
            }
        }
    }

    fun deleteProvider(id: String) {
        viewModelScope.launch {
            providerRepository.deleteProvider(id)
        }
    }
}
