package com.openchat.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openchat.app.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val globalSystemPrompt by viewModel.globalSystemPrompt.collectAsState()
    val isMemoryEnabled by viewModel.isMemoryEnabled.collectAsState()
    val memories by viewModel.memories.collectAsState()
    
    var promptInput by remember(globalSystemPrompt) { mutableStateOf(globalSystemPrompt) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Global Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))

            // AI Memory Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("AI Memory", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "Allow AI to remember facts about you across sessions",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isMemoryEnabled,
                    onCheckedChange = { viewModel.toggleMemory(it) }
                )
            }

            if (isMemoryEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Stored Memories", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (memories.isNotEmpty()) {
                                TextButton(onClick = { viewModel.clearAllMemories() }) {
                                    Text("Clear All", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            }
                        }
                        
                        if (memories.isEmpty()) {
                            Text("No memories stored yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            memories.forEach { memory ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "• ${memory.content}",
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f),
                                        lineHeight = 16.sp
                                    )
                                    IconButton(onClick = { viewModel.deleteMemory(memory.id) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text("Voice & Audio", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))

            val autoRead by viewModel.autoRead.collectAsState()
            val ttsSpeed by viewModel.ttsSpeed.collectAsState()
            val ttsPitch by viewModel.ttsPitch.collectAsState()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-read responses", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text("Speak AI responses aloud automatically", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = autoRead, onCheckedChange = { viewModel.setAutoRead(it) })
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("TTS Speed: ${String.format("%.1f", ttsSpeed)}x", fontSize = 14.sp)
            Slider(
                value = ttsSpeed,
                onValueChange = { viewModel.setTtsSpeed(it) },
                valueRange = 0.5f..2.0f,
                steps = 15
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("TTS Pitch: ${String.format("%.1f", ttsPitch)}x", fontSize = 14.sp)
            Slider(
                value = ttsPitch,
                onValueChange = { viewModel.setTtsPitch(it) },
                valueRange = 0.5f..2.0f,
                steps = 15
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // Theme Settings
            val theme by viewModel.theme.collectAsState()
            val fontSize by viewModel.fontSize.collectAsState()
            val bubbleStyle by viewModel.bubbleStyle.collectAsState()
            val showTimestamps by viewModel.showTimestamps.collectAsState()
            
            var expandedThemeMenu by remember { mutableStateOf(false) }
            val themes = listOf("system", "light", "dark")
            val themeLabels = mapOf("system" to "System Default", "light" to "Light", "dark" to "Dark")

            Text("Appearance", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            
            ExposedDropdownMenuBox(
                expanded = expandedThemeMenu,
                onExpandedChange = { expandedThemeMenu = !expandedThemeMenu }
            ) {
                OutlinedTextField(
                    value = themeLabels[theme] ?: "System Default",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Theme") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedThemeMenu) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expandedThemeMenu,
                    onDismissRequest = { expandedThemeMenu = false }
                ) {
                    themes.forEach { themeOption ->
                        DropdownMenuItem(
                            text = { Text(themeLabels[themeOption] ?: "") },
                            onClick = {
                                viewModel.setTheme(themeOption)
                                expandedThemeMenu = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("Font Size: ${fontSize}sp")
            Slider(value = fontSize.toFloat(), onValueChange = { viewModel.setFontSize(it.toInt()) }, valueRange = 12f..20f, steps = 8)

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Show Timestamps")
                Spacer(modifier = Modifier.weight(1f))
                Switch(checked = showTimestamps, onCheckedChange = { viewModel.setShowTimestamps(it) })
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // AI Behavior
            val temperature by viewModel.temperature.collectAsState()
            val maxTokens by viewModel.maxTokens.collectAsState()
            val streamingEnabled by viewModel.streamingEnabled.collectAsState()
            val extendedThinking by viewModel.extendedThinking.collectAsState()
            val thinkingBudget by viewModel.thinkingBudget.collectAsState()

            Spacer(modifier = Modifier.height(16.dp))
            Text("AI Behavior", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            Text("Temperature: ${String.format("%.1f", temperature)}")
            Slider(value = temperature, onValueChange = { viewModel.setTemperature(it) }, valueRange = 0f..2f, steps = 20)
            
            OutlinedTextField(value = maxTokens.toString(), onValueChange = { viewModel.setMaxTokens(it.toIntOrNull() ?: 8192) }, label = { Text("Max Tokens") })
            
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Streaming")
                Spacer(modifier = Modifier.weight(1f))
                Switch(checked = streamingEnabled, onCheckedChange = { viewModel.setStreamingEnabled(it) })
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Extended Thinking")
                Spacer(modifier = Modifier.weight(1f))
                Switch(checked = extendedThinking, onCheckedChange = { viewModel.setExtendedThinking(it) })
            }
            if (extendedThinking) {
                 Text("Thinking Budget: $thinkingBudget")
                 Slider(value = thinkingBudget.toFloat(), onValueChange = { viewModel.setThinkingBudget(it.toInt()) }, valueRange = 1000f..50000f)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Applies to all new sessions unless overridden. Instructions for how the AI should behave.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                placeholder = { Text("e.g. You are a helpful assistant...") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.updateSystemPrompt(promptInput)
                    scope.launch { snackbarHostState.showSnackbar("System prompt saved") }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Save Prompt")
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // Workspace
            val autoInjectWorkspace by viewModel.autoInjectWorkspace.collectAsState()
            val maxContextFiles by viewModel.maxContextFiles.collectAsState()
            val autoSaveArtifacts by viewModel.autoSaveArtifacts.collectAsState()

            Spacer(modifier = Modifier.height(16.dp))
            Text("Workspace", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Auto-Inject Context")
                Spacer(modifier = Modifier.weight(1f))
                Switch(checked = autoInjectWorkspace, onCheckedChange = { viewModel.setAutoInjectWorkspace(it) })
            }
            OutlinedTextField(value = maxContextFiles.toString(), onValueChange = { viewModel.setMaxContextFiles(it.toIntOrNull() ?: 5) }, label = { Text("Max Context Files") })
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Auto-Save Artifacts")
                Spacer(modifier = Modifier.weight(1f))
                Switch(checked = autoSaveArtifacts, onCheckedChange = { viewModel.setAutoSaveArtifacts(it) })
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear All Chat History")
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Clear All History?") },
            text = { Text("This will permanently delete all chat sessions and messages. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory {
                            showDeleteConfirm = false
                            scope.launch { snackbarHostState.showSnackbar("All history cleared") }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
