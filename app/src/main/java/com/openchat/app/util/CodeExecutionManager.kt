package com.openchat.app.util

import android.content.Context
import android.webkit.WebView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class CodeExecutionManager(private val context: Context, private val terminalExecutor: TerminalExecutor) {
    
    private val _executionOutput = MutableStateFlow<String>("")
    val executionOutput = _executionOutput.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting = _isExecuting.asStateFlow()

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)

    fun execute(language: String, code: String, workingDir: File? = null) {
        _isExecuting.value = true
        _executionOutput.value = "Starting execution...\n"
        
        when (language.lowercase()) {
            "javascript", "js" -> executeJs(code)
            "python", "py", "bash", "sh" -> {
                scope.launch {
                    executeTerminal(language, code, workingDir)
                }
            }
            else -> {
                _executionOutput.value = "Language $language is not supported for execution."
                _isExecuting.value = false
            }
        }
    }

    private fun executeJs(code: String) {
        // Run JS in a invisible webview
        val webView = WebView(context)
        webView.settings.javaScriptEnabled = true
        webView.evaluateJavascript("(function() { try { return eval(`${code.replace("`", "\\`").replace("$", "\\$")}`); } catch(e) { return 'Error: ' + e.message; } })()") { result ->
            _executionOutput.value = result.trim('"')
            _isExecuting.value = false
        }
    }

    private suspend fun executeTerminal(language: String, code: String, workingDir: File?) {
        val cmd = if (language.lowercase().contains("python")) {
            "python3 -c \"${code.replace("\"", "\\\"")}\""
        } else {
            code
        }
        
        val dir = workingDir ?: context.cacheDir
        val result = terminalExecutor.execute(cmd, dir)
        _executionOutput.value = result
        _isExecuting.value = false
    }
    
    fun clearOutput() {
        _executionOutput.value = ""
    }
}
