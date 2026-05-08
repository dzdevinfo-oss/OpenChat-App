package com.openchat.app.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.openchat.app.data.model.AiModel
import com.openchat.app.data.model.ApiProvider
import com.openchat.app.data.model.Message
import com.openchat.app.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

@Singleton
class AiApiRepository @Inject constructor(
    private val retrofitBuilder: RetrofitBuilder,
    private val providerRepository: ProviderRepository,
    private val workspaceRepository: WorkspaceRepository
) {
    suspend fun sendStreamingMessage(
        provider: ApiProvider,
        model: AiModel,
        messages: List<Message>,
        systemPrompt: String?,
        onToken: (String) -> Unit,
        onThinking: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val apiKey = providerRepository.getApiKey(provider.id) 
                ?: throw IllegalStateException("API key not found")
                
            val apiService = retrofitBuilder.build(provider.baseUrl, apiKey)
            val sessionId = messages.firstOrNull()?.sessionId

            var finalSystemPrompt = systemPrompt ?: ""
            if (sessionId != null) {
                val files = workspaceRepository.getFilesBySessionId(sessionId).firstOrNull() ?: emptyList()
                if (files.isNotEmpty()) {
                    val workspaceContext = StringBuilder("\n\n[WORKSPACE_FILES]\n")
                    files.forEach { file ->
                        workspaceContext.append("[File: ${file.fileName}]\n${file.content}\n[/File]\n\n")
                    }
                    workspaceContext.append("[/WORKSPACE_FILES]")
                    finalSystemPrompt += workspaceContext.toString()
                }
            }

            val apiMessages = mutableListOf<ChatMessage>()
            if (finalSystemPrompt.isNotEmpty()) {
                apiMessages.add(ChatMessage("system", finalSystemPrompt))
            }

            val gson = Gson()
            messages.filter { !it.isStreaming }.forEach { msg ->
                val base64Images = try {
                    if (msg.attachments.isNotEmpty() && msg.attachments != "[]") {
                        gson.fromJson<List<String>>(msg.attachments, object : TypeToken<List<String>>() {}.type)
                    } else emptyList()
                } catch (e: Exception) {
                    emptyList<String>()
                }

                val contentAny = if (base64Images.isNotEmpty()) {
                    val contents = mutableListOf<ContentBlock>()
                    if (msg.content.isNotBlank()) {
                        contents.add(TextBlock(text = msg.content))
                    }
                    base64Images.forEach { base64 ->
                        contents.add(ImageBlock(image_url = ImageUrl(url = "data:image/jpeg;base64,$base64")))
                    }
                    contents
                } else {
                    msg.content
                }
                
                apiMessages.add(ChatMessage(msg.role, contentAny))
            }

            val thinkingConfig = if (model.modelId.contains("thinking") || model.modelId.contains("reasoning")) {
                ThinkingConfig(budgetTokens = 10000)
            } else null

            val request = ChatRequest(
                model = model.modelId,
                messages = apiMessages,
                stream = true,
                system = if (finalSystemPrompt.isNotEmpty()) finalSystemPrompt else null,
                thinking = thinkingConfig
            )

            val response = apiService.chatCompletionsStreaming(request)
            
            if (!response.isSuccessful) {
                if (response.code() == 401) throw RuntimeException("401 Unauthorized - Check API Key")
                if (response.code() == 429) throw RuntimeException("429 Rate Limited")
                throw RuntimeException("API Error: ${response.code()} ${response.message()}")
            }

            val responseBody = response.body() ?: throw RuntimeException("Empty response body")
            
            StreamingParser.parse(responseBody).collect { chunk ->
                when (chunk) {
                    is StreamingParser.StreamChunk.Content -> onToken(chunk.text)
                    is StreamingParser.StreamChunk.Thinking -> onThinking(chunk.text)
                }
            }
            onComplete()
        } catch (e: Exception) {
            onError(e)
        }
    }

    suspend fun sendSimpleMessage(
        provider: ApiProvider,
        model: AiModel,
        systemPrompt: String?,
        userPrompt: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val apiKey = providerRepository.getApiKey(provider.id) ?: return@withContext null
            val apiService = retrofitBuilder.build(provider.baseUrl, apiKey)

            val apiMessages = mutableListOf<ChatMessage>()
            if (!systemPrompt.isNullOrEmpty()) {
                apiMessages.add(ChatMessage("system", systemPrompt))
            }
            apiMessages.add(ChatMessage("user", userPrompt))

            val request = ChatRequest(
                model = model.modelId,
                messages = apiMessages,
                stream = false,
                system = systemPrompt
            )

            val response = apiService.chatCompletions(request)
            if (response.isSuccessful) {
                val bodyString = response.body()?.string() ?: return@withContext null
                val json = JSONObject(bodyString)
                val choices = json.getJSONArray("choices")
                if (choices.length() > 0) {
                    val choice = choices.getJSONObject(0)
                    val message = choice.getJSONObject("message")
                    return@withContext message.getString("content")
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchModels(provider: ApiProvider): List<AiModel> = withContext(Dispatchers.IO) {
        try {
            val apiKey = providerRepository.getApiKey(provider.id) ?: return@withContext emptyList()
            val apiService = retrofitBuilder.build(provider.baseUrl, apiKey)
            val response = apiService.getModels()
            
            if (response.isSuccessful) {
                val body = response.body()
                val list = body?.data ?: body?.models ?: emptyList()
                list.map { item ->
                    val resolvedId = item.id ?: item.name?.replace("models/", "") ?: ""
                    val resolvedName = item.name?.replace("models/", "") ?: resolvedId
                    AiModel(
                        id = UUID.randomUUID().toString(),
                        modelId = resolvedId,
                        displayName = resolvedName,
                        providerId = provider.id,
                        isBuiltIn = false,
                        censorMode = "default",
                        contextWindow = null,
                        supportsVision = true,
                        supportsStreaming = true
                    )
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
