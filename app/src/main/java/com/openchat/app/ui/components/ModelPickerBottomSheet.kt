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
                .let { filtered ->
                    val builtIns = filtered.filter { it.isBuiltIn }.groupBy { model ->
                        activeProviders.find { it.id == model.providerId }?.name ?: "Unknown Provider"
                    }
                    val customs = filtered.filter { !it.isBuiltIn }
                    
                    if (customs.isNotEmpty()) {
                        builtIns + ("CUSTOM MODELS" to customs)
                    } else {
                        builtIns
                    }
                }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                groupedModels.forEach { (groupName, models) ->
                    item {
                        Text(
                            text = groupName.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Teal500,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp)
                        )
                    }
                    items(models) { model ->
                        val provider = activeProviders.find { it.id == model.providerId }
                        val isSelected = selectedModel?.id == model.id

                        Surface(
                            onClick = {
                                viewModel.selectModel(model)
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Simple logo representation
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            if (isSelected) Teal500 else MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (provider?.name ?: "?").take(1),
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = model.displayName,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Teal500 else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${provider?.name ?: "Unknown"} • ${model.modelId}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                RadioButton(
                                    selected = isSelected,
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
}
