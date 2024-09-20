package com.uoa.sensor.repository

import android.util.Log
import com.uoa.core.database.daos.LocationDao
import com.uoa.core.database.daos.RawSensorDataDao
import com.uoa.core.database.entities.LocationEntity
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.model.LocationData
import com.uoa.core.utils.toDomainModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID

class LocationRepositoryImpl(private val locationDao: LocationDao, val rawSensorDataDao: RawSensorDataDao): LocationRepository {
    override suspend fun insertLocation(location: LocationEntity) {
        withContext(Dispatchers.IO) {
            locationDao.insertLocation(location)
        }
    }

    override suspend fun getLocationsByIds(ids: List<UUID>): List<LocationData> {
        Log.d("LocationRepository", "getLocationsByIds called with ids: $ids")

        try {
            val locationEntities = locationDao.getLocationsByIds(ids)
            Log.d("LocationRepository", "Retrieved locationEntities: $locationEntities")
            val locationDataList = locationEntities.map {
                val domainModel = it.toDomainModel()
                Log.d("LocationRepository", "Converted to domain model: $domainModel")
                domainModel
            }
            Log.d(
                "LocationRepository",
                "Returning locationDataList with size: ${locationDataList.size}"
            )
            return locationDataList
        }
        catch (e: Exception) {
            Log.e("LocationRepository", "Error getting locations by ids: $ids", e)

            return emptyList()

        }
    }



    override suspend fun insertLocationBatch(locationList: List<LocationEntity>) {
        withContext(Dispatchers.IO) {
            locationDao.insertLocationBatch(locationList)
        }
    }


    override suspend fun getLocationById(id: UUID): LocationEntity? {
        return locationDao.getLocationById(id)
    }

    override fun getLocationBySynced(syncStat: Boolean): Flow<List<LocationEntity>> {
        return locationDao.getLocationBySyncStatus(syncStat)
    }

    override suspend fun updateLocation(location: LocationEntity) {
        locationDao.updateLocation(location)
    }

    override suspend fun deleteAllLocations() {
        locationDao.deleteAllLocations()
    }

    override suspend fun getLocationDataByTripId(tripId: UUID): List<Double> {
        val rawSensorDataList=rawSensorDataDao.getSensorDataByTripId(tripId)

        val locationIdList= mutableListOf(UUID.randomUUID())

        val locationsList= mutableListOf(0.0,0.0,0.0)

        rawSensorDataList.collect{ rawSensorData ->
            rawSensorData.forEach() {
                locationIdList.add(it.locationId)
            }
        }

        locationIdList.forEach() {
            val location=locationDao.getLocationById(it)
            locationsList.add(location!!.latitude)
            locationsList.add(location.longitude)
            locationsList.add(location.altitude)
        }

        return locationsList.toList()

    }
}
