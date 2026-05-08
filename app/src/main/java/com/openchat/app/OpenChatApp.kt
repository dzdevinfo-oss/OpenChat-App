package com.openchat.app

import android.app.Application
import com.openchat.app.data.db.dao.AiModelDao
import com.openchat.app.data.db.dao.ApiProviderDao
import com.openchat.app.data.model.AiModel
import com.openchat.app.data.model.ApiProvider
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltAndroidApp
class OpenChatApp : Application() {

    @Inject
    lateinit var apiProviderDao: ApiProviderDao

    @Inject
    lateinit var aiModelDao: AiModelDao

    // Global coroutine exception handler for top-level app errors
    private val globalExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + globalExceptionHandler)

    override fun onCreate() {
        super.onCreate()
        seedDatabase()
    }

    private fun seedDatabase() {
        applicationScope.launch {
            val existingProviders = apiProviderDao.getAll().firstOrNull()
            if (existingProviders.isNullOrEmpty()) {
                val googleId = UUID.randomUUID().toString()
                val openRouterId = UUID.randomUUID().toString()
                val openaiId = UUID.randomUUID().toString()
                val groqId = UUID.randomUUID().toString()
                val togetherId = UUID.randomUUID().toString()
                val mistralId = UUID.randomUUID().toString()

                apiProviderDao.insert(
                    ApiProvider(id = googleId, name = "Google AI Studio", baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai/", encryptedApiKey = "", isActive = true, createdAt = System.currentTimeMillis())
                )
                apiProviderDao.insert(
                    ApiProvider(id = openRouterId, name = "OpenRouter", baseUrl = "https://openrouter.ai/api/v1/", encryptedApiKey = "", isActive = false, createdAt = System.currentTimeMillis())
                )
                apiProviderDao.insert(
                    ApiProvider(id = openaiId, name = "OpenAI", baseUrl = "https://api.openai.com/v1/", encryptedApiKey = "", isActive = false, createdAt = System.currentTimeMillis())
                )
                apiProviderDao.insert(
                    ApiProvider(id = groqId, name = "Groq", baseUrl = "https://api.groq.com/openai/v1/", encryptedApiKey = "", isActive = false, createdAt = System.currentTimeMillis())
                )
                apiProviderDao.insert(
                    ApiProvider(id = togetherId, name = "Together AI", baseUrl = "https://api.together.xyz/v1/", encryptedApiKey = "", isActive = false, createdAt = System.currentTimeMillis())
                )
                apiProviderDao.insert(
                    ApiProvider(id = mistralId, name = "Mistral", baseUrl = "https://api.mistral.ai/v1/", encryptedApiKey = "", isActive = false, createdAt = System.currentTimeMillis())
                )

                val defaultModels = listOf(
                    AiModel(UUID.randomUUID().toString(), "gemini-2.5-pro-preview-05-06", "Gemini 2.5 Pro", googleId, true, "default", 2000000, true, true),
                    AiModel(UUID.randomUUID().toString(), "gemini-2.5-flash-preview-04-17", "Gemini 2.5 Flash", googleId, true, "default", 1000000, true, true),
                    AiModel(UUID.randomUUID().toString(), "gemini-2.0-flash", "Gemini 2.0 Flash", googleId, true, "default", 1000000, true, true),
                    AiModel(UUID.randomUUID().toString(), "gemini-2.0-flash-lite", "Gemini 2.0 Flash Lite", googleId, true, "default", 1000000, true, true),
                    AiModel(UUID.randomUUID().toString(), "gemini-1.5-pro", "Gemini 1.5 Pro", googleId, true, "default", 2000000, true, true),
                    AiModel(UUID.randomUUID().toString(), "gemini-1.5-flash", "Gemini 1.5 Flash", googleId, true, "default", 1000000, true, true),
                    
                    AiModel(UUID.randomUUID().toString(), "google/gemini-2.5-pro-preview", "Gemini 2.5 Pro (OR)", openRouterId, true, "default", null, true, true),
                    AiModel(UUID.randomUUID().toString(), "anthropic/claude-sonnet-4-5", "Claude 4.5 Sonnet", openRouterId, true, "default", null, true, true),
                    AiModel(UUID.randomUUID().toString(), "anthropic/claude-opus-4", "Claude 4 Opus", openRouterId, true, "default", null, true, true),
                    AiModel(UUID.randomUUID().toString(), "openai/gpt-4o", "GPT-4o", openRouterId, true, "default", null, true, true),
                    AiModel(UUID.randomUUID().toString(), "openai/o3", "o3", openRouterId, true, "default", null, false, true),
                    AiModel(UUID.randomUUID().toString(), "meta-llama/llama-3.3-70b-instruct", "Llama 3.3 70B", openRouterId, true, "default", null, false, true),
                    AiModel(UUID.randomUUID().toString(), "qwen/qwen3-32b", "Qwen 3 32B", openRouterId, true, "default", null, false, true),
                    AiModel(UUID.randomUUID().toString(), "mistralai/mistral-large", "Mistral Large", openRouterId, true, "default", null, false, true)
                )

                defaultModels.forEach { aiModelDao.insert(it) }
            }
        }
    }
}
