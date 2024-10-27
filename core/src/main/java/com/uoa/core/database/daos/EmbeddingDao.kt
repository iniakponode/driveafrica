package com.uoa.core.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.uoa.core.database.entities.EmbeddingEntity
import java.util.UUID

@Dao
interface EmbeddingDao {
    // Insert a new embedding
    @Insert
    suspend fun insertEmbedding(embedding: EmbeddingEntity)

    // Retrieve all embeddings
    @Query("SELECT * FROM embeddings")
    suspend fun getAllEmbeddings(): List<EmbeddingEntity>

    // Retrieve embeddings by source type
    @Query("SELECT * FROM embeddings WHERE sourceType = :sourceType")
    suspend fun getEmbeddingsBySourceType(sourceType: String): List<EmbeddingEntity>

    // Retrieve a specific embedding by its chunk ID
    @Query("SELECT * FROM embeddings WHERE chunkId = :chunkId LIMIT 1")
    suspend fun getEmbeddingByChunkId(chunkId: UUID): EmbeddingEntity?

    // Delete all embeddings (if you want to reset the database)
    @Query("DELETE FROM embeddings")
    suspend fun clearAllEmbeddings()

    // Update an existing embedding
    @Update
    suspend fun updateEmbedding(embeddingEntity: EmbeddingEntity)

    // Delete an embedding by its chunk ID
    @Query("DELETE FROM embeddings WHERE chunkId = :chunkId")
    suspend fun deleteEmbeddingByChunkId(chunkId: UUID)

}
