package com.uoa.sensor.repository

import com.uoa.core.database.daos.TripSummaryDao
import com.uoa.core.database.repository.TripSummaryRepository
import com.uoa.core.model.TripSummary
import com.uoa.core.utils.toBehaviourEntities
import com.uoa.core.utils.toDomainModel
import com.uoa.core.utils.toEntity
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class TripSummaryRepositoryImpl @Inject constructor(
    private val tripSummaryDao: TripSummaryDao
) : TripSummaryRepository {
    override suspend fun insertTripSummary(tripSummary: TripSummary) {
        tripSummaryDao.upsertTripSummaryWithBehaviours(
            tripSummary.toEntity(),
            tripSummary.toBehaviourEntities()
        )
    }

    override suspend fun getTripSummaryByTripId(tripId: UUID): TripSummary? {
        return tripSummaryDao.getTripSummaryByTripId(tripId)?.toDomainModel()
    }

    override suspend fun getTripSummariesByDriverAndDateRange(
        driverId: UUID,
        startDate: Date,
        endDate: Date
    ): List<TripSummary> {
        return tripSummaryDao.getTripSummariesByDriverAndDateRange(driverId, startDate, endDate)
            .map { it.toDomainModel() }
    }

    override suspend fun getUnsyncedTripSummaries(): List<TripSummary> {
        return tripSummaryDao.getUnsyncedTripSummaries()
            .map { it.toDomainModel() }
    }

    override suspend fun markTripSummariesSynced(tripIds: List<UUID>, synced: Boolean) {
        if (tripIds.isEmpty()) {
            return
        }
        tripSummaryDao.markTripSummariesSynced(tripIds, synced)
    }

    override suspend fun countTripSummariesByDriverAndDateRange(
        driverId: UUID,
        startDate: Date,
        endDate: Date
    ): Int {
        return tripSummaryDao.countTripSummariesByDriverAndDateRange(driverId, startDate, endDate)
    }
}
