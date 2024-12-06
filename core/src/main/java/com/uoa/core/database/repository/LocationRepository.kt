package com.uoa.core.database.repository

import com.uoa.core.database.entities.LocationEntity
import com.uoa.core.model.LocationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

interface LocationRepository {
    suspend fun insertLocation(location: LocationEntity)

    suspend fun getLocationsByIds(ids: List<UUID>): List<LocationData>

    suspend fun insertLocationBatch(locationList: List<LocationEntity>)

    suspend fun getLocationById(id: UUID): LocationEntity?

    fun getLocationBySynced(syncStat: Boolean): Flow<List<LocationEntity>>

    suspend fun updateLocation(location: LocationEntity)

    suspend fun deleteAllLocations()
    suspend fun getLocationDataByTripId(tripId: UUID): List<LocationData>
}