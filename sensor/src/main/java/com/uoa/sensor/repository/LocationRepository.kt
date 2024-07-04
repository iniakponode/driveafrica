package com.uoa.sensor.data.repository

import com.uoa.core.database.daos.LocationDao
import com.uoa.core.database.entities.LocationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class LocationRepository(private val locationDao: LocationDao) {
    suspend fun insertLocation(location: LocationEntity) {
        withContext(Dispatchers.IO) {
            locationDao.insertLocation(location)
        }
    }

    suspend fun insertLocationBatch(locationList: List<LocationEntity>) {
        withContext(Dispatchers.IO) {
            locationDao.insertLocationBatch(locationList)
        }
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
