package com.openchat.app.util

import com.openchat.app.data.remote.ContentBlock
import com.openchat.app.data.remote.ImageBlock
import com.openchat.app.data.remote.ImageUrl
import com.openchat.app.data.remote.TextBlock

object MultimodalMessageBuilder {

    fun buildContent(text: String, base64Images: List<String>): List<ContentBlock> {
        val contents = mutableListOf<ContentBlock>()
        
        // Add text part
        if (text.isNotBlank()) {
            contents.add(TextBlock(text))
        }

        // Add image parts
        base64Images.forEach { base64 ->
            contents.add(ImageBlock(ImageUrl("data:image/jpeg;base64,$base64")))
        }

        return contents
    }
}
