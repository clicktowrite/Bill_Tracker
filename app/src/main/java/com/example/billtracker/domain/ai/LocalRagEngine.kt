package com.example.billtracker.domain.ai

import com.example.billtracker.data.repository.BillTrackerRepository
import com.example.billtracker.domain.formatter.CurrencyFormatter
import com.example.billtracker.domain.model.BillingDocument
import com.example.billtracker.domain.model.MeterReading
import com.example.billtracker.domain.model.Tenant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DocumentChunk(
    val documentTitle: String,
    val chunkIndex: Int,
    val text: String,
    val tenantId: Long?
)

data class RagContextResult(
    val promptWithContext: String,
    val sources: List<String>
)

class LocalRagEngine(
    private val repository: BillTrackerRepository,
    private val documentLibrary: LocalDocumentLibrary,
    val inferenceEngine: LocalInferenceEngine = LlamaCppInferenceEngine()
) {

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    /**
     * Chunks extracted text into manageable paragraphs for search retrieval.
     */
    fun chunkDocument(doc: BillingDocument, chunkSizeWords: Int = 80): List<DocumentChunk> {
        val text = doc.extractedText ?: return emptyList()
        val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()

        val chunks = mutableListOf<DocumentChunk>()
        var i = 0
        var chunkIdx = 0
        while (i < words.size) {
            val end = (i + chunkSizeWords).coerceAtMost(words.size)
            val chunkWords = words.subList(i, end).joinToString(" ")
            chunks.add(
                DocumentChunk(
                    documentTitle = doc.title,
                    chunkIndex = chunkIdx++,
                    text = chunkWords,
                    tenantId = doc.tenantId
                )
            )
            i += (chunkSizeWords * 3) / 4 // 25% overlap
        }
        return chunks
    }

    /**
     * Builds structured prompt integrating exact Room DB figures and relevant retrieved document chunks.
     */
    suspend fun buildPromptWithContext(
        query: String,
        selectedTenantId: Long? = null
    ): RagContextResult = withContext(Dispatchers.IO) {
        val sources = mutableListOf<String>()
        val sb = StringBuilder()

        sb.append("System: You are an intelligent billing assistant for BillTracker. Provide factual answers based ONLY on the verified records below. Never fabricate amounts or dates.\n\n")

        // 1. Structured Database Records (Authoritative Truth)
        val tenants = repository.allTenantsFlow.first()
        val activeTenant: Tenant? = if (selectedTenantId != null) {
            tenants.find { it.id == selectedTenantId }
        } else {
            tenants.firstOrNull()
        }

        if (activeTenant != null) {
            sb.append("--- Official Billing Data (Room Database) ---\n")
            sb.append("Customer: ${activeTenant.name} (Unit: ${activeTenant.roomOrUnit})\n")

            val meters = repository.getAllMetersForTenant(activeTenant.id).first()
            val currencySymbol = repository.currencySymbolFlow.first()
            val readings = repository.getReadingsForTenant(activeTenant.id).first()

            meters.forEach { meter ->
                sb.append("Meter: ${meter.name} (${meter.utilityType.displayName}), Current Reading: ${meter.currentReading}\n")
            }

            if (readings.isNotEmpty()) {
                sb.append("Recent Finalized Bills:\n")
                readings.take(5).forEach { r ->
                    val dateStr = dateFormatter.format(Date(r.billingDate))
                    val statusStr = if (r.isPaid) "PAID" else "UNPAID"
                    val amtStr = CurrencyFormatter.format(r.totalAmount, currencySymbol)
                    sb.append("• Date: $dateStr | Usage: ${r.usage} | Total: $amtStr | Status: $statusStr\n")
                }
            } else {
                sb.append("No finalized billing records found for this profile.\n")
            }
            sb.append("-------------------------------------------\n\n")
            sources.add("Database: ${activeTenant.name} Records")
        }

        // 2. Local Document Library Retrieval (RAG)
        val docs = documentLibrary.documents.value.filter {
            selectedTenantId == null || it.tenantId == null || it.tenantId == selectedTenantId
        }

        val allChunks = docs.flatMap { chunkDocument(it) }
        val queryTokens = query.lowercase().split("\\s+".toRegex()).filter { it.length > 2 }

        val rankedChunks = allChunks.map { chunk ->
            val chunkLower = chunk.text.lowercase()
            val score = queryTokens.count { token -> chunkLower.contains(token) }
            chunk to score
        }.filter { it.second > 0 }.sortedByDescending { it.second }.take(3)

        if (rankedChunks.isNotEmpty()) {
            sb.append("--- Retrieved Supporting Documents ---\n")
            rankedChunks.forEach { (chunk, _) ->
                sb.append("[From ${chunk.documentTitle}]: ${chunk.text}\n\n")
                if (!sources.contains("Doc: ${chunk.documentTitle}")) {
                    sources.add("Doc: ${chunk.documentTitle}")
                }
            }
            sb.append("---------------------------------------\n\n")
        }

        sb.append("User Query: $query\n")
        sb.append("Assistant: ")

        RagContextResult(
            promptWithContext = sb.toString(),
            sources = sources
        )
    }
}
