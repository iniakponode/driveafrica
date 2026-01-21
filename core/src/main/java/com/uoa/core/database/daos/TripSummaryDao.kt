package com.uoa.core.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.uoa.core.database.entities.TripSummaryBehaviourEntity
import com.uoa.core.database.entities.TripSummaryEntity
import com.uoa.core.database.entities.TripSummaryWithBehaviours
import java.util.Date
import java.util.UUID

@Dao
interface TripSummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTripSummary(tripSummary: TripSummaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTripSummaryBehaviours(behaviours: List<TripSummaryBehaviourEntity>)

    @Query("DELETE FROM trip_summary_behaviour WHERE tripId = :tripId")
    suspend fun deleteTripSummaryBehavioursByTripId(tripId: UUID)

    @Transaction
    suspend fun upsertTripSummaryWithBehaviours(
        tripSummary: TripSummaryEntity,
        behaviours: List<TripSummaryBehaviourEntity>
    ) {
        insertTripSummary(tripSummary)
        deleteTripSummaryBehavioursByTripId(tripSummary.tripId)
        if (behaviours.isNotEmpty()) {
            insertTripSummaryBehaviours(behaviours)
        }
    }

    @Transaction
    @Query("SELECT * FROM trip_summary WHERE tripId = :tripId LIMIT 1")
    suspend fun getTripSummaryByTripId(tripId: UUID): TripSummaryWithBehaviours?

    @Transaction
    @Query(
        "SELECT * FROM trip_summary WHERE driverId = :driverId AND startDate BETWEEN :startDate AND :endDate"
    )
    suspend fun getTripSummariesByDriverAndDateRange(
        driverId: UUID,
        startDate: Date,
        endDate: Date
    ): List<TripSummaryWithBehaviours>

    @Transaction
    @Query("SELECT * FROM trip_summary WHERE sync = 0")
    suspend fun getUnsyncedTripSummaries(): List<TripSummaryWithBehaviours>

    @Query("UPDATE trip_summary SET sync = :synced WHERE tripId IN (:tripIds)")
    suspend fun markTripSummariesSynced(tripIds: List<UUID>, synced: Boolean)

    @Query("SELECT COUNT(*) FROM trip_summary WHERE driverId = :driverId AND startDate BETWEEN :startDate AND :endDate")
    suspend fun countTripSummariesByDriverAndDateRange(
        driverId: UUID,
        startDate: Date,
        endDate: Date
    ): Int
}
