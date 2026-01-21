package com.uoa.sensor.repository

import com.uoa.core.database.daos.TripFeatureStateDao
import com.uoa.core.database.entities.TripFeatureStateEntity
import com.uoa.core.database.repository.TripFeatureStateRepository
import java.util.UUID
import javax.inject.Inject

class TripFeatureStateRepositoryImpl @Inject constructor(
    private val tripFeatureStateDao: TripFeatureStateDao
) : TripFeatureStateRepository {
    override suspend fun getUnsyncedTripFeatureStates(): List<TripFeatureStateEntity> {
        return tripFeatureStateDao.getUnsyncedTripFeatureStates()
    }

    override suspend fun markTripFeatureStatesSynced(tripIds: List<UUID>, synced: Boolean) {
        if (tripIds.isEmpty()) {
            return
        }
        tripFeatureStateDao.markTripFeatureStatesSynced(tripIds, synced)
    }
}
