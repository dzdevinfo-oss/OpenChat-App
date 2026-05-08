package com.openchat.app.data.remote

data class ChatMessage(
    val role: String,
    val content: Any
)

sealed class ContentBlock {
    abstract val type: String
}

data class TextBlock(
    override val type: String = "text",
    val text: String
) : ContentBlock()

data class ImageBlock(
    override val type: String = "image_url",
    val image_url: ImageUrl
) : ContentBlock()

data class ImageUrl(
    val url: String
)
