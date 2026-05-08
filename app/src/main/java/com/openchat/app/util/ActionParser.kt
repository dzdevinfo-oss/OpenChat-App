package com.openchat.app.util

import com.google.gson.Gson
import org.json.JSONObject

sealed class AgentAction {
    data class CreateFile(val name: String, val content: String) : AgentAction()
    data class EditFile(val name: String, val content: String) : AgentAction()
    data class Terminal(val command: String) : AgentAction()
    data class TaskComplete(val summary: String) : AgentAction()
    data class Error(val message: String) : AgentAction()
}

object ActionParser {
    private val gson = Gson()

    fun parseActions(text: String): List<AgentAction> {
        val actions = mutableListOf<AgentAction>()
        
        // Find blocks like ```action:type ... ```
        val regex = "```action:([\\w_]+)\\n([\\s\\S]*?)```".toRegex()
        val matches = regex.findAll(text)
        
        matches.forEach { match ->
            val type = match.groups[1]?.value ?: ""
            val jsonStr = match.groups[2]?.value ?: ""
            
            try {
                val json = JSONObject(jsonStr)
                when (type) {
                    "create_file" -> {
                        actions.add(AgentAction.CreateFile(
                            name = json.optString("name", "untitled"),
                            content = json.optString("content", "")
                        ))
                    }
                    "edit_file" -> {
                        actions.add(AgentAction.EditFile(
                            name = json.optString("name", "untitled"),
                            content = json.optString("content", "")
                        ))
                    }
                    "terminal" -> {
                        actions.add(AgentAction.Terminal(
                            command = json.optString("command", "")
                        ))
                    }
                    "task_complete" -> {
                        actions.add(AgentAction.TaskComplete(
                            summary = json.optString("summary", "Task finished.")
                        ))
                    }
                }
            } catch (e: Exception) {
                actions.add(AgentAction.Error("Failed to parse action $type: ${e.message}"))
            }
        }
        
        return actions
    }
}
