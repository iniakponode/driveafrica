package com.uoa.core.nlg.lngrepositoryimpl.local


import com.uoa.core.database.daos.EmbeddingDao
import com.uoa.core.database.entities.EmbeddingEntity
import com.uoa.core.nlg.repository.EmbeddingUtilsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class EmbeddingUtilsRepositoryImpl @Inject constructor(
    private val embeddingDao: EmbeddingDao
) : EmbeddingUtilsRepository {

    override suspend fun saveEmbedding(embeddingEntity: EmbeddingEntity) = withContext(Dispatchers.IO) {
        embeddingDao.insertEmbedding(embeddingEntity)
    }

    override suspend fun getEmbeddingByChunkId(chunkId: UUID): EmbeddingEntity? = withContext(Dispatchers.IO) {
        embeddingDao.getEmbeddingByChunkId(chunkId)
    }

    override suspend fun getAllEmbeddings(): List<EmbeddingEntity> = withContext(Dispatchers.IO) {
        embeddingDao.getAllEmbeddings()
    }

    override suspend fun updateEmbedding(embeddingEntity: EmbeddingEntity) = withContext(Dispatchers.IO) {
        embeddingDao.updateEmbedding(embeddingEntity)
    }

    override suspend fun deleteEmbedding(chunkId: UUID) = withContext(Dispatchers.IO) {
        embeddingDao.deleteEmbeddingByChunkId(chunkId)
    }
}