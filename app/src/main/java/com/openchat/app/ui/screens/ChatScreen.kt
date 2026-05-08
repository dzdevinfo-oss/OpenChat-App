package com.openchat.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.openchat.app.data.model.Message
import com.openchat.app.data.model.Session
import android.widget.Toast
import com.openchat.app.ui.components.ArtifactPanel
import com.openchat.app.util.Artifact
import com.openchat.app.util.ArtifactType
import kotlinx.coroutines.launch
import com.openchat.app.ui.theme.Teal500
import com.openchat.app.ui.components.MessageItem
import com.openchat.app.ui.components.ModelPickerBottomSheet
import com.openchat.app.ui.components.SidebarDrawer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionId: String,
    onNavigateToWorkspace: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToApiConfig: () -> Unit,
    onNavigateToCustomModels: () -> Unit,
    onNavigateToMemories: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val currentArtifacts by viewModel.currentArtifacts.collectAsState()
    val selectedArtifact by viewModel.selectedArtifact.collectAsState()
    val isArtifactPanelOpen by viewModel.isArtifactPanelOpen.collectAsState()
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    
    val currentSession by viewModel.currentSession.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    
    var showModelPicker by remember { mutableStateOf(false) }

    LaunchedEffect(sessionId) {
        viewModel.loadSession(sessionId)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SidebarDrawer(
                viewModel = viewModel,
                onClose = { scope.launch { drawerState.close() } },
                onNavigateToChat = { id ->
                    onNavigateToChat(id)
                    scope.launch { drawerState.close() }
                },
                onNavigateToSettings = {
                    onNavigateToSettings()
                    scope.launch { drawerState.close() }
                },
                onNavigateToApiConfig = {
                    onNavigateToApiConfig()
                    scope.launch { drawerState.close() }
                },
                onNavigateToCustomModels = {
                    onNavigateToCustomModels()
                    scope.launch { drawerState.close() }
                },
                onNavigateToMemories = {
                    onNavigateToMemories()
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showModelPicker = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = selectedModel?.displayName ?: "Select Model",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Select Model")
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        if (currentArtifacts.isNotEmpty()) {
                            IconButton(onClick = { viewModel.showArtifact(currentArtifacts.first()) }) {
                                Icon(Icons.Default.Code, contentDescription = "Artifacts", tint = Teal500)
                            }
                        }
                        IconButton(onClick = { 
                            if (currentSession != null) onNavigateToWorkspace(currentSession!!.id) 
                        }) {
                            Icon(Icons.Default.Workspaces, contentDescription = "Workspace")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                MessageInputBar(
                    isStreaming = isStreaming,
                    viewModel = viewModel,
                    onSend = { content, uris -> viewModel.sendMessage(content, uris) },
                    onStop = { viewModel.stopStreaming() }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (messages.isEmpty() && currentSession == null) {
                    EmptyChatState(
                        onSuggestionClick = { content -> viewModel.sendMessage(content, emptyList()) }
                    )
                } else {
                    val listState = rememberLazyListState()
                    LaunchedEffect(messages.size, isStreaming) {
                        if (messages.isNotEmpty() && listState.firstVisibleItemIndex == 0) {
                            listState.animateScrollToItem(0)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = true,
                        contentPadding = PaddingValues(bottom = 16.dp, top = 16.dp)
                    ) {
                        items(messages, key = { it.id }) { message ->
                            MessageItem(
                                message = message,
                                onCopy = { 
                                    val clip = ClipData.newPlainText("Chat Message", message.content)
                                    clipboardManager.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                onDelete = { viewModel.deleteMessage(message.id) },
                                onEdit = { newContent -> viewModel.editAndResend(message.id, newContent) },
                                onRegenerate = { viewModel.regenerateLastResponse() },
                                onSpeak = { text -> viewModel.speak(text) },
                                onViewArtifact = { code, lang -> 
                                    viewModel.showArtifact(Artifact(
                                        id = "temp",
                                        type = when(lang.lowercase()) {
                                            "html" -> ArtifactType.HTML
                                            "svg" -> ArtifactType.SVG
                                            "react", "jsx" -> ArtifactType.REACT
                                            else -> ArtifactType.CODE
                                        },
                                        language = lang,
                                        content = code,
                                        title = "Artifact"
                                    ))
                                },
                                onSaveToWorkspace = { lang, code -> 
                                    viewModel.saveToWorkspace(lang, code)
                                    Toast.makeText(context, "Saved to Workspace", Toast.LENGTH_SHORT).show()
                                },
                                terminalExecutor = viewModel.getTerminalExecutor()
                            )
                        }
                    }
                }
            }
        }
    }

    if (showModelPicker) {
        ModelPickerBottomSheet(
            viewModel = viewModel,
            onDismiss = { showModelPicker = false }
        )
    }
}

@Composable
fun EmptyChatState(onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "What can I help you with?",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        val suggestions = listOf(
            "Write a Python script",
            "Explain quantum computing",
            "Help me debug this code"
        )
        
        suggestions.forEach { suggestion ->
            SuggestionChip(
                onClick = { onSuggestionClick(suggestion) },
                label = { Text(suggestion) },
                modifier = Modifier.padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInputBar(
    isStreaming: Boolean,
    viewModel: ChatViewModel,
    onSend: (String, List<Uri>) -> Unit,
    onStop: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val attachedUris = remember { mutableStateListOf<Uri>() }
    val isListening by viewModel.isListening.collectAsState()
    val partialTranscription by viewModel.partialTranscription.collectAsState()
    val context = LocalContext.current

    // Update text field with partial transcription while listening
    LaunchedEffect(partialTranscription) {
        if (isListening && partialTranscription.isNotBlank()) {
            text = partialTranscription
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            // Attachment Preview
            if (attachedUris.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(attachedUris) { uri ->
                        val mimeType = context.contentResolver.getType(uri) ?: ""
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (mimeType.startsWith("image/")) {
                                androidx.compose.foundation.Image(
                                    painter = rememberAsyncImagePainter(uri),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                                Text(
                                    text = uri.lastPathSegment?.takeLast(5) ?: "...",
                                    fontSize = 10.sp,
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(2.dp)
                                )
                            }
                            
                            IconButton(
                                onClick = { attachedUris.remove(uri) },
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.GetMultipleContents()
                ) { uris ->
                    attachedUris.addAll(uris)
                }

                IconButton(onClick = { launcher.launch("*/*") }) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Attach file", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    placeholder = { 
                        Text(if (isListening) "Listening..." else "Message...") 
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    maxLines = 5
                )

                if (isStreaming) {
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop streaming", tint = MaterialTheme.colorScheme.onError)
                    }
                } else if (text.isNotBlank() || attachedUris.isNotEmpty()) {
                    IconButton(
                        onClick = { 
                            onSend(text, attachedUris.toList())
                            text = ""
                            attachedUris.clear()
                        },
                        modifier = Modifier
                            .background(Teal500, CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Send", tint = Color.White)
                    }
                } else {
                    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = if (isListening) 1.5f else 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulsescale"
                    )

                    IconButton(
                        onClick = {
                            if (isListening) {
                                viewModel.stopVoiceInput()
                            } else {
                                viewModel.startVoiceInput { result ->
                                    text = result
                                    // Optional: onSend(result, emptyList()) if we want auto-send
                                }
                            }
                        },
                        modifier = Modifier
                            .scale(if (isListening) scale else 1f)
                            .background(
                                if (isListening) Teal500.copy(alpha = 0.3f) else MaterialTheme.colorScheme.secondaryContainer, 
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicNone else Icons.Default.Mic, 
                            contentDescription = "Voice input", 
                            tint = if (isListening) Teal500 else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}
