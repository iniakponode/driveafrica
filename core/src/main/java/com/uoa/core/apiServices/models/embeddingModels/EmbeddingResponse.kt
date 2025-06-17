package com.uoa.core.apiServices.models.embeddingModels

// EmbeddingResponse.kt
data class EmbeddingResponse(
    val chunk_id: String, // UUID as String
    val chunk_text: String,
    val embedding: String,
    val source_type: String,
    val source_page: Int,
    val created_at: String // ISO 8601 format
)

