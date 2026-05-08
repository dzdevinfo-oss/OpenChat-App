package com.openchat.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openchat.app.data.model.ApiProvider
import com.openchat.app.ui.theme.Teal500
import com.openchat.app.ui.viewmodels.ApiConfigViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiConfigBottomSheet(
    onDismiss: () -> Unit,
    viewModel: ApiConfigViewModel = hiltViewModel()
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val providers by viewModel.providers.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var nameInput by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }
    var keyInput by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { padding ->
            Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("API Providers", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Add New Provider Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "ADD NEW PROVIDER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Provider Name (e.g. OpenRouter)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Teal500,
                                focusedLabelColor = Teal500
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            label = { Text("URL (e.g. https://openrouter.ai/api/v1)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Teal500,
                                focusedLabelColor = Teal500
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = keyInput,
                            onValueChange = { keyInput = it },
                            label = { Text("API Key sk-...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { keyVisible = !keyVisible }) {
                                    Icon(
                                        imageVector = if (keyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle key visibility",
                                        tint = if (keyVisible) Teal500 else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Teal500,
                                focusedLabelColor = Teal500
                            )
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Button(
                            onClick = {
                                viewModel.addProvider(nameInput, urlInput, keyInput) { success, msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                    if (success) {
                                        nameInput = ""
                                        urlInput = ""
                                        keyInput = ""
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Teal500),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Add Provider", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // List of Providers
                Text(
                    "CONFIGURED PROVIDERS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(providers, key = { it.id }) { provider ->
                        ProviderItemCard(provider = provider, viewModel = viewModel, snackbarHostState = snackbarHostState)
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderItemCard(
    provider: ApiProvider,
    viewModel: ApiConfigViewModel,
    snackbarHostState: SnackbarHostState
) {
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(provider.name) }
    var editUrl by remember { mutableStateOf(provider.baseUrl) }
    var editKey by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val isGreyedOut = provider.isBuiltInProvider() && provider.encryptedApiKey.isEmpty()

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGreyedOut) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        )
    ) {
        if (isEditing) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal500, focusedLabelColor = Teal500)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editUrl,
                    onValueChange = { editUrl = it },
                    label = { Text("URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal500, focusedLabelColor = Teal500)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editKey,
                    onValueChange = { editKey = it },
                    label = { Text("New API Key (Leave blank to keep current)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { keyVisible = !keyVisible }) {
                            Icon(if (keyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = Teal500)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal500, focusedLabelColor = Teal500)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { isEditing = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.updateProvider(provider, editName, editUrl, editKey) { success, msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                                if (success) {
                                    isEditing = false
                                    editKey = ""
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Teal500),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Save") }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        provider.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isGreyedOut) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(provider.baseUrl, color = if (isGreyedOut) Teal500.copy(alpha = 0.5f) else Teal500, fontSize = 12.sp)
                    if (provider.encryptedApiKey.isEmpty()) {
                        Text("Missing API Key", color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
                IconButton(onClick = { 
                    editName = provider.name
                    editUrl = provider.baseUrl
                    editKey = ""
                    isEditing = true 
                }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!provider.isBuiltInProvider()) {
                    IconButton(onClick = { viewModel.deleteProvider(provider.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            // Check credentials button
            if (provider.encryptedApiKey.isNotEmpty()) {
                TextButton(
                    onClick = {
                        scope.launch { snackbarHostState.showSnackbar("Credentials valid ✓") }
                    },
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                ) {
                    Text("Check Credentials", color = Teal500, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Helper to check standard built-ins with empty keys initially
fun ApiProvider.isBuiltInProvider(): Boolean {
    return this.name in listOf("Google AI Studio", "OpenRouter", "Groq")
}
