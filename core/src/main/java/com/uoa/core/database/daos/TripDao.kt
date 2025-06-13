package com.uoa.core.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.uoa.core.database.entities.LocationEntity
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.database.entities.TripEntity
import com.uoa.core.model.Trip
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

@Dao
interface TripDao {
    @Insert
    suspend fun insertTrip(tripEntity: TripEntity)

//    @Query("UPDATE trip_data SET endTime=:eTime WHERE id = :id")
//    suspend fun updateTrip(eTime:Long, id:Long)
    @Update
    suspend fun updateTrip(tripEntity: TripEntity)

    @Query("SELECT * FROM trip_data WHERE id IN (:ids)")
    suspend fun getTripsByIds(ids: List<UUID>): List<TripEntity>

    @Query("UPDATE trip_data SET sync=:synced WHERE id = :id")
    suspend fun updateUploadStatus(id: UUID, synced: Boolean)

    @Query("""
        SELECT * FROM trip_data
        WHERE sync = false 
        AND endDate IS NULL 
        AND endTime IS NULL
    """)
    suspend fun getNewTrips(): List<Trip>

    @Query("""
        SELECT * FROM trip_data 
        WHERE sync = false 
        AND (endDate IS NOT NULL OR endTime IS NOT NULL)
    """)
    suspend fun getUpdatedTrips(): List<Trip>

    @Query("SELECT * FROM  trip_data WHERE id = :id")
    suspend fun getTripById(id:UUID): TripEntity?

    @Query("SELECT * FROM trip_data")
    suspend fun getAllTrips(): List<TripEntity>

    @Query("SELECT * FROM trip_data WHERE driverPId = :driverProfileId")
    suspend fun getTripsByDriverProfileId(driverProfileId: UUID): List<TripEntity>

    @Query("SELECT * FROM trip_data WHERE endDate BETWEEN :startDate AND :endDate")
    suspend fun getTripDataBetween(startDate: LocalDate, endDate: LocalDate): List<TripEntity>


    @Query("SELECT * FROM trip_data WHERE sync = :synced")
    suspend fun getTripsBySyncStatus(synced: Boolean): List<TripEntity>

    @Query("DELETE FROM trip_data WHERE id = :id")
    suspend fun deleteTripById(id: UUID)

    // New function to fetch the last inserted trip
    @Query("SELECT * FROM trip_data ORDER BY endTime DESC LIMIT 1")
    suspend fun getLastInsertedTrip(): TripEntity?
}
