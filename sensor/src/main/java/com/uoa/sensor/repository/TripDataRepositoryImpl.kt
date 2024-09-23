package com.uoa.sensor.repository

import com.uoa.core.database.daos.TripDao
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.model.Trip
import com.uoa.core.utils.toDomainModel
import com.uoa.core.utils.toEntity
import java.util.UUID
import javax.inject.Inject

class TripDataRepositoryImpl @Inject constructor(private val tripDataDao: TripDao):
    TripDataRepository {

    override suspend fun insertTrip(trip: Trip){
        return tripDataDao.insertTrip(trip.toEntity())
    }

    override suspend fun updateTrip(trip: Trip) {
        tripDataDao.updateTrip(trip.toEntity())
    }

    override suspend fun getAllTrips(): List<Trip> {
        return tripDataDao.getAllTrips().map { it.toDomainModel() }
    }
    override suspend fun getTripById(id: UUID): Trip? {
        return tripDataDao.getTripById(id)?.toDomainModel()
    }

    override suspend fun updateUploadStatus(id: Int, sync: Boolean) {
        return tripDataDao.updateUploadStatus(id, sync)
    }

    override suspend fun getTripsByDriverProfileId(driverProfileId: UUID): List<Trip> {
        return tripDataDao.getTripsByDriverProfileId(driverProfileId).map { it.toDomainModel() }
    }

    override suspend fun getTripsBySyncStatus(synced: Boolean): List<Trip> {
        return tripDataDao.getTripsBySyncStatus(synced).map { it.toDomainModel() }
    }

    override suspend fun deleteTripById(id: UUID) {
        tripDataDao.deleteTripById(id)
    }
}
