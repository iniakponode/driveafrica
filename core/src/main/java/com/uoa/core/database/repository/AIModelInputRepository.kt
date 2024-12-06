package com.uoa.core.database.repository

import com.uoa.core.database.entities.AIModelInputsEntity
import com.uoa.core.model.AIModelInputs
import com.uoa.core.model.LocationData
import com.uoa.core.model.RawSensorData
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

interface AIModelInputRepository {

        suspend fun insertAiModelInput(aiModelInput: AIModelInputsEntity)
        suspend fun deleteAiModelInput()
        suspend fun getAiModelInputs(): List<AIModelInputsEntity>
        suspend fun getAiModelInputById(id: Int): AIModelInputsEntity?
        suspend fun getAiModelInputInputByTripId(tripId:UUID): List<AIModelInputs>
        suspend fun updateAiModelInput(aiModelInput: AIModelInputsEntity)
        suspend fun deleteAiModelInputById(id: Int)
        suspend fun processDataForAIModelInputs(sensorData: RawSensorData, location: LocationData, tripId:UUID)

}