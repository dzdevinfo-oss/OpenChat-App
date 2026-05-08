package com.openchat.app.util

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

data class FileContent(val name: String, val content: String, val type: String)

@Singleton
class FileProcessor @Inject constructor() {

    fun processFile(context: Context, uri: Uri): FileContent? {
        val fileName = getFileName(context, uri) ?: "unknown_file"
        val extension = fileName.substringAfterLast(".", "").lowercase()
        
        return try {
            val content = when (extension) {
                "pdf" -> extractTextFromPdf(context, uri)
                "txt", "md", "json", "py", "js", "ts", "kt", "java", "html", "css", "xml", "yaml", "yml" -> readTextFile(context, uri)
                "csv" -> readCsvAsMarkdown(context, uri)
                else -> "[Binary or Unsupported File Content]"
            }
            FileContent(fileName, content, extension)
        } catch (e: Exception) {
            FileContent(fileName, "Error reading file: ${e.message}", extension)
        }
    }

    private fun readTextFile(context: Context, uri: Uri): String {
        val stringBuilder = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    stringBuilder.append(line).append("\n")
                }
            }
        }
        return stringBuilder.toString()
    }

    private fun extractTextFromPdf(context: Context, uri: Uri): String {
        // PdfRenderer doesn't extract text directly, it renders to bitmap.
        // True text extraction on Android usually requires iText or PDFBox.
        // We'll return a placeholder or use a very basic method if available.
        return "[PDF Content Extraction requires specialized library like iText or PDFBox. Filename: ${getFileName(context, uri)}]"
    }

    private fun readCsvAsMarkdown(context: Context, uri: Uri): String {
        val sb = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String?
                var isFirst = true
                while (reader.readLine().also { line = it } != null) {
                    val parts = line!!.split(",")
                    sb.append("| ").append(parts.joinToString(" | ")).append(" |\n")
                    if (isFirst) {
                        sb.append("| ").append(parts.joinToString(" | ") { "---" }).append(" |\n")
                        isFirst = false
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = it.getString(index)
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }
}
