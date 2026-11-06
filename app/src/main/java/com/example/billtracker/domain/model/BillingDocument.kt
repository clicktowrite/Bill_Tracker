package com.example.billtracker.domain.model

data class BillingDocument(
    val id: Long = 0,
    val tenantId: Long?,
    val title: String,
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val dateImported: Long = System.currentTimeMillis(),
    val extractedText: String? = null,
    val isIndexed: Boolean = false,
    val chunkCount: Int = 0
)
