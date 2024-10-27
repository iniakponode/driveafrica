package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "embeddings")
data class EmbeddingEntity(
    @PrimaryKey val chunkId: UUID,          // Unique ID for each chunk of text
    val chunkText: String,                  // The text content of the chunk
    val embedding: ByteArray,               // The embedding vector (serialized as ByteArray)
    val sourceType: String,                 // The type of source (e.g., nat_dr_reg_law, ng_high_way_code)
    val sourcePage: Int?,                   // Optional page number for traceability
    val createdAt: Long                     // Timestamp for when the embedding was created
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EmbeddingEntity

        if (chunkId != other.chunkId) return false
        if (chunkText != other.chunkText) return false
        if (!embedding.contentEquals(other.embedding)) return false
        if (sourceType != other.sourceType) return false
        if (sourcePage != other.sourcePage) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = chunkId.hashCode()
        result = 31 * result + chunkText.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + sourceType.hashCode()
        result = 31 * result + (sourcePage ?: 0)
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
