package com.openchat.app.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtifactBottomSheet(
    code: String,
    language: String,
    onDismiss: () -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Artifact Preview", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            HorizontalDivider()

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()
                    }
                },
                update = { webView ->
                    val htmlToLoad = buildHtmlContent(code, language)
                    webView.loadDataWithBaseURL(null, htmlToLoad, "text/html", "UTF-8", null)
                }
            )
        }
    }
}

private fun buildHtmlContent(code: String, language: String): String {
    return when (language.lowercase()) {
        "html" -> code
        "svg" -> """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style> 
                    body { margin: 0; padding: 16px; display: flex; justify-content: center; align-items: center; min-height: 100vh; background-color: #ffffff; }
                    svg { max-width: 100%; height: auto; }
                </style>
            </head>
            <body>
                $code
            </body>
            </html>
        """.trimIndent()
        "react", "jsx", "tsx" -> """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <script src="https://unpkg.com/react@18/umd/react.development.js"></script>
                <script src="https://unpkg.com/react-dom@18/umd/react-dom.development.js"></script>
                <script src="https://unpkg.com/@babel/standalone/babel.min.js"></script>
                <script src="https://cdn.tailwindcss.com"></script>
                <style> body { margin: 0; padding: 16px; font-family: sans-serif; } </style>
            </head>
            <body>
                <div id="root"></div>
                <script type="text/babel">
                    $code
                    
                    // Simple auto-render logic
                    if (typeof App !== 'undefined') {
                        const root = ReactDOM.createRoot(document.getElementById('root'));
                        root.render(<App />);
                    } else if (typeof Render !== 'undefined') {
                        const root = ReactDOM.createRoot(document.getElementById('root'));
                        root.render(<Render />);
                    } else {
                        // Fallback: try to find any React component and render it
                        // This is a naive approach, relies on component being exported or named App
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
        else -> """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style> body { margin: 0; padding: 16px; font-family: monospace; white-space: pre-wrap; background-color: #f4f4f4; } </style>
            </head>
            <body>$code</body>
            </html>
        """.trimIndent()
    }
}
