package com.uoa.core.network.model.Gemini

data class GeminiRequest(
    val model: String,
    val prompt: Prompt,
    val temperature: Double? = null,
    val candidateCount: Int? = null,
    val topK: Int? = null,
    val topP: Double? = null
)

data class Prompt(
    val text: String
)