package com.openchat.app.ui.screens

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openchat.app.data.model.Message
import com.openchat.app.data.model.Session
import com.openchat.app.ui.viewmodels.ChatViewModel
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val currentSession by viewModel.currentSession.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    
    var showModelPicker by remember { mutableStateOf(false) }
    var showArtifact by remember { mutableStateOf(false) }
    var artifactCode by remember { mutableStateOf("") }
    var artifactLanguage by remember { mutableStateOf("") }

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
                        IconButton(onClick = { 
                            if (currentSession != null) onNavigateToWorkspace(currentSession!!.id) 
                        }) {
                            Icon(Icons.Default.Code, contentDescription = "Workspace / Artifacts")
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
                    onSend = { content -> viewModel.sendMessage(content, emptyList()) },
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
                                onCopy = { /* TODO Use ClipboardManager */ },
                                onDelete = { viewModel.deleteMessage(message.id) },
                                onEdit = { newContent -> viewModel.editAndResend(message.id, newContent) },
                                onRegenerate = { viewModel.regenerateLastResponse() },
                                onSpeak = { text -> viewModel.speak(text) },
                                onViewArtifact = { code, lang -> 
                                    artifactCode = code
                                    artifactLanguage = lang
                                    showArtifact = true
                                }
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

    if (showArtifact) {
        com.openchat.app.ui.components.ArtifactBottomSheet(
            code = artifactCode,
            language = artifactLanguage,
            onDismiss = { showArtifact = false }
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
    onSend: (String) -> Unit,
    onStop: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    
    val sttLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (spokenText != null) {
                text += (if (text.isEmpty()) "" else " ") + spokenText
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.Bottom
        ) {
            val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
            ) { uris ->
                // Basic URI handling structure - full multimodal parsing would need more work
                // But we can store them in a state variable to be sent alongside the message
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
                placeholder = { Text("Message...") },
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
            } else if (text.isNotBlank()) {
                IconButton(
                    onClick = { 
                        onSend(text)
                        text = ""
                    },
                    modifier = Modifier
                        .background(Teal500, CircleShape)
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Send", tint = Color.White)
                }
            } else {
                IconButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        }
                        sttLauncher.launch(intent)
                    },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Speech to text", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
    }
}
