package com.example.billtracker.domain.ai

enum class ModelState {
    IMPORTED,
    LOADING,
    LOADED,
    FAILED
}

data class GgufModel(
    val id: String,
    val name: String,
    val fileName: String,
    val filePath: String,
    val sizeBytes: Long,
    val architecture: String? = null,
    val quantization: String? = null,
    val contextLength: Int = 2048,
    val tensorCount: Long = 0,
    val state: ModelState = ModelState.IMPORTED,
    val errorMessage: String? = null,
    val dateImported: Long = System.currentTimeMillis()
) {
    val formattedSize: String
        get() {
            val mb = sizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1024) {
                String.format("%.2f GB", mb / 1024.0)
            } else {
                String.format("%.1f MB", mb)
            }
        }
}
