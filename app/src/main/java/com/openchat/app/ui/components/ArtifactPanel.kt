package com.openchat.app.ui.components

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.openchat.app.util.Artifact
import com.openchat.app.util.ArtifactType

@Composable
fun ArtifactPanel(
    isOpen: Boolean,
    artifact: Artifact?,
    onClose: () -> Unit,
    onFullScreen: (Artifact) -> Unit
) {
    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it })
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.85f)
                .background(MaterialTheme.colorScheme.surface),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = artifact?.title ?: "Artifact",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    Row {
                        IconButton(onClick = { /* Share logic */ }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                        IconButton(onClick = { artifact?.let { onFullScreen(it) } }) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Full Screen")
                        }
                    }
                }
                
                HorizontalDivider()
                
                // Content
                Box(modifier = Modifier.weight(1f)) {
                    if (artifact != null) {
                        ArtifactRenderer(artifact)
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No artifact selected")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtifactRenderer(artifact: Artifact) {
    val htmlContent = remember(artifact) {
        when (artifact.type) {
            ArtifactType.HTML -> artifact.content
            ArtifactType.SVG -> """
                <html>
                <body style="margin:0;display:flex;justify-content:center;align-items:center;height:100vh;background:#f0f0f0;">
                    ${artifact.content}
                </body>
                </html>
            """.trimIndent()
            ArtifactType.REACT -> """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8" />
                    <script src="https://unpkg.com/react@18/umd/react.development.js"></script>
                    <script src="https://unpkg.com/react-dom@18/umd/react-dom.development.js"></script>
                    <script src="https://unpkg.com/@babel/standalone/babel.min.js"></script>
                    <script src="https://cdn.tailwindcss.com"></script>
                </head>
                <body>
                    <div id="root"></div>
                    <script type="text/babel">
                        ${artifact.content.let { 
                            if (it.contains("export default")) {
                                it.replace("export default", "const App =") + "\nconst root = ReactDOM.createRoot(document.getElementById('root'));\nroot.render(<App />);"
                            } else {
                                it + "\nconst root = ReactDOM.createRoot(document.getElementById('root'));\nroot.render(<App />);"
                            }
                        }}
                    </script>
                </body>
                </html>
            """.trimIndent()
            else -> "<html><body><pre>${artifact.content}</pre></body></html>"
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://localhost", htmlContent, "text/html", "UTF-8", null)
        }
    )
}
