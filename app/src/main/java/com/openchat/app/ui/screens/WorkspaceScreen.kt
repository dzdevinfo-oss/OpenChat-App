package com.openchat.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openchat.app.data.model.WorkspaceFile
import com.openchat.app.ui.theme.Teal500
import com.openchat.app.ui.viewmodels.WorkspaceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    sessionId: String,
    onBack: () -> Unit,
    viewModel: WorkspaceViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentFiles by viewModel.currentFiles.collectAsState()
    val currentlyOpenFile by viewModel.currentlyOpenFile.collectAsState()
    val deletedFiles by viewModel.deletedFiles.collectAsState() // for trash view if needed

    LaunchedEffect(sessionId) {
        viewModel.setSessionId(sessionId)
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var fileToRename by remember { mutableStateOf<WorkspaceFile?>(null) }
    var fileNameInput by remember { mutableStateOf("") }
    
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.importFileFromDevice(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workspace") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "New File")
                    }
                    IconButton(onClick = { importLauncher.launch("*/*") }) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Import file")
                    }
                    IconButton(onClick = {
                        val uri = viewModel.exportWorkspaceAsZip()
                        if (uri != null) {
                            Toast.makeText(context, "Exported: \$uri", Toast.LENGTH_LONG).show()
                            // Ideally launch ACTION_VIEW or SEND to share the zip
                        } else {
                            Toast.makeText(context, "Export failed or empty", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Download, contentDescription = "Export to ZIP")
                    }
                }
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Left Pane: File List
            Column(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "FILES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(currentFiles, key = { it.id }) { file ->
                        val isSelected = currentlyOpenFile?.id == file.id
                        var expandedMenu by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { viewModel.openFile(file) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = file.fileName,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )

                            Box {
                                IconButton(onClick = { expandedMenu = true }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(16.dp))
                                }
                                DropdownMenu(expanded = expandedMenu, onDismissRequest = { expandedMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Rename") },
                                        onClick = {
                                            fileToRename = file
                                            fileNameInput = file.fileName
                                            expandedMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        onClick = {
                                            viewModel.deleteFile(file.id)
                                            expandedMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Divider
            VerticalDivider(modifier = Modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.outlineVariant)

            // Right Pane: Editor
            Box(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxHeight()
            ) {
                if (currentlyOpenFile != null) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = currentlyOpenFile!!.fileName,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            if (currentlyOpenFile!!.previousContent != null) {
                                TextButton(onClick = { viewModel.undoLastEdit(currentlyOpenFile!!.id) }) {
                                    Text("Undo Change")
                                }
                            }
                        }
                        HorizontalDivider()
                        
                        Box(modifier = Modifier.weight(1f)) {
                            FileEditorScreen(
                                file = currentlyOpenFile!!,
                                onContentChanged = { newContent ->
                                    viewModel.updateFileContent(currentlyOpenFile!!.id, newContent)
                                }
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select a file to view or edit", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New File") },
            text = {
                OutlinedTextField(
                    value = fileNameInput,
                    onValueChange = { fileNameInput = it },
                    label = { Text("File Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = fileNameInput.trim()
                    if (name.isNotEmpty()) {
                        val ext = name.substringAfterLast('.', "txt")
                        viewModel.createFile(name, "", ext)
                    }
                    showCreateDialog = false
                    fileNameInput = ""
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false; fileNameInput = "" }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (fileToRename != null) {
        AlertDialog(
            onDismissRequest = { fileToRename = null },
            title = { Text("Rename File") },
            text = {
                OutlinedTextField(
                    value = fileNameInput,
                    onValueChange = { fileNameInput = it },
                    label = { Text("File Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = fileNameInput.trim()
                    if (name.isNotEmpty() && fileToRename != null) {
                        viewModel.renameFile(fileToRename!!.id, name)
                    }
                    fileToRename = null
                    fileNameInput = ""
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToRename = null; fileNameInput = "" }) {
                    Text("Cancel")
                }
            }
        )
    }
}
