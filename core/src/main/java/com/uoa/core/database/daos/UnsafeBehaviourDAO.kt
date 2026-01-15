package com.uoa.core.database.daos

import android.util.Log
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.uoa.core.database.entities.LocationEntity
import com.uoa.core.database.entities.UnsafeBehaviourEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.Date
import java.util.UUID

@Dao
interface UnsafeBehaviourDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnsafeBehaviour(unsafeBehaviour: UnsafeBehaviourEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnsafeBehaviourBatch(unsafeBehaviours: List<UnsafeBehaviourEntity>)

    @Update
    suspend fun updateUnsafeBehaviour(unsafeBehaviour: UnsafeBehaviourEntity)

    @Delete
    suspend fun deleteUnsafeBehaviour(unsafeBehaviour: UnsafeBehaviourEntity)

    @Query("SELECT * FROM unsafe_behaviour WHERE tripId = :tripID")
    fun getUnsafeBehavioursByTripId(tripID: UUID): Flow<List<UnsafeBehaviourEntity>>

    @Query("SELECT * FROM unsafe_behaviour WHERE locationId = :locationId AND sync = :synced AND processed= :procd")
     fun getUnsafeBehavioursByLocationIdAndSyncStatus(locationId: UUID, synced: Boolean, procd: Boolean): List<UnsafeBehaviourEntity>

    @Query("SELECT * FROM unsafe_behaviour ORDER BY id DESC LIMIT 20")
    fun getUnsafeBehavioursForTips(): Flow<List<UnsafeBehaviourEntity>>

    @Query("SELECT * FROM unsafe_behaviour WHERE id = :id LIMIT 1")
    suspend fun getUnsafeBehaviourById(id: UUID): UnsafeBehaviourEntity?

    // Custom SQL query to update specific fields where updated = false
    @Query(" UPDATE unsafe_behaviour " +
            "SET updatedAt = :updatedAt, " +
            "updated = :updated " +
            "WHERE id IN (:ids) AND updated = 0"
    )
    suspend fun updateUnsafeBehaviourFields(
        updatedAt: Date,
        updated: Boolean,
        ids: List<UUID>
    )

    @Transaction
    suspend fun updateUnsafeBehavioursTransaction(
        unsafeBehaviours: List<UnsafeBehaviourEntity>,
        alcoholInf: Boolean
    ) {
        try {
            Log.d("UnsafeBehaviourDao", "Unsafe Behaviours for batch update. $unsafeBehaviours")

            if (unsafeBehaviours.isNotEmpty()) {
                // Extract the list of IDs to update
                val ids = unsafeBehaviours.map { it.id }

                // Use the current timestamp for the 'updatedAt' field
                val updatedAt = Date()

                // Perform the batch update using the optimized SQL query
                updateUnsafeBehaviourFields(
                    updatedAt = updatedAt,
                    updated = true,
                    ids = ids
                )

                Log.d("UnsafeBehaviourDao", "Batch update executed for ${ids.size} entities.")
            } else {
                Log.d("UnsafeBehaviourDao", "No entities were found to update.")
            }
        } catch (e: Exception) {
            Log.e("UnsafeBehaviourDaoError", "Error during batch update", e)
            // Implement additional error handling: Retry, rollback, notify, etc.
        }
    }

    @Query("SELECT * FROM unsafe_behaviour WHERE updated = 0 AND tripId = :tripID")
    fun getEntitiesToBeUpdated(tripID: UUID): Flow<List<UnsafeBehaviourEntity>>

    // The @Update method now returns the number of rows updated
    @Update
    suspend fun updateUnsafeBehavioursBatch(unsafeBehaviours: List<UnsafeBehaviourEntity>): Int

    // Function to handle batch update with filtering and error handling
    suspend fun updateUnsafeBehaviours(unsafeBehaviours: List<UnsafeBehaviourEntity>) {
        try {
            // Filter the entities to only those that need to be updated (updated = false)

            if (unsafeBehaviours.isNotEmpty()) {
                // Call the update function and capture the number of rows affected
                val rowsUpdated = updateUnsafeBehavioursBatch(unsafeBehaviours)

                // Log the number of rows that were actually updated
                Log.d("UnsafeBehaviourDao", "Number of entities updated: $rowsUpdated")

                // Optionally, log the entities that were attempted to update
                Log.d("UnsafeBehaviourDao", "Entities to update: $unsafeBehaviours")
            } else {
                Log.d("UnsafeBehaviourDao", "No entities were found to update.")
            }
        } catch (e: Exception) {
            Log.e("UnsafeBehaviourDaoError", "Error during batch update", e)
            // Implement additional error handling: Retry, rollback, notify, etc.
        }
    }

    @Query("SELECT * FROM unsafe_behaviour WHERE sync = :synced")
    suspend fun getUnsafeBehavioursBySyncStatus(synced: Boolean): List<UnsafeBehaviourEntity>

    @Query("SELECT * FROM unsafe_behaviour WHERE sync= :synced AND processed= :processed")
    suspend fun getUnsafeBehaviourBySyncAndProcessedStatus(synced: Boolean, processed: Boolean): List<UnsafeBehaviourEntity>


    @Query("DELETE FROM unsafe_behaviour WHERE sync = :synced")
    suspend fun deleteAllUnsafeBehavioursBySyncStatus(synced: Boolean)

    @Query("DELETE FROM unsafe_behaviour")
    suspend fun deleteAllUnsafeBehaviours()

    @Query("DELETE FROM unsafe_behaviour WHERE id IN (:ids)")
    suspend fun deleteUnsafeBehavioursByIds(ids: List<UUID>)

    @Query("DELETE FROM unsafe_behaviour WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteUnsafeBehavioursBefore(cutoffTimestamp: Long)

    @Query("SELECT DISTINCT locationId FROM unsafe_behaviour WHERE locationId IS NOT NULL AND timestamp >= :cutoffTimestamp")
    suspend fun getLocationIdsWithUnsafeBehavioursSince(cutoffTimestamp: Long): List<UUID>

    @Query("SELECT COUNT(*) FROM unsafe_behaviour WHERE behaviorType = :behaviorType AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getUnsafeBehaviourCountByTypeAndTime(behaviorType: String, startTime: Long, endTime: Long): Int

    @Query("SELECT COUNT(*) FROM unsafe_behaviour WHERE behaviorType = :behaviourType AND tripId = :tripId GROUP BY :total_distance/1000")
    suspend fun getUnsafeBehaviourCountByTypeAndDistance(behaviourType: String, tripId: UUID, total_distance: Float): Int

    @Query("SELECT * FROM unsafe_behaviour WHERE date BETWEEN :startDate AND :endDate")
    fun getUnsafeBehavioursBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<UnsafeBehaviourEntity>>

    // get last Inserted UnsafeBehaviour
    @Query("SELECT * FROM unsafe_behaviour ORDER BY id DESC LIMIT 1")
    suspend fun getLastInsertedUnsafeBehaviour(): UnsafeBehaviourEntity
}
