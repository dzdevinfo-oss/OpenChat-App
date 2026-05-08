package com.openchat.app.util

data class Artifact(
    val id: String,
    val type: ArtifactType,
    val language: String,
    val content: String,
    val title: String
)

enum class ArtifactType {
    HTML, SVG, REACT, CODE, UNKNOWN
}

object ArtifactDetector {
    fun detectArtifacts(text: String): List<Artifact> {
        val artifacts = mutableListOf<Artifact>()
        
        // Match code blocks like ```html ... ``` or ```svg ... ```
        val regex = "```([\\w-]+)?\\n([\\s\\S]*?)```".toRegex()
        val matches = regex.findAll(text)
        
        matches.forEach { match ->
            val lang = match.groups[1]?.value?.lowercase() ?: "text"
            val content = match.groups[2]?.value ?: ""
            
            val type = when (lang) {
                "html" -> ArtifactType.HTML
                "svg" -> ArtifactType.SVG
                "react", "jsx", "tsx" -> ArtifactType.REACT
                "javascript", "js", "python", "py", "bash", "sh" -> ArtifactType.CODE
                else -> ArtifactType.UNKNOWN
            }
            
            if (type != ArtifactType.UNKNOWN) {
                val title = when (type) {
                    ArtifactType.HTML -> "Web Page"
                    ArtifactType.SVG -> "Vector Graphic"
                    ArtifactType.REACT -> "React Component"
                    ArtifactType.CODE -> "Code Snippet"
                    else -> "Artifact"
                }
                
                artifacts.add(Artifact(
                    id = java.util.UUID.randomUUID().toString(),
                    type = type,
                    language = lang,
                    content = content,
                    title = title
                ))
            }
        }
        
        return artifacts
    }
}
