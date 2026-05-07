package com.openchat.app.ui.screens

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.openchat.app.data.model.WorkspaceFile
import kotlinx.coroutines.delay

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FileEditorScreen(
    file: WorkspaceFile,
    onContentChanged: (String) -> Unit
) {
    var webView: WebView? by remember { mutableStateOf(null) }
    var isReady by remember { mutableStateOf(false) }

    // Map fileType to monaco language
    val language = when(file.fileType.lowercase()) {
        "kt", "kotlin" -> "kotlin"
        "js", "javascript" -> "javascript"
        "ts", "typescript" -> "typescript"
        "html" -> "html"
        "css" -> "css"
        "json" -> "json"
        "py", "python" -> "python"
        "java" -> "java"
        "cpp", "c" -> "cpp"
        "md", "markdown" -> "markdown"
        "xml" -> "xml"
        else -> "plaintext"
    }

    LaunchedEffect(file.content, isReady) {
        if (isReady && webView != null) {
            val contentEscaped = file.content
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
            webView?.evaluateJavascript("updateContent(\"$contentEscaped\", \"$language\");", null)
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccessFromFileURLs = true
                settings.allowUniversalAccessFromFileURLs = true
                
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // It's possible onEditorReady is called before onPageFinished
                    }
                }

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onEditorReady() {
                        isReady = true
                    }
                    @JavascriptInterface
                    fun onContentChanged(newContent: String) {
                        onContentChanged(newContent)
                    }
                }, "AndroidInterface")

                loadUrl("file:///android_asset/editor.html")
            }
        },
        update = {
            webView = it
        }
    )
}
