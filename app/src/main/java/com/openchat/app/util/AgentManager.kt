package com.openchat.app.util

import android.content.Context
import android.util.Log
import com.openchat.app.data.model.AiModel
import com.openchat.app.data.model.ApiProvider
import com.openchat.app.data.model.Message
import com.openchat.app.data.model.WorkspaceFile
import com.openchat.app.data.repository.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

enum class AgentStatus {
    IDLE, RUNNING, PAUSED, DONE, ERROR
}

data class AgentJob(
    val sessionId: String,
    val task: String,
    val job: Job,
    var status: AgentStatus = AgentStatus.RUNNING
)

@Singleton
class AgentManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workspaceRepository: WorkspaceRepository,
    private val chatRepository: ChatRepository,
    private val aiApiRepository: AiApiRepository,
    private val providerRepository: ProviderRepository
) {
    private val agentScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeAgents = mutableMapOf<String, AgentJob>()
    private val terminalExecutor = TerminalExecutor(context)

    private val _agentStatuses = MutableStateFlow<Map<String, AgentStatus>>(emptyMap())
    val agentStatuses: StateFlow<Map<String, AgentStatus>> = _agentStatuses.asStateFlow()

    fun launchAgent(
        sessionId: String,
        task: String,
        provider: ApiProvider,
        model: AiModel
    ) {
        if (activeAgents.containsKey(sessionId)) return

        val job = agentScope.launch {
            try {
                updateStatus(sessionId, AgentStatus.RUNNING)
                
                // 1. Send initial task to AI
                var currentTask = task
                var iteration = 0
                val maxIterations = 20
                var isComplete = false

                val sessionMessages = mutableListOf<Message>()
                sessionMessages.add(Message(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    role = "user",
                    content = "Please perform the following autonomous task: $currentTask",
                    timestamp = System.currentTimeMillis()
                ))

                while (iteration < maxIterations && !isComplete) {
                    iteration++
                    
                    val systemPrompt = """
                        You are an autonomous agent with access to a workspace and a terminal.
                        You can perform actions using the following formats:
                        
                        ```action:create_file
                        {"name": "file.txt", "content": "text"}
                        ```
                        
                        ```action:edit_file
                        {"name": "file.txt", "content": "new text"}
                        ```
                        
                        ```action:terminal
                        {"command": "ls -la"}
                        ```
                        
                        ```action:task_complete
                        {"summary": "Done!"}
                        ```
                        
                        After each action, you will receive results from the system.
                        Analyze the results and decide your next step.
                        Always output these blocks for actions.
                    """.trimIndent()

                    val aiResponse = aiApiRepository.sendSimpleMessage(
                        provider = provider,
                        model = model,
                        systemPrompt = systemPrompt,
                        userPrompt = sessionMessages.last().content
                    ) ?: break

                    // Save AI response to history
                    sessionMessages.add(Message(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        role = "assistant",
                        content = aiResponse,
                        timestamp = System.currentTimeMillis()
                    ))

                    // Parse actions
                    val actions = ActionParser.parseActions(aiResponse)
                    if (actions.isEmpty()) {
                        // AI didn't provide an action? Ask it to be more specific or just wait
                        sessionMessages.add(Message(
                            id = UUID.randomUUID().toString(),
                            sessionId = sessionId,
                            role = "user",
                            content = "You didn't specify an action. Please use the requested blocks to proceed or mark as complete.",
                            timestamp = System.currentTimeMillis()
                        ))
                        continue
                    }

                    val results = mutableListOf<String>()
                    for (action in actions) {
                        when (action) {
                            is AgentAction.CreateFile -> {
                                val workingDir = File(context.filesDir, "workspaces/$sessionId")
                                val file = WorkspaceFile(
                                    id = UUID.randomUUID().toString(),
                                    sessionId = sessionId,
                                    fileName = action.name,
                                    filePath = File(workingDir, action.name).absolutePath,
                                    content = action.content,
                                    isFolder = false,
                                    parentId = null,
                                    createdAt = System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis()
                                )
                                workspaceRepository.insertFile(file)
                                results.add("File ${action.name} created successfully.")
                            }
                            is AgentAction.EditFile -> {
                                // Find file by name in session
                                val files = workspaceRepository.getFilesBySessionId(sessionId).first()
                                val existingFile = files.find { it.fileName == action.name }
                                if (existingFile != null) {
                                    workspaceRepository.updateFileContent(existingFile.id, action.content)
                                    results.add("File ${action.name} updated.")
                                } else {
                                    results.add("Error: File ${action.name} not found.")
                                }
                            }
                            is AgentAction.Terminal -> {
                                val workingDir = File(context.filesDir, "workspaces/$sessionId")
                                val output = terminalExecutor.execute(action.command, workingDir)
                                results.add("Terminal output for '${action.command}':\n$output")
                            }
                            is AgentAction.TaskComplete -> {
                                isComplete = true
                                results.add("Task complete marked by AI: ${action.summary}")
                            }
                            is AgentAction.Error -> {
                                results.add("Error parsing action: ${action.message}")
                            }
                        }
                    }

                    // Feed results back to AI
                    sessionMessages.add(Message(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        role = "user",
                        content = "Results:\n" + results.joinToString("\n---\n"),
                        timestamp = System.currentTimeMillis()
                    ))
                }

                updateStatus(sessionId, if (isComplete) AgentStatus.DONE else AgentStatus.IDLE)
            } catch (e: Exception) {
                Log.e("AgentManager", "Agent error", e)
                updateStatus(sessionId, AgentStatus.ERROR)
            } finally {
                activeAgents.remove(sessionId)
            }
        }

        activeAgents[sessionId] = AgentJob(sessionId, task, job)
    }

    fun stopAgent(sessionId: String) {
        activeAgents[sessionId]?.job?.cancel()
        activeAgents.remove(sessionId)
        updateStatus(sessionId, AgentStatus.IDLE)
    }

    private fun updateStatus(sessionId: String, status: AgentStatus) {
        _agentStatuses.value = _agentStatuses.value + (sessionId to status)
    }
}
