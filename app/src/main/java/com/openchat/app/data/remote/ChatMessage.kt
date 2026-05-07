package com.openchat.app.data.remote

data class ChatMessage(
    val role: String,
    val content: Any
)

sealed class ContentBlock(val type: String)

data class TextBlock(
    val text: String
) : ContentBlock("text")

data class ImageBlock(
    val image_url: ImageUrl
) : ContentBlock("image_url")

data class ImageUrl(
    val url: String
)
