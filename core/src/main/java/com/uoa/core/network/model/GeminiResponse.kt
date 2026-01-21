package com.uoa.core.network.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class GeminiResponse(
    @field:SerializedName("result")
    val result: String,
    @field:SerializedName("status")
    val status: String,
    @field:SerializedName("data")
    val data: List<GeminiData>
)

@Keep
data class Metadata(
    // Include metadata fields
    @field:SerializedName("metadata")
    val metadata: String
)
