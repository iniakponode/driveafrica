package com.uoa.core.apiServices.models.embedding

// EmbeddingCreate.kt
data class EmbeddingCreate(
    val chunk_text: String,
    val embedding: String,
    val source_type: String,
    val source_page: Int
)

