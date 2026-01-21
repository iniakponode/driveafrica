package com.uoa.core.network.model.Gemini

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class GeminiRequest(
    @field:SerializedName("model")
    val model: String,
    @field:SerializedName("prompt")
    val prompt: Prompt,
    @field:SerializedName("temperature")
    val temperature: Double? = null,
    @field:SerializedName("candidateCount")
    val candidateCount: Int? = null,
    @field:SerializedName("topK")
    val topK: Int? = null,
    @field:SerializedName("topP")
    val topP: Double? = null
)

@Keep
data class Prompt(
    @field:SerializedName("text")
    val text: String
)
