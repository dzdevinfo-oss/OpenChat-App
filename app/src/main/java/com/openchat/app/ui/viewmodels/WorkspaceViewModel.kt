package com.openchat.app.ui.viewmodels

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openchat.app.data.model.WorkspaceFile
import com.openchat.app.data.repository.WorkspaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

@HiltViewModel
class WorkspaceViewModel @Inject constructor(
    private val workspaceRepository: WorkspaceRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _sessionId = MutableStateFlow<String?>(null)

    val currentFiles = _sessionId.filterNotNull().flatMapLatest { sid ->
        workspaceRepository.getFilesBySessionId(sid)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val deletedFiles = _sessionId.filterNotNull().flatMapLatest { sid ->
        workspaceRepository.getDeletedBySession(sid)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentlyOpenFile = MutableStateFlow<WorkspaceFile?>(null)
    val currentlyOpenFile: StateFlow<WorkspaceFile?> = _currentlyOpenFile.asStateFlow()

    fun setSessionId(sessionId: String) {
        _sessionId.value = sessionId
    }

    private val _terminalHistory = MutableStateFlow<String>("")
    val terminalHistory = _terminalHistory.asStateFlow()
    
    private val terminalExecutor = TerminalExecutor(context)

    fun executeCommand(command: String) {
        val sid = _sessionId.value ?: return
        _terminalHistory.value += "\n$ $command"
        viewModelScope.launch {
            val workingDir = File(context.filesDir, "workspaces/$sid")
            val result = terminalExecutor.execute(command, workingDir)
            _terminalHistory.value += "\n$result"
        }
    }

    fun clearTerminal() {
        _terminalHistory.value = ""
    }

    fun openFile(file: WorkspaceFile) {
        _currentlyOpenFile.value = file
    }

    fun closeFile() {
        _currentlyOpenFile.value = null
    }

    fun createFile(name: String, content: String, type: String, parentId: String? = null) {
        val sid = _sessionId.value ?: return
        viewModelScope.launch {
            val workspaceDir = File(context.filesDir, "workspace/$sid")
            val filePath = File(workspaceDir, name).absolutePath
            val newFile = WorkspaceFile(
                id = UUID.randomUUID().toString(),
                workspaceId = sid,
                sessionId = sid,
                fileName = name,
                filePath = filePath,
                fileType = type,
                content = content,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isDeleted = false,
                previousContent = null,
                isFolder = false,
                parentId = parentId
            )
            workspaceRepository.insertFile(newFile)
            _currentlyOpenFile.value = newFile
        }
    }

    fun createFolder(name: String, parentId: String? = null) {
        val sid = _sessionId.value ?: return
        viewModelScope.launch {
            val newFolder = WorkspaceFile(
                id = UUID.randomUUID().toString(),
                workspaceId = sid,
                sessionId = sid,
                fileName = name,
                filePath = "", // Folders don't need a direct file path in this simple implementation
                fileType = "folder",
                content = "",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isDeleted = false,
                previousContent = null,
                isFolder = true,
                parentId = parentId
            )
            workspaceRepository.insertFile(newFolder)
        }
    }

    fun updateFileContent(id: String, newContent: String) {
        viewModelScope.launch {
            workspaceRepository.updateFileContent(id, newContent)
            // Update currently open file
            if (_currentlyOpenFile.value?.id == id) {
                _currentlyOpenFile.value = workspaceRepository.getFileById(id)
            }
        }
    }

    fun deleteFile(id: String) {
        viewModelScope.launch {
            workspaceRepository.deleteFile(id)
            if (_currentlyOpenFile.value?.id == id) {
                _currentlyOpenFile.value = null
            }
        }
    }

    fun renameFile(id: String, newName: String) {
        viewModelScope.launch {
            workspaceRepository.renameFile(id, newName)
            if (_currentlyOpenFile.value?.id == id) {
                _currentlyOpenFile.value = workspaceRepository.getFileById(id)
            }
        }
    }

    fun recoverFile(id: String) {
        viewModelScope.launch {
            workspaceRepository.recoverDeleted(id)
        }
    }

    private val redoStack = mutableMapOf<String, String>() // fileId -> content

    fun undoLastEdit(id: String) {
        viewModelScope.launch {
            val file = workspaceRepository.getFileById(id) ?: return@launch
            file.content.let { redoStack[id] = it }
            
            workspaceRepository.undoLastEdit(id)
            if (_currentlyOpenFile.value?.id == id) {
                _currentlyOpenFile.value = workspaceRepository.getFileById(id)
            }
        }
    }

    fun redoLastEdit(id: String) {
        val redoContent = redoStack[id] ?: return
        updateFileContent(id, redoContent)
        redoStack.remove(id)
    }

    fun permanentDelete(id: String) {
        viewModelScope.launch {
            workspaceRepository.permanentDelete(id)
        }
    }

    fun exportWorkspaceAsZip(): Uri? {
        val sid = _sessionId.value ?: return null
        val files = currentFiles.value
        if (files.isEmpty()) return null

        return try {
            val zipDir = File(context.cacheDir, "exports")
            zipDir.mkdirs()
            val zipFile = File(zipDir, "workspace_\${sid}.zip")
            
            ZipOutputStream(FileOutputStream(zipFile)).use { zout ->
                for (file in files) {
                    val entry = ZipEntry(file.fileName)
                    zout.putNextEntry(entry)
                    zout.write(file.content.toByteArray())
                    zout.closeEntry()
                }
            }
            
            FileProvider.getUriForFile(
                context,
                "\${context.packageName}.fileprovider",
                zipFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun importFileFromDevice(uri: Uri) {
        val sid = _sessionId.value ?: return
        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(uri)) ?: "txt"
                var name = uri.lastPathSegment ?: "imported_\${System.currentTimeMillis()}"
                if (!name.contains(".")) {
                    name += ".\$extension"
                }

                val content = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                
                val workspaceDir = File(context.filesDir, "workspace/\$sid")
                val filePath = File(workspaceDir, name).absolutePath
                val newFile = WorkspaceFile(
                    id = UUID.randomUUID().toString(),
                    workspaceId = sid,
                    sessionId = sid,
                    fileName = name,
                    filePath = filePath,
                    fileType = extension,
                    content = content,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isDeleted = false,
                    previousContent = null
                )
                workspaceRepository.insertFile(newFile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
