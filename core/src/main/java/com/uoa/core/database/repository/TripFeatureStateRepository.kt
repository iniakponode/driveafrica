package com.uoa.core.database.repository

import com.uoa.core.database.entities.TripFeatureStateEntity
import java.util.UUID

interface TripFeatureStateRepository {
    suspend fun getUnsyncedTripFeatureStates(): List<TripFeatureStateEntity>

    suspend fun markTripFeatureStatesSynced(tripIds: List<UUID>, synced: Boolean)
}
