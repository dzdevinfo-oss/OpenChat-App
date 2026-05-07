package com.openchat.app.data.repository

import com.openchat.app.data.db.dao.WorkspaceFileDao
import com.openchat.app.data.model.WorkspaceFile
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkspaceRepository @Inject constructor(
    private val workspaceFileDao: WorkspaceFileDao
) {
    fun getFilesBySessionId(sessionId: String): Flow<List<WorkspaceFile>> = workspaceFileDao.getBySessionId(sessionId)

    suspend fun getFileById(id: String): WorkspaceFile? = workspaceFileDao.getById(id)

    suspend fun insertFile(file: WorkspaceFile) {
        val diskFile = File(file.filePath)
        diskFile.parentFile?.mkdirs()
        diskFile.writeText(file.content)
        workspaceFileDao.insert(file)
    }

    suspend fun updateFileContent(id: String, newContent: String) {
        val existingFile = workspaceFileDao.getById(id) ?: return
        val currentContent = existingFile.content
        
        val diskFile = File(existingFile.filePath)
        diskFile.writeText(newContent)
        
        val updatedFile = existingFile.copy(
            content = newContent,
            previousContent = currentContent,
            updatedAt = System.currentTimeMillis()
        )
        workspaceFileDao.update(updatedFile)
    }

    suspend fun deleteFile(id: String) {
        workspaceFileDao.delete(id)
    }

    suspend fun undoLastEdit(id: String) {
        val file = workspaceFileDao.getById(id) ?: return
        if (file.previousContent != null) {
            val diskFile = File(file.filePath)
            diskFile.writeText(file.previousContent)
            workspaceFileDao.undoLastEdit(id)
        }
    }

    suspend fun permanentDelete(id: String) {
        val existingFile = workspaceFileDao.getById(id)
        existingFile?.let {
            val diskFile = File(it.filePath)
            if (diskFile.exists()) {
                diskFile.delete()
            }
        }
        workspaceFileDao.permanentDelete(id)
    }

    suspend fun recoverDeleted(id: String) {
        workspaceFileDao.recoverDeleted(id)
    }

    fun getDeletedBySession(sessionId: String): Flow<List<WorkspaceFile>> {
        return workspaceFileDao.getDeletedBySession(sessionId)
    }

    suspend fun renameFile(id: String, newName: String) {
        val existingFile = workspaceFileDao.getById(id) ?: return
        val diskFile = File(existingFile.filePath)
        val newFile = File(diskFile.parentFile, newName)
        if (diskFile.exists()) {
            diskFile.renameTo(newFile)
        }
        workspaceFileDao.renameFile(id, newName, newFile.absolutePath)
    }
}
