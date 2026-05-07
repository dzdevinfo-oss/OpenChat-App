package com.openchat.app.data.remote

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    @SerializedName("max_tokens") val maxTokens: Int? = null,
    val temperature: Double? = null,
    val system: String? = null,
    val thinking: ThinkingConfig? = null
)

data class ThinkingConfig(
    val type: String = "enabled",
    @SerializedName("budget_tokens") val budgetTokens: Int
)
