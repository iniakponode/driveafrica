package com.uoa.ml.data.repository

import com.uoa.core.database.daos.AIModelInputDao
import com.uoa.core.database.entities.AIModelInputsEntity
import com.uoa.core.database.repository.AIModelInputRepository
import com.uoa.core.model.AIModelInputs
import com.uoa.core.model.LocationData
import com.uoa.core.model.RawSensorData
import java.util.UUID
import javax.inject.Inject

class AIModelInputRepositoryImpl @Inject constructor(
    private val aiModelInputDao: AIModelInputDao
) : AIModelInputRepository {

    override suspend fun insertAiModelInput(aiModelInput: AIModelInputsEntity) {
        aiModelInputDao.insertAiModelInput(aiModelInput)
    }

    override suspend fun deleteAiModelInput() {
        aiModelInputDao.deleteAllAiModelInputs()
    }

    override suspend fun getAiModelInputs(): List<AIModelInputsEntity> {
        return aiModelInputDao.getAllAiModelInputs()
    }

    override suspend fun getAiModelInputById(id: UUID): AIModelInputsEntity? {
        return aiModelInputDao.getAiModelInputById(id)
    }

    override suspend fun updateAiModelInput(aiModelInput: AIModelInputsEntity) {
        aiModelInputDao.updateAiModelInput(aiModelInput)
    }

    override suspend fun deleteAiModelInputById(id: UUID) {
        aiModelInputDao.deleteAiModelInputById(id)
    }

    override suspend fun deleteAIModelInputsByIds(ids: List<UUID>) {
        aiModelInputDao.deleteAIModelInputsByIds(ids)
    }

    override suspend fun deleteAiModelInputsByTripId(tripId: UUID) {
        aiModelInputDao.deleteAiModelInputsByTripId(tripId)
    }

    override suspend fun getAiModelInputInputByTripId(tripId: UUID): List<AIModelInputs> {
        return aiModelInputDao.getAiModelInputsByTripId(tripId)
    }

    override suspend fun getAiModelInputsBySyncStatus(status: Boolean): List<AIModelInputsEntity> {
        return aiModelInputDao.getAiModelInputsBySyncStatus(status)
    }

    override suspend fun getAiModelInputsBySyncAndProcessedStatus(
        synced: Boolean,
        processed: Boolean
    ): List<AIModelInputsEntity> {
        return aiModelInputDao.getAiModelInputBySyncAndProcessedStatus(synced,processed)
    }


    override suspend fun processDataForAIModelInputs(sensorData: RawSensorData, location: LocationData, tripId:UUID) {
        // Trip-level aggregation happens at trip end; per-sample processing is intentionally skipped.
    }
}
