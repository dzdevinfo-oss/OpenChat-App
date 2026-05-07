package com.openchat.app.data.remote

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody
import java.io.BufferedReader
import java.io.InputStreamReader

object StreamingParser {
    fun parse(responseBody: ResponseBody): Flow<StreamChunk> = flow {
        val reader = BufferedReader(InputStreamReader(responseBody.byteStream()))
        val gson = Gson()
        
        try {
            var line: String? = reader.readLine()
            while (line != null) {
                if (line.startsWith("data: ")) {
                    val data = line.substring(6).trim()
                    if (data == "[DONE]") {
                        break
                    }
                    try {
                        val json = gson.fromJson(data, JsonObject::class.java)
                        
                        // OpenAI format
                        if (json.has("choices")) {
                            val choices = json.getAsJsonArray("choices")
                            if (choices.size() > 0) {
                                val choice = choices[0].asJsonObject
                                if (choice.has("delta")) {
                                    val delta = choice.getAsJsonObject("delta")
                                    if (delta.has("content") && !delta.get("content").isJsonNull) {
                                        emit(StreamChunk.Content(delta.get("content").asString))
                                    }
                                    if (delta.has("reasoning_content") && !delta.get("reasoning_content").isJsonNull) {
                                        emit(StreamChunk.Thinking(delta.get("reasoning_content").asString))
                                    }
                                    if (delta.has("thinking") && !delta.get("thinking").isJsonNull) {
                                        emit(StreamChunk.Thinking(delta.get("thinking").asString))
                                    }
                                }
                            }
                        }
                        
                        // Anthropic format (via OpenRouter or native)
                        if (json.has("type")) {
                            val type = json.get("type").asString
                            if (type == "content_block_delta") {
                                val delta = json.getAsJsonObject("delta")
                                if (delta.has("text")) {
                                    emit(StreamChunk.Content(delta.get("text").asString))
                                } else if (delta.has("thinking")) {
                                    emit(StreamChunk.Thinking(delta.get("thinking").asString))
                                }
                            }
                        }
                        
                    } catch (e: Exception) {
                        // Ignore individual JSON parse errors to keep stream alive
                    }
                }
                line = reader.readLine()
            }
        } finally {
            reader.close()
        }
    }

    sealed class StreamChunk {
        data class Content(val text: String) : StreamChunk()
        data class Thinking(val text: String) : StreamChunk()
    }
}
