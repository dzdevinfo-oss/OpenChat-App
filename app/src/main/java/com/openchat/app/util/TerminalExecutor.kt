package com.openchat.app.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class TerminalExecutor(private val context: Context) {
    
    suspend fun execute(command: String, workingDir: File): String = withContext(Dispatchers.IO) {
        try {
            if (!workingDir.exists()) {
                workingDir.mkdirs()
            }

            // Simple shell execution
            val processBuilder = ProcessBuilder()
            processBuilder.directory(workingDir)
            
            // On Android, we usually use /system/bin/sh
            processBuilder.command("/system/bin/sh", "-c", command)
            processBuilder.redirectErrorStream(true)

            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            
            // Read output in a separate thread or just read it
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            val finished = process.waitFor(30, TimeUnit.SECONDS)
            if (!finished) {
                process.destroy()
                return@withContext output.append("\n[Error: Command timed out after 30s]").toString()
            }

            if (output.isEmpty()) {
                return@withContext "[Executed successfully with no output]"
            }

            output.toString()
        } catch (e: Exception) {
            "[Error: ${e.message}]"
        }
    }
}
