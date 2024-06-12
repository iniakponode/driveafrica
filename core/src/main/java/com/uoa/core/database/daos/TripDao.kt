package com.uoa.core.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.uoa.core.database.entities.TripEntity
import java.util.UUID

@Dao
interface TripDao {
    @Insert
    suspend fun insertTrip(tripEntity: TripEntity)

//    @Query("UPDATE trip_data SET endTime=:eTime WHERE id = :id")
//    suspend fun updateTrip(eTime:Long, id:Long)
    @Update
    suspend fun updateTrip(tripEntity: TripEntity)

    @Query("UPDATE trip_data SET synced=:synced WHERE id = :id")
    suspend fun updateUploadStatus(id:Int, synced: Boolean)

    @Query("SELECT * FROM  trip_data WHERE id = :id")
    suspend fun getTripById(id:UUID): TripEntity?

    @Query("SELECT * FROM trip_data")
    suspend fun getAllTrips(): List<TripEntity>

    @Query("SELECT * FROM trip_data WHERE driverProfileId = :driverProfileId")
    suspend fun getTripsByDriverProfileId(driverProfileId: Long): List<TripEntity>

    @Query("SELECT * FROM trip_data WHERE synced = :synced")
    suspend fun getTripsBySyncStatus(synced: Boolean): List<TripEntity>

    @Query("DELETE FROM trip_data WHERE id = :id")
    suspend fun deleteTripById(id: Long)
}
