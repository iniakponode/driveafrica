package com.uoa.dbda.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.uoa.core.database.daos.RawSensorDataDao
import com.uoa.core.database.daos.UnsafeBehaviourDao
import com.uoa.core.database.entities.UnsafeBehaviourEntity
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.core.utils.toDomainModel
import com.uoa.core.utils.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.core.model.RawSensorData
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnsafeBehaviourRepositoryImpl @Inject constructor(
    private val unsafeBehaviourDao: UnsafeBehaviourDao, private val rawSensorDataDao: RawSensorDataDao
): UnsafeBehaviourRepository {

    override suspend fun insertUnsafeBehaviour(unsafeBehaviour: UnsafeBehaviourModel) {
        unsafeBehaviourDao.insertUnsafeBehaviour(unsafeBehaviour.toEntity())
    }

    override suspend fun insertUnsafeBehaviourBatch(unsafeBehaviours: List<UnsafeBehaviourModel>) {
        withContext(Dispatchers.IO){
            unsafeBehaviourDao.insertUnsafeBehaviourBatch(unsafeBehaviours.map { it.toEntity() })
        }
    }

    override suspend fun updateUnsafeBehaviour(unsafeBehaviour: UnsafeBehaviourModel) {
        unsafeBehaviourDao.updateUnsafeBehaviour(unsafeBehaviour.toEntity())
    }

    override suspend fun batchUpdateUnsafeBehaviours(unsafeBehaviours: List<UnsafeBehaviourEntity>) {
        unsafeBehaviourDao.updateUnsafeBehaviours(unsafeBehaviours)
    }

    override suspend fun deleteUnsafeBehaviour(unsafeBehaviour: UnsafeBehaviourModel) {
        unsafeBehaviourDao.deleteUnsafeBehaviour(unsafeBehaviour.toEntity())
    }

    override fun getUnsafeBehavioursByTripId(tripId: UUID): Flow<List<UnsafeBehaviourModel>> {

            return unsafeBehaviourDao.getUnsafeBehavioursByTripId(tripId).map { unsafeBehaviourEntityList ->
                unsafeBehaviourEntityList.map {
                    it.toDomainModel()
                }
            }
    }
   override suspend fun getEntitiesToBeUpdated(tripId: UUID): Flow<List<UnsafeBehaviourEntity>> {
        return unsafeBehaviourDao.getEntitiesToBeUpdated(tripId)
    }

    override suspend fun updateUnsafeBehaviourTransactions(unsafeBehaviours: List<UnsafeBehaviourEntity>, alcoholInf: Boolean) {
        unsafeBehaviourDao.updateUnsafeBehavioursTransaction(unsafeBehaviours, alcoholInf)
    }
    override suspend fun getUnsafeBehaviourById(id: UUID): UnsafeBehaviourEntity? {
        return unsafeBehaviourDao.getUnsafeBehaviourById(id)
    }

    override suspend fun getUnsafeBehavioursBySyncStatus(synced: Boolean): List<UnsafeBehaviourEntity> {
        return unsafeBehaviourDao.getUnsafeBehavioursBySyncStatus(synced)
    }

    override suspend fun deleteAllUnsafeBehavioursBySyncStatus(synced: Boolean) {
        unsafeBehaviourDao.deleteAllUnsafeBehavioursBySyncStatus(synced)
    }

    override suspend fun deleteAllUnsafeBehaviours() {
        unsafeBehaviourDao.deleteAllUnsafeBehaviours()
    }

    override suspend fun getUnsafeBehaviourCountByTypeAndTime(behaviorType: String, startTime: Long, endTime: Long): Int {
        return unsafeBehaviourDao.getUnsafeBehaviourCountByTypeAndTime(behaviorType, startTime, endTime)
    }

    override suspend fun getUnsafeBehaviourCountByTypeAndDistance(behaviourType: String, tripId: UUID, totalDistance: Float): Int {
        return unsafeBehaviourDao.getUnsafeBehaviourCountByTypeAndDistance(behaviourType, tripId, totalDistance)
    }

    override fun getUnsafeBehavioursBetweenDates(sDate: LocalDate, eDate: LocalDate): Flow<List<UnsafeBehaviourModel>> {
        return unsafeBehaviourDao.getUnsafeBehavioursBetweenDates(sDate, eDate)
            .map { entityList ->
                entityList
                    .filter { it.locationId != null } // Filter out entities with null location
                    .map { it.toDomainModel() }
            }
    }

    override fun getUnsafeBehavioursForTips(): Flow<List<UnsafeBehaviourModel>> {
        return unsafeBehaviourDao.getUnsafeBehavioursForTips()
            .map { entityList ->
                Log.d("UnsafeBehaviourRepositoryImpl", "Fetched ${entityList.size} unsafe behaviours for tips")
                val filteredList = entityList.filter { it.locationId != null }
                Log.d("UnsafeBehaviourRepositoryImpl", "Filtered list size: ${filteredList.size}")
                filteredList.map { it.toDomainModel() }
            }
    }

    override suspend fun getSensorDataBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<RawSensorDataEntity>> {
//        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        return withContext(Dispatchers.IO){
            rawSensorDataDao.getSensorDataBetweenDates(startDate, endDate)
        }

    }

    override suspend fun getSensorDataByTripId(tripId: UUID): Flow<List<RawSensorDataEntity>> {
        return withContext(Dispatchers.IO){
            rawSensorDataDao.getSensorDataByTripId(tripId)
        }
    }

    override suspend fun getSensorDataBySyncStatus(synced: Boolean): List<RawSensorData> {
        val entities=rawSensorDataDao.getSensorDataBySyncStatus(synced)
        return entities.map { it.toDomainModel() }
    }

    override suspend fun getLastInsertedUnsafeBehaviour(): UnsafeBehaviourEntity {
        return withContext(Dispatchers.IO){
            unsafeBehaviourDao.getLastInsertedUnsafeBehaviour()
        }
    }
}

