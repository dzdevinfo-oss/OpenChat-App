package com.openchat.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openchat.app.util.ArtifactDetector
import com.openchat.app.util.ArtifactType
import com.openchat.app.util.CodeExecutionManager
import com.openchat.app.util.TerminalExecutor
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.TextView

@Composable
fun MessageItem(
    message: Message,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (String) -> Unit,
    onRegenerate: () -> Unit,
    onSpeak: (String) -> Unit,
    onViewArtifact: ((String, String) -> Unit)? = null,
    onSaveToWorkspace: ((String, String) -> Unit)? = null,
    terminalExecutor: TerminalExecutor? = null
) {
    val context = LocalContext.current
    val executionManager = remember { terminalExecutor?.let { CodeExecutionManager(context, it) } }
    val isUser = message.role == "user"
    var expandedMenu by remember { mutableStateOf(false) }
    
    // Parse artifact
    var artifactCode: String? = null
    var artifactLanguage: String? = null
    
    if (!isUser) {
        val artifactRegex = "(?s)```(html|svg|react|jsx|tsx)(.*?)```".toRegex()
        val match = artifactRegex.find(message.content)
        if (match != null) {
            artifactLanguage = match.groupValues[1]
            artifactCode = match.groupValues[2].trim()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = "AI", tint = Teal500, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Box(modifier = Modifier.weight(1f, fill = false)) {
            Column {
                // Thinking block
                if (!message.thinkingContent.isNullOrBlank()) {
                    var thinkingExpanded by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(bottom = 8.dp)
                            .clickable { thinkingExpanded = !thinkingExpanded },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Thinking Process", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(if (thinkingExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            AnimatedVisibility(visible = thinkingExpanded) {
                                Text(
                                    text = message.thinkingContent,
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        ))
                        .background(if (isUser) Teal500 else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { expandedMenu = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (isUser) {
                        Text(text = message.content, color = Color.White, fontSize = 16.sp)
                    } else {
                        MessageContent(
                            content = message.content + if (message.isStreaming) " █" else "",
                            onRunCode = { lang, code -> executionManager?.execute(lang, code) },
                            onSaveCode = { lang, code -> onSaveToWorkspace?.invoke(lang, code) },
                            executionManager = executionManager
                        )
                    }
                }

                if (artifactCode != null && artifactLanguage != null && onViewArtifact != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onViewArtifact(artifactCode, artifactLanguage) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View ${artifactLanguage.uppercase()} Artifact")
                    }
                }

                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false }
                ) {
                    DropdownMenuItem(text = { Text("Copy") }, onClick = { onCopy(); expandedMenu = false }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) })
                    if (!isUser) {
                        DropdownMenuItem(text = { Text("Read Aloud") }, onClick = { onSpeak(message.content); expandedMenu = false }, leadingIcon = { Icon(Icons.Default.PlayArrow, null) })
                    }
                    if (isUser) {
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { onEdit(message.content); expandedMenu = false }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                    } else {
                        DropdownMenuItem(text = { Text("Regenerate") }, onClick = { onRegenerate(); expandedMenu = false }, leadingIcon = { Icon(Icons.Default.Refresh, null) })
                    }
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { onDelete(); expandedMenu = false }, leadingIcon = { Icon(Icons.Default.Delete, null) })
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = "User", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun MessageContent(
    content: String,
    onRunCode: (String, String) -> Unit,
    onSaveCode: (String, String) -> Unit,
    executionManager: CodeExecutionManager?
) {
    val segments = remember(content) { splitMessageContent(content) }
    
    Column {
        segments.forEach { segment ->
            when (segment) {
                is ContentSegment.Text -> {
                    MarkdownText(markdown = segment.text, color = MaterialTheme.colorScheme.onSurface)
                }
                is ContentSegment.Code -> {
                    CodeBlock(
                        language = segment.language,
                        code = segment.code,
                        onRun = onRunCode,
                        onSave = onSaveCode,
                        executionManager = executionManager
                    )
                }
            }
        }
    }
}

sealed class ContentSegment {
    data class Text(val text: String) : ContentSegment()
    data class Code(val language: String, val code: String) : ContentSegment()
}

fun splitMessageContent(content: String): List<ContentSegment> {
    val segments = mutableListOf<ContentSegment>()
    val regex = "```([\\w-]+)?\\n([\\s\\S]*?)```".toRegex()
    var lastIndex = 0
    
    regex.findAll(content).forEach { match ->
        if (match.range.first > lastIndex) {
            segments.add(ContentSegment.Text(content.substring(lastIndex, match.range.first)))
        }
        val lang = match.groups[1]?.value ?: "text"
        val code = match.groups[2]?.value ?: ""
        segments.add(ContentSegment.Code(lang, code))
        lastIndex = match.range.last + 1
    }
    
    if (lastIndex < content.length) {
        segments.add(ContentSegment.Text(content.substring(lastIndex)))
    }
    
    return if (segments.isEmpty() && content.isNotEmpty()) listOf(ContentSegment.Text(content)) else segments
}

@Composable
fun CodeBlock(
    language: String,
    code: String,
    onRun: (String, String) -> Unit,
    onSave: (String, String) -> Unit,
    executionManager: CodeExecutionManager?
) {
    val output by (executionManager?.executionOutput?.collectAsState() ?: mutableStateOf(""))
    val isExecuting by (executionManager?.isExecuting?.collectAsState() ?: mutableStateOf(false))

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF333333))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(language.uppercase(), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = { onRun(language, code) }, modifier = Modifier.size(24.dp)) {
                        Icon(if (isExecuting) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = "Run", tint = Teal500, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { onSave(language, code) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Save, contentDescription = "Save", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    }
                }
            }
            
            // Code
            Text(
                text = code,
                color = Color.LightGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(12.dp),
                fontStyle = FontStyle.Normal
            )
            
            // Output
            if (output.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(8.dp)
                ) {
                    Text(text = output, color = Color(0xFF00FF00), fontSize = 11.sp)
                }
            }
        }
    }
}
    AndroidView(
        factory = { context ->
            TextView(context).apply {
                setTextColor(android.graphics.Color.argb(
                    color.alpha,
                    color.red,
                    color.green,
                    color.blue
                ))
                textSize = 16f
            }
        },
        update = { textView ->
            val markwon = Markwon.builder(textView.context)
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TablePlugin.create(textView.context))
                .usePlugin(HtmlPlugin.create())
                .build()
            
            markwon.setMarkdown(textView, markdown)
        }
    )
}
