package com.example.billtracker.domain.ai

import kotlinx.coroutines.flow.Flow

data class InferenceParameters(
    val temperature: Float = 0.7f,
    val maxTokens: Int = 512,
    val topP: Float = 0.9f
)

interface LocalInferenceEngine {
    val loadedModel: GgufModel?
    val isModelLoaded: Boolean

    suspend fun loadModel(model: GgufModel): Result<Unit>
    suspend fun unloadModel()
    fun generateResponse(
        prompt: String,
        parameters: InferenceParameters = InferenceParameters()
    ): Flow<String>
    fun stopGeneration()
}
