package com.openchat.app.util

import com.openchat.app.data.model.WorkspaceFile
import com.openchat.app.data.repository.WorkspaceRepository
import kotlinx.coroutines.flow.firstOrNull
import java.io.File
import java.util.UUID

class WorkspaceActionHandler(
    private val workspaceRepository: WorkspaceRepository,
    private val sessionId: String,
    private val filesDir: File
) {
    private val createRegex = """```create:([^\n]+)\n(.*?)\n```""".toRegex(RegexOption.DOT_MATCHES_ALL)
    private val editRegex = """```edit:([^\n]+)\n(.*?)\n```""".toRegex(RegexOption.DOT_MATCHES_ALL)
    private val deleteRegex = """```delete:([^\n]+)```""".toRegex()

    suspend fun processContent(content: String): List<String> {
        val notifications = mutableListOf<String>()

        // Process Create
        createRegex.findAll(content).forEach { match ->
            val fileName = match.groupValues[1].trim()
            val fileContent = match.groupValues[2]
            val type = fileName.substringAfterLast('.', "txt")
            
            val workspaceDir = File(filesDir, "workspace/$sessionId")
            val filePath = File(workspaceDir, fileName).absolutePath
            
            val newFile = WorkspaceFile(
                id = UUID.randomUUID().toString(),
                workspaceId = sessionId,
                sessionId = sessionId,
                fileName = fileName,
                filePath = filePath,
                fileType = type,
                content = fileContent,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isDeleted = false,
                previousContent = null
            )
            workspaceRepository.insertFile(newFile)
            notifications.add("File created: $fileName")
        }

        // Process Edit
        editRegex.findAll(content).forEach { match ->
            val fileName = match.groupValues[1].trim()
            val newContent = match.groupValues[2]
            
            val files = workspaceRepository.getFilesBySessionId(sessionId).firstOrNull() ?: emptyList()
            val fileToEdit = files.find { it.fileName == fileName }
            
            if (fileToEdit != null) {
                workspaceRepository.updateFileContent(fileToEdit.id, newContent)
                notifications.add("File updated: $fileName ✓")
            }
        }

        // Process Delete
        deleteRegex.findAll(content).forEach { match ->
            val fileName = match.groupValues[1].trim()
            
            val files = workspaceRepository.getFilesBySessionId(sessionId).firstOrNull() ?: emptyList()
            val fileToDelete = files.find { it.fileName == fileName }
            
            if (fileToDelete != null) {
                workspaceRepository.deleteFile(fileToDelete.id)
                notifications.add("File deleted: $fileName")
            }
        }

        return notifications
    }
    
    suspend fun getWorkspaceContext(): String {
        val files = workspaceRepository.getFilesBySessionId(sessionId).firstOrNull() ?: emptyList()
        if (files.isEmpty()) return ""
        
        val sb = StringBuilder()
        sb.append("[WORKSPACE_FILES]\n")
        files.forEach { file ->
            sb.append("- ${file.fileName} (${file.content.length} chars)\n")
        }
        sb.append("[/WORKSPACE_FILES]")
        return sb.toString()
    }

    suspend fun getFileContent(fileName: String): String? {
        val files = workspaceRepository.getFilesBySessionId(sessionId).firstOrNull() ?: emptyList()
        return files.find { it.fileName == fileName }?.content
    }
}
