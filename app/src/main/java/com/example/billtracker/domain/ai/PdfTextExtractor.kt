package com.example.billtracker.domain.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.zip.Inflater

object PdfTextExtractor {

    suspend fun extractTextFromPdf(context: Context, pdfFile: File): String = withContext(Dispatchers.IO) {
        if (!pdfFile.exists()) return@withContext ""

        val sb = StringBuilder()

        try {
            val bytes = FileInputStream(pdfFile).use { it.readBytes() }
            val contentStr = String(bytes, Charsets.ISO_8859_1)

            // 1. Extract text between BT (Begin Text) and ET (End Text) or inside Tj / TJ operators
            val tjRegex = Regex("""\((.*?)\)\s*Tj""", RegexOption.DOT_MATCHES_ALL)
            val matches = tjRegex.findAll(contentStr)
            for (match in matches) {
                val extracted = match.groupValues[1]
                if (extracted.isNotBlank()) {
                    sb.append(extracted).append(" ")
                }
            }

            // 2. Also search for readable character sequences
            if (sb.length < 50) {
                val wordsRegex = Regex("""[A-Za-z0-9₱$,.:/%\-]{3,}""")
                val foundWords = wordsRegex.findAll(contentStr)
                    .map { it.value }
                    .filter { !it.startsWith("obj") && !it.startsWith("endobj") && !it.startsWith("stream") && !it.startsWith("Flate") }
                    .take(150)
                    .joinToString(" ")

                if (foundWords.isNotBlank()) {
                    sb.append(foundWords)
                }
            }
        } catch (_: Exception) {}

        if (sb.length < 30) {
            sb.append("Billing Document: ${pdfFile.nameWithoutExtension} (Size: ${pdfFile.length()} bytes)")
        }

        sb.toString()
    }
}
