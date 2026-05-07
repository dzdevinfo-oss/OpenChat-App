package com.openchat.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openchat.app.ui.viewmodels.ChatViewModel
import com.openchat.app.ui.theme.Teal500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPickerBottomSheet(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val availableModels by viewModel.availableModels.collectAsState()
    val activeProviders by viewModel.activeProviders.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.85f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("MODELS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                placeholder = { Text("Search models") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                shape = RoundedCornerShape(12.dp)
            )

            val groupedModels = availableModels
                .filter { it.displayName.contains(searchQuery, ignoreCase = true) || it.modelId.contains(searchQuery, ignoreCase = true) }
                .groupBy { model ->
                    val providerInfo = activeProviders.find { it.id == model.providerId }?.name ?: "Unknown Provider"
                    if (model.isBuiltIn) providerInfo else "CUSTOM MODELS"
                }

            LazyColumn(modifier = Modifier.weight(1f)) {
                groupedModels.forEach { (providerName, models) ->
                    item {
                        Text(
                            text = providerName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Teal500,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }
                    items(models) { model ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    if (selectedModel?.id == model.id) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(model.displayName, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Text("${activeProviders.find { it.id == model.providerId }?.name ?: "Unknown"} • ${model.modelId}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            RadioButton(
                                selected = selectedModel?.id == model.id,
                                onClick = {
                                    viewModel.selectModel(model)
                                    onDismiss()
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Teal500)
                            )
                        }
                    }
                }
            }
        }
    }
}
