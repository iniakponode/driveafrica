//package com.uoa.core.nlg.repository
//
//import com.uoa.core.database.entities.EmbeddingEntity
//import java.util.UUID
//
//interface EmbeddingUtilsRepository {
//    suspend fun saveEmbedding(embeddingEntity: EmbeddingEntity)
//    suspend fun getEmbeddingByChunkId(chunkId: UUID): EmbeddingEntity?
//    suspend fun getAllEmbeddings(): List<EmbeddingEntity>
//    suspend fun updateEmbedding(embeddingEntity: EmbeddingEntity)
//    suspend fun deleteEmbedding(chunkId: UUID)
//}