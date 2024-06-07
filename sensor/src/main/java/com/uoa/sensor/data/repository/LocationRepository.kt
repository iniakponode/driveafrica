package com.uoa.sensor.data.repository

import com.uoa.core.database.daos.LocationDao
import com.uoa.core.database.entities.LocationEntity
import kotlinx.coroutines.flow.Flow

class LocationRepository(private val locationDao: LocationDao) {

    suspend fun insertLocation(location: LocationEntity) {
        locationDao.insertLocation(location)
    }

    suspend fun getLocationById(id: Int): LocationEntity? {
        return locationDao.getLocationById(id)
    }

    fun getUnsyncedLocations(): Flow<List<LocationEntity>> {
        return locationDao.getUnsyncedLocations()
    }

    suspend fun updateLocation(location: LocationEntity) {
        locationDao.updateLocation(location)
    }

    suspend fun deleteAllLocations() {
        locationDao.deleteAllLocations()
    }
}
