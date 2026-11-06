package com.example.billtracker.domain.ai

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class GgufModelManager(
    private val context: Context,
    val inferenceEngine: LocalInferenceEngine = LlamaCppInferenceEngine()
) {

    private val modelsDir: File
        get() {
            val dir = File(context.filesDir, "models")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    private val _models = MutableStateFlow<List<GgufModel>>(emptyList())
    val models: StateFlow<List<GgufModel>> = _models.asStateFlow()

    private val _activeModelId = MutableStateFlow<String?>(null)
    val activeModelId: StateFlow<String?> = _activeModelId.asStateFlow()

    init {
        scanModels()
    }

    fun scanModels() {
        val files = modelsDir.listFiles { f -> f.extension.equals("gguf", ignoreCase = true) } ?: emptyArray()
        val loadedId = inferenceEngine.loadedModel?.id

        val list = files.map { file ->
            val metadata = GgufHeaderParser.parse(file)
            val modelId = file.name
            val isLoaded = loadedId == modelId

            GgufModel(
                id = modelId,
                name = metadata?.modelName ?: file.nameWithoutExtension,
                fileName = file.name,
                filePath = file.absolutePath,
                sizeBytes = file.length(),
                architecture = metadata?.architecture ?: "Unknown",
                quantization = metadata?.quantization ?: "GGUF",
                contextLength = metadata?.contextLength ?: 2048,
                tensorCount = metadata?.tensorCount ?: 0,
                state = if (isLoaded) ModelState.LOADED else ModelState.IMPORTED,
                dateImported = file.lastModified()
            )
        }.sortedByDescending { it.dateImported }

        _models.value = list
    }

    suspend fun importModelFromUri(uri: Uri): Result<GgufModel> = withContext(Dispatchers.IO) {
        try {
            var fileName = "model_${System.currentTimeMillis()}.gguf"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex) ?: fileName
                }
            }

            if (!fileName.endsWith(".gguf", ignoreCase = true)) {
                return@withContext Result.failure(IllegalArgumentException("Only .gguf files are supported."))
            }

            val targetFile = File(modelsDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(IllegalStateException("Unable to open input stream for model."))

            val metadata = GgufHeaderParser.parse(targetFile)
            if (metadata == null) {
                targetFile.delete()
                return@withContext Result.failure(IllegalArgumentException("Invalid or unsupported GGUF format."))
            }

            val model = GgufModel(
                id = targetFile.name,
                name = metadata.modelName ?: targetFile.nameWithoutExtension,
                fileName = targetFile.name,
                filePath = targetFile.absolutePath,
                sizeBytes = targetFile.length(),
                architecture = metadata.architecture ?: "transformer",
                quantization = metadata.quantization ?: "GGUF",
                contextLength = metadata.contextLength,
                tensorCount = metadata.tensorCount,
                state = ModelState.IMPORTED
            )

            scanModels()
            Result.success(model)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadModel(modelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val model = _models.value.find { it.id == modelId }
            ?: return@withContext Result.failure(IllegalArgumentException("Model not found."))

        // Unload existing
        inferenceEngine.unloadModel()

        // Update state to loading
        _models.value = _models.value.map {
            if (it.id == modelId) it.copy(state = ModelState.LOADING) else it
        }

        val result = inferenceEngine.loadModel(model)
        if (result.isSuccess) {
            _activeModelId.value = modelId
            _models.value = _models.value.map {
                if (it.id == modelId) it.copy(state = ModelState.LOADED)
                else it.copy(state = ModelState.IMPORTED)
            }
            Result.success(Unit)
        } else {
            val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to load model"
            _models.value = _models.value.map {
                if (it.id == modelId) it.copy(state = ModelState.FAILED, errorMessage = err)
                else it
            }
            Result.failure(result.exceptionOrNull() ?: Exception(err))
        }
    }

    suspend fun unloadActiveModel() = withContext(Dispatchers.IO) {
        inferenceEngine.unloadModel()
        _activeModelId.value = null
        _models.value = _models.value.map { it.copy(state = ModelState.IMPORTED) }
    }

    suspend fun deleteModel(modelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val model = _models.value.find { it.id == modelId }
            ?: return@withContext Result.failure(IllegalArgumentException("Model not found."))

        if (model.state == ModelState.LOADED || _activeModelId.value == modelId) {
            return@withContext Result.failure(IllegalStateException("Cannot delete a loaded model. Unload it first."))
        }

        val file = File(model.filePath)
        if (file.exists()) file.delete()
        scanModels()
        Result.success(Unit)
    }
}
