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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import com.openchat.app.util.TerminalExecutor
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openchat.app.data.model.WorkspaceFile
import com.openchat.app.ui.theme.Teal500
import com.openchat.app.ui.viewmodels.WorkspaceViewModel
import kotlinx.coroutines.launch

@Composable
fun FileTreeItem(
    file: WorkspaceFile,
    allFiles: List<WorkspaceFile>,
    level: Int,
    selectedId: String?,
    onFileClick: (WorkspaceFile) -> Unit,
    onRename: (WorkspaceFile) -> Unit,
    onDelete: (WorkspaceFile) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    val isSelected = selectedId == file.id
    var expandedMenu by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                .clickable {
                    if (file.isFolder) {
                        isExpanded = !isExpanded
                    } else {
                        onFileClick(file)
                    }
                }
                .padding(start = (16 * level + 12).dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (file.isFolder) {
                    if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder
                } else {
                    Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = file.fileName,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            Box {
                IconButton(onClick = { expandedMenu = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(14.dp))
                }
                DropdownMenu(expanded = expandedMenu, onDismissRequest = { expandedMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            onRename(file)
                            expandedMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onDelete(file)
                            expandedMenu = false
                        }
                    )
                }
            }
        }

        if (file.isFolder && isExpanded) {
            val children = allFiles.filter { it.parentId == file.id }
            children.forEach { child ->
                FileTreeItem(
                    file = child,
                    allFiles = allFiles,
                    level = level + 1,
                    selectedId = selectedId,
                    onFileClick = onFileClick,
                    onRename = onRename,
                    onDelete = onDelete
                )
            }
        }
    }
}
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
    var showTerminal by remember { mutableStateOf(false) }
    var terminalInput by remember { mutableStateOf("") }
    val terminalHistory by viewModel.terminalHistory.collectAsState()
    
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
                    var showMenu by remember { mutableStateOf(false) }
                    var showTrash by remember { mutableStateOf(false) }

                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.NoteAdd, contentDescription = "New File")
                    }
                    IconButton(onClick = { importLauncher.launch("*/*") }) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Import file")
                    }
                    IconButton(onClick = { showTerminal = !showTerminal }) {
                        Icon(if (showTerminal) Icons.Default.Terminal else Icons.Default.Terminal, contentDescription = "Toggle Terminal", tint = if (showTerminal) Teal500 else MaterialTheme.colorScheme.onSurface)
                    }
                    
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Export as ZIP") },
                                onClick = {
                                    showMenu = false
                                    val uri = viewModel.exportWorkspaceAsZip()
                                    if (uri != null) {
                                        Toast.makeText(context, "Exported: $uri", Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Recycle Bin") },
                                onClick = {
                                    showMenu = false
                                    showTrash = true
                                }
                            )
                        }
                    }

                    if (showTrash) {
                        AlertDialog(
                            onDismissRequest = { showTrash = false },
                            title = { Text("Recycle Bin") },
                            text = {
                                if (deletedFiles.isEmpty()) {
                                    Text("No deleted files.")
                                } else {
                                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                                        items(deletedFiles) { file ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(file.fileName, modifier = Modifier.weight(1f))
                                                Row {
                                                    IconButton(onClick = { viewModel.recoverFile(file.id) }) {
                                                        Icon(Icons.Default.Restore, contentDescription = "Restore", tint = Teal500)
                                                    }
                                                    IconButton(onClick = { viewModel.permanentDelete(file.id) }) {
                                                        Icon(Icons.Default.DeleteForever, contentDescription = "Delete Permanent", tint = MaterialTheme.colorScheme.error)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showTrash = false }) {
                                    Text("Close")
                                }
                            }
                        )
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
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "EXPLORER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                    Row {
                        IconButton(onClick = { showCreateDialog = true }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.NoteAdd, contentDescription = "New File", modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { viewModel.createFolder("New Folder") }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                val rootFiles = currentFiles.filter { it.parentId == null }
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(rootFiles, key = { it.id }) { file ->
                        FileTreeItem(
                            file = file,
                            allFiles = currentFiles,
                            level = 0,
                            selectedId = currentlyOpenFile?.id,
                            onFileClick = { viewModel.openFile(file) },
                            onRename = { 
                                fileToRename = it
                                fileNameInput = it.fileName
                            },
                            onDelete = { viewModel.deleteFile(it.id) }
                        )
                    }
                }
            }

            // Divider
            VerticalDivider(modifier = Modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.outlineVariant)

            // Right Pane: Editor + Terminal
            Column(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxHeight()
            ) {
                Box(modifier = Modifier.weight(if (showTerminal) 0.6f else 1f)) {
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = currentlyOpenFile!!.fileName,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (currentlyOpenFile!!.previousContent != null) {
                                        IconButton(onClick = { viewModel.undoLastEdit(currentlyOpenFile!!.id) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Undo, contentDescription = "Undo", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    IconButton(onClick = { viewModel.redoLastEdit(currentlyOpenFile!!.id) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Redo, contentDescription = "Redo", modifier = Modifier.size(16.dp))
                                    }
                                }
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // "Saved" status / Save button
                                    TextButton(onClick = { 
                                        // updateFileContent already does save, but we can add a visual confirmation
                                        Toast.makeText(context, "Saved ✓", Toast.LENGTH_SHORT).show()
                                    }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                        Text("Save", fontSize = 13.sp)
                                    }
                                    
                                    var editorMenu by remember { mutableStateOf(false) }
                                    Box {
                                        IconButton(onClick = { editorMenu = true }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                                        }
                                        DropdownMenu(expanded = editorMenu, onDismissRequest = { editorMenu = false }) {
                                            DropdownMenuItem(
                                                text = { Text("Close") },
                                                onClick = {
                                                    viewModel.closeFile()
                                                    editorMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Copy Path") },
                                                onClick = {
                                                    // TODO Copy to clipboard
                                                    editorMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Delete") },
                                                onClick = {
                                                    viewModel.deleteFile(currentlyOpenFile!!.id)
                                                    editorMenu = false
                                                }
                                            )
                                        }
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

                if (showTerminal) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.4f)
                            .background(Color(0xFF1E1E1E))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TERMINAL", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Row {
                                IconButton(onClick = { viewModel.clearTerminal() }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.ClearAll, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { showTerminal = false }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Hide", tint = Color.Gray)
                                }
                            }
                        }
                        
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    Text(
                                        text = terminalHistory,
                                        color = Color.LightGray,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                            }
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$ ", color = Teal500, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            BasicTextField(
                                value = terminalInput,
                                onValueChange = { terminalInput = it },
                                modifier = Modifier.weight(1f).padding(start = 4.dp),
                                textStyle = LocalTextStyle.current.copy(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                cursorBrush = SolidColor(Color.White),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = {
                                    if (terminalInput.isNotBlank()) {
                                        viewModel.executeCommand(terminalInput)
                                        terminalInput = ""
                                    }
                                })
                            )
                        }
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
