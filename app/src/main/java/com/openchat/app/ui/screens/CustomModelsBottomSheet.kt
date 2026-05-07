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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openchat.app.data.model.AiModel
import com.openchat.app.data.model.ApiProvider
import com.openchat.app.ui.theme.Teal500
import com.openchat.app.ui.viewmodels.CustomModelsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomModelsBottomSheet(
    onDismiss: () -> Unit,
    viewModel: CustomModelsViewModel = hiltViewModel()
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val models by viewModel.models.collectAsState()
    val customModels = models.filter { !it.isBuiltIn }
    val activeProviders by viewModel.activeProviders.collectAsState()
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var modelIdInput by remember { mutableStateOf("") }
    var displayNameInput by remember { mutableStateOf("") }
    var selectedProviderId by remember { mutableStateOf("") }
    var providerDropdownExpanded by remember { mutableStateOf(false) }
    
    var censorMode by remember { mutableStateOf("Default") }
    var censorDropdownExpanded by remember { mutableStateOf(false) }
    val censorOptions = listOf("Default", "Uncensored", "Safe")

    LaunchedEffect(activeProviders) {
        if (selectedProviderId.isBlank() && activeProviders.isNotEmpty()) {
            selectedProviderId = activeProviders.first().id
        }
    }

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
                    Text("Custom Models", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Add Custom Model Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("ADD CUSTOM MODEL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = modelIdInput,
                            onValueChange = { modelIdInput = it },
                            label = { Text("Model ID (e.g. google/gemini-2.5-flash)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = displayNameInput,
                            onValueChange = { displayNameInput = it },
                            label = { Text("Display Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Provider Dropdown
                        ExposedDropdownMenuBox(
                            expanded = providerDropdownExpanded,
                            onExpandedChange = { providerDropdownExpanded = it }
                        ) {
                            val selectedProvName = activeProviders.find { it.id == selectedProviderId }?.name ?: "Select Provider"
                            OutlinedTextField(
                                value = selectedProvName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Provider API") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerDropdownExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = providerDropdownExpanded,
                                onDismissRequest = { providerDropdownExpanded = false }
                            ) {
                                activeProviders.forEach { provider ->
                                    DropdownMenuItem(
                                        text = { Text(provider.name) },
                                        onClick = {
                                            selectedProviderId = provider.id
                                            providerDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Censor Mode Dropdown
                        ExposedDropdownMenuBox(
                            expanded = censorDropdownExpanded,
                            onExpandedChange = { censorDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = censorMode,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Assign to Censored Mode") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = censorDropdownExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = censorDropdownExpanded,
                                onDismissRequest = { censorDropdownExpanded = false }
                            ) {
                                censorOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            censorMode = option
                                            censorDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                viewModel.addModel(modelIdInput, displayNameInput, selectedProviderId, censorMode.lowercase()) { success, msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                    if (success) {
                                        modelIdInput = ""
                                        displayNameInput = ""
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Teal500),
                            shape = RoundedCornerShape(12.dp),
                            enabled = activeProviders.isNotEmpty()
                        ) {
                            Text("Add Model", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (activeProviders.isEmpty()) {
                            Text("Please add an API Provider first.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }

                // List of Custom Models
                Text("ADDED CUSTOM MODELS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(customModels, key = { it.id }) { model ->
                        CustomModelItemCard(model = model, activeProviders = activeProviders, viewModel = viewModel, snackbarHostState = snackbarHostState)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomModelItemCard(
    model: AiModel,
    activeProviders: List<ApiProvider>,
    viewModel: CustomModelsViewModel,
    snackbarHostState: SnackbarHostState
) {
    var isEditing by remember { mutableStateOf(false) }
    var editModelId by remember { mutableStateOf(model.modelId) }
    var editDisplayName by remember { mutableStateOf(model.displayName) }
    var editProviderId by remember { mutableStateOf(model.providerId) }
    var editCensorMode by remember { mutableStateOf(model.censorMode.replaceFirstChar { it.uppercase() }) }
    
    var providerDropdown by remember { mutableStateOf(false) }
    var censorDropdown by remember { mutableStateOf(false) }
    val censorOptions = listOf("Default", "Uncensored", "Safe")
    
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        if (isEditing) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(value = editModelId, onValueChange = { editModelId = it }, label = { Text("Model ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = editDisplayName, onValueChange = { editDisplayName = it }, label = { Text("Display Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                
                ExposedDropdownMenuBox(expanded = providerDropdown, onExpandedChange = { providerDropdown = it }) {
                    val provName = activeProviders.find { it.id == editProviderId }?.name ?: "Select Provider"
                    OutlinedTextField(
                        value = provName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Provider API") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerDropdown) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = providerDropdown, onDismissRequest = { providerDropdown = false }) {
                        activeProviders.forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider.name) },
                                onClick = { editProviderId = provider.id; providerDropdown = false }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                ExposedDropdownMenuBox(expanded = censorDropdown, onExpandedChange = { censorDropdown = it }) {
                    OutlinedTextField(
                        value = editCensorMode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Censored Mode") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = censorDropdown) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = censorDropdown, onDismissRequest = { censorDropdown = false }) {
                        censorOptions.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = { editCensorMode = option; censorDropdown = false })
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { isEditing = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.updateModel(model, editModelId, editDisplayName, editProviderId, editCensorMode.lowercase()) { success, msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                                if (success) isEditing = false
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
                    Text(model.modelId, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(model.displayName, color = Teal500, fontSize = 12.sp)
                }
                IconButton(onClick = { 
                    editModelId = model.modelId
                    editDisplayName = model.displayName
                    editProviderId = model.providerId
                    editCensorMode = model.censorMode.replaceFirstChar { it.uppercase() }
                    isEditing = true 
                }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { viewModel.deleteModel(model.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
