package com.uoa.sensor.repository

import android.util.Log
import com.uoa.core.database.daos.TripDao
import com.uoa.core.database.entities.TripEntity
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.model.Trip
import com.uoa.core.utils.toDomainModel
import com.uoa.core.utils.toEntity
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

class TripDataRepositoryImpl @Inject constructor(private val tripDataDao: TripDao):
    TripDataRepository {

    override suspend fun insertTrip(trip: Trip){
        return tripDataDao.insertTrip(trip.toEntity())
    }

    override suspend fun getTripsBetweenDates(startDate: LocalDate, endDate: LocalDate): List<Trip>
    {
        try {
            val tripEntities=tripDataDao.getTripDataBetween(startDate, endDate)
            val tripDataList = tripEntities.map {
                val domainModel = it.toDomainModel()
                Log.d("TripRepository", "Converted to domain model: $domainModel")
                domainModel
        }
            Log.d(
                "TripRepository",
                "Returning tripsDataList with size: ${tripDataList.size}"
            )
            return tripDataList
        }
        catch (e: Exception) {
            Log.e("TripRepository", "Error getting trips between dates: Start: $startDate and End: $endDate", e)

            return emptyList()

        }

    }

    override suspend fun getNewTrips(): List<Trip> {
        return tripDataDao.getNewTrips() // Implemented as trips with sync=false and endDate/endTime=null
    }

    override suspend fun getUpdatedTrips(): List<Trip> {
        return tripDataDao.getUpdatedTrips() // Implemented as trips with sync=false and endDate/endTime!=null
    }

    override suspend fun getTripByIds(ids: List<UUID>): List<Trip>{
        Log.d("TripRepository", "getTripByIds called with ids: $ids")

        try {
            val tripEntities = tripDataDao.getTripsByIds(ids)
            Log.d("TripRepository", "Retrieved tripEntities: $tripEntities")
            val tripDataList = tripEntities.map {
                val domainModel = it.toDomainModel()
                Log.d("TripRepository", "Converted to domain model: $domainModel")
                domainModel
            }
            Log.d(
                "TripRepository",
                "Returning tripsDataList with size: ${tripDataList.size}"
            )
            return tripDataList
        }
        catch (e: Exception) {
            Log.e("TripRepository", "Error getting trips by ids: $ids", e)

            return emptyList()

        }
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

    override suspend fun updateUploadStatus(id: UUID, sync: Boolean) {
        return tripDataDao.updateUploadStatus(id, sync)
    }

    override suspend fun getTripsByDriverProfileId(driverProfileId: UUID): List<Trip> {
        return tripDataDao.getTripsByDriverProfileId(driverProfileId).map { it.toDomainModel() }
    }

    override suspend fun getTripsBySyncStatus(synced: Boolean): List<Trip> {
        return tripDataDao.getTripsBySyncStatus(synced).map { it.toDomainModel() }
    }

    override suspend fun getLastInsertedTrip(): TripEntity? {
        return tripDataDao.getLastInsertedTrip()
    }

    override suspend fun deleteTripById(id: UUID) {
        tripDataDao.deleteTripById(id)
    }
}
