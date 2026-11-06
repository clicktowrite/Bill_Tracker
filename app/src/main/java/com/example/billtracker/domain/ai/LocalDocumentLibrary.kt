package com.example.billtracker.domain.ai

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.billtracker.domain.model.BillingDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class LocalDocumentLibrary(
    private val context: Context
) {

    private val docsDir: File
        get() {
            val dir = File(context.filesDir, "billing_documents")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    private val _documents = MutableStateFlow<List<BillingDocument>>(emptyList())
    val documents: StateFlow<List<BillingDocument>> = _documents.asStateFlow()

    init {
        scanDocuments()
    }

    fun scanDocuments() {
        val files = docsDir.listFiles { f -> f.extension.equals("pdf", ignoreCase = true) } ?: emptyArray()
        val list = files.mapIndexed { index, file ->
            BillingDocument(
                id = index.toLong() + 1,
                tenantId = null,
                title = file.nameWithoutExtension,
                fileName = file.name,
                filePath = file.absolutePath,
                fileSizeBytes = file.length(),
                dateImported = file.lastModified(),
                isIndexed = true
            )
        }.sortedByDescending { it.dateImported }

        _documents.value = list
    }

    suspend fun importDocument(uri: Uri, tenantId: Long? = null): Result<BillingDocument> = withContext(Dispatchers.IO) {
        try {
            var fileName = "Document_${System.currentTimeMillis()}.pdf"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIdx != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIdx) ?: fileName
                }
            }

            val targetFile = File(docsDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(IllegalStateException("Failed to read PDF document."))

            val extracted = PdfTextExtractor.extractTextFromPdf(context, targetFile)

            val doc = BillingDocument(
                id = System.currentTimeMillis(),
                tenantId = tenantId,
                title = targetFile.nameWithoutExtension,
                fileName = targetFile.name,
                filePath = targetFile.absolutePath,
                fileSizeBytes = targetFile.length(),
                extractedText = extracted,
                isIndexed = true
            )

            scanDocuments()
            Result.success(doc)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addExportedPdf(pdfFile: File, tenantId: Long? = null): Result<BillingDocument> = withContext(Dispatchers.IO) {
        try {
            if (!pdfFile.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Exported PDF does not exist."))
            }

            val targetFile = File(docsDir, pdfFile.name)
            pdfFile.copyTo(targetFile, overwrite = true)
            val extracted = PdfTextExtractor.extractTextFromPdf(context, targetFile)

            val doc = BillingDocument(
                id = System.currentTimeMillis(),
                tenantId = tenantId,
                title = targetFile.nameWithoutExtension,
                fileName = targetFile.name,
                filePath = targetFile.absolutePath,
                fileSizeBytes = targetFile.length(),
                extractedText = extracted,
                isIndexed = true
            )

            scanDocuments()
            Result.success(doc)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDocument(filePath: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(filePath)
        val deleted = if (file.exists()) file.delete() else false
        scanDocuments()
        deleted
    }
}
