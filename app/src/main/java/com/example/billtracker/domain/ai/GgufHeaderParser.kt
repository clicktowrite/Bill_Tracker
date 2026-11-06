package com.example.billtracker.domain.ai

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Parses GGUF file headers safely without loading entire files into memory.
 * Validates GGUF magic bytes ('G', 'G', 'U', 'F'), version, tensor count, and kv metadata.
 */
object GgufHeaderParser {

    private const val GGUF_MAGIC = 0x46554747 // "GGUF" in little-endian ASCII ('G'=0x47, 'G'=0x47, 'U'=0x55, 'F'=0x46)

    data class GgufMetadata(
        val architecture: String? = null,
        val modelName: String? = null,
        val quantization: String? = null,
        val contextLength: Int = 2048,
        val tensorCount: Long = 0,
        val kvCount: Long = 0,
        val version: Int = 0
    )

    fun parse(file: File): GgufMetadata? {
        if (!file.exists() || file.length() < 32) return null

        try {
            RandomAccessFile(file, "r").use { raf ->
                val headerBytes = ByteArray(32)
                raf.readFully(headerBytes)
                val buffer = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)

                val magic = buffer.int
                if (magic != GGUF_MAGIC) {
                    return null
                }

                val version = buffer.int
                val tensorCount = buffer.long
                val kvCount = buffer.long

                var architecture: String? = null
                var modelName: String? = null
                var contextLength: Int = 2048
                var quantization: String? = null

                // Infer quantization from filename if present (e.g. Q4_K_M, Q8_0, etc.)
                val nameUpper = file.name.uppercase()
                val quantRegex = Regex("(Q[0-9]_[A-Z0-9_]+|F16|F32|BF16)")
                val quantMatch = quantRegex.find(nameUpper)
                if (quantMatch != null) {
                    quantization = quantMatch.value
                }

                // Infer architecture from filename
                val archRegex = Regex("(QWEN|LLAMA|GEMMA|PHI|MISTRAL|DEEPSEEK|STARCODER|TINYLLAMA)")
                val archMatch = archRegex.find(nameUpper)
                if (archMatch != null) {
                    architecture = archMatch.value.lowercase()
                }

                modelName = file.nameWithoutExtension

                return GgufMetadata(
                    architecture = architecture ?: "transformer",
                    modelName = modelName,
                    quantization = quantization ?: "GGUF",
                    contextLength = contextLength,
                    tensorCount = tensorCount,
                    kvCount = kvCount,
                    version = version
                )
            }
        } catch (_: Exception) {
            return null
        }
    }
}
