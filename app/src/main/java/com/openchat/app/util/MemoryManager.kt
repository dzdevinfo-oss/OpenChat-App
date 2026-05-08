package com.openchat.app.util

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.openchat.app.data.model.AiModel
import com.openchat.app.data.model.ApiProvider
import com.openchat.app.data.model.Memory
import com.openchat.app.data.model.Message
import com.openchat.app.data.repository.AiApiRepository
import com.openchat.app.data.repository.MemoryRepository
import com.openchat.app.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryManager @Inject constructor(
    private val memoryRepository: MemoryRepository,
    private val aiApiRepository: AiApiRepository,
    private val settingsRepository: SettingsRepository
) {
    private val gson = Gson()
    private val memoryScope = CoroutineScope(Dispatchers.IO)

    suspend fun extractMemories(
        sessionId: String,
        messages: List<Message>,
        provider: ApiProvider,
        model: AiModel
    ) {
        if (!settingsRepository.isMemoryEnabled.value) return

        memoryScope.launch {
            try {
                val lastMessages = messages.takeLast(3)
                if (lastMessages.isEmpty()) return@launch

                val extractionPrompt = """
                    Extract any facts worth remembering from this conversation. 
                    Focus on user preferences, facts about the user, or important context they mentioned.
                    Ignore common greetings or small talk.
                    Return ONLY a JSON array of objects with 'fact' and 'important' fields.
                    Example: [{"fact": "The user likes Python", "important": true}]
                    If nothing new is worth remembering, return [].
                """.trimIndent()

                val response = aiApiRepository.sendSimpleMessage(
                    provider = provider,
                    model = model,
                    systemPrompt = extractionPrompt,
                    userPrompt = lastMessages.joinToString("\n") { "${it.role}: ${it.content}" }
                )

                if (response != null) {
                    val facts = parseFacts(response)
                    facts.filter { it.important }.forEach { fact ->
                        val memory = Memory(
                            id = UUID.randomUUID().toString(),
                            content = fact.fact,
                            createdAt = System.currentTimeMillis(),
                            sessionId = sessionId, // We can store per session or global
                            isActive = true
                        )
                        memoryRepository.insertMemory(memory)
                    }
                }
            } catch (e: Exception) {
                Log.e("MemoryManager", "Extraction failed", e)
            }
        }
    }

    private fun parseFacts(json: String): List<FactExtraction> {
        return try {
            // AI might return markdown blocks
            val cleanJson = if (json.contains("```json")) {
                json.substringAfter("```json").substringBefore("```").trim()
            } else if (json.contains("```")) {
                json.substringAfter("```").substringBefore("```").trim()
            } else {
                json.trim()
            }
            val listType = object : TypeToken<List<FactExtraction>>() {}.type
            gson.fromJson(cleanJson, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMemoryContext(): String {
        if (!settingsRepository.isMemoryEnabled.value) return ""
        val memories = memoryRepository.getAllMemories().firstOrNull() ?: return ""
        if (memories.isEmpty()) return ""

        val sb = StringBuilder("\n\n[MEMORIES]\n")
        memories.filter { it.isActive }.forEach {
            sb.append("- ${it.content}\n")
        }
        sb.append("[/MEMORIES]\n")
        return sb.toString()
    }

    data class FactExtraction(val fact: String, val important: Boolean)
}
