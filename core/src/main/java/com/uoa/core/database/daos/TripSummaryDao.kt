package com.uoa.core.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.uoa.core.database.entities.TripSummaryEntity
import java.util.Date
import java.util.UUID

@Dao
interface TripSummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTripSummary(tripSummary: TripSummaryEntity)

    @Query("SELECT * FROM trip_summary WHERE tripId = :tripId LIMIT 1")
    suspend fun getTripSummaryByTripId(tripId: UUID): TripSummaryEntity?

    @Query(
        "SELECT * FROM trip_summary WHERE driverId = :driverId AND startDate BETWEEN :startDate AND :endDate"
    )
    suspend fun getTripSummariesByDriverAndDateRange(
        driverId: UUID,
        startDate: Date,
        endDate: Date
    ): List<TripSummaryEntity>

    @Query("SELECT COUNT(*) FROM trip_summary WHERE driverId = :driverId AND startDate BETWEEN :startDate AND :endDate")
    suspend fun countTripSummariesByDriverAndDateRange(
        driverId: UUID,
        startDate: Date,
        endDate: Date
    ): Int
}
