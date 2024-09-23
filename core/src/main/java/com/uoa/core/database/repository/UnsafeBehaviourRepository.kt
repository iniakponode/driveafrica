package com.uoa.core.database.repository

import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.database.entities.UnsafeBehaviourEntity
import com.uoa.core.model.RawSensorData
import com.uoa.core.model.UnsafeBehaviourModel
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

interface UnsafeBehaviourRepository {
    suspend fun insertUnsafeBehaviour(unsafeBehaviour: UnsafeBehaviourModel)

    suspend fun getEntitiesToBeUpdated(tripId: UUID): Flow<List<UnsafeBehaviourEntity>>

    suspend fun updateUnsafeBehaviourTransactions(
        unsafeBehaviours: List<UnsafeBehaviourEntity>,
        alcoholInf: Boolean
    )
    suspend fun insertUnsafeBehaviourBatch(unsafeBehaviours: List<UnsafeBehaviourModel>)

    suspend fun updateUnsafeBehaviour(unsafeBehaviour: UnsafeBehaviourModel)

    suspend fun deleteUnsafeBehaviour(unsafeBehaviour: UnsafeBehaviourModel)

    fun getUnsafeBehavioursByTripId(tripId: UUID): Flow<List<UnsafeBehaviourModel>>

    suspend fun batchUpdateUnsafeBehaviours(unsafeBehaviours: List<UnsafeBehaviourEntity>)

    suspend fun getUnsafeBehaviourById(id: UUID): UnsafeBehaviourEntity?

    suspend fun getUnsafeBehavioursBySyncStatus(synced: Boolean): List<UnsafeBehaviourEntity>

    suspend fun deleteAllUnsafeBehavioursBySyncStatus(synced: Boolean)

    suspend fun deleteAllUnsafeBehaviours()

    suspend fun getUnsafeBehaviourCountByTypeAndTime(
        behaviorType: String,
        startTime: Long,
        endTime: Long
    ): Int

    suspend fun getUnsafeBehaviourCountByTypeAndDistance(
        behaviourType: String,
        tripId: UUID,
        totalDistance: Float
    ): Int

    fun getUnsafeBehavioursBetweenDates(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<UnsafeBehaviourModel>>

    suspend fun getSensorDataBetweenDates(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<RawSensorDataEntity>>

    suspend fun getSensorDataByTripId(tripId: UUID): Flow<List<RawSensorDataEntity>>

    suspend fun getSensorDataBySyncStatus(synced: Boolean): List<RawSensorData>

    suspend fun getLastInsertedUnsafeBehaviour(): UnsafeBehaviourEntity?


}