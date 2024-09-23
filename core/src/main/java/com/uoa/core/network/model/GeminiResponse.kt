package com.uoa.core.network.model

data class GeminiResponse(
    val result: String,
    val status: String,
    val data: List<GeminiData>
)

data class Metadata(
    // Include metadata fields
    val metadata: String
)
