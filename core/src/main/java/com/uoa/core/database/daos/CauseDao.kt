package com.uoa.core.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.uoa.core.database.entities.CauseEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface CauseDao {
    // This DAO is used to interact with the cause entity

    // Insert a cause
    @Insert
    fun insertCause(causeEntity: CauseEntity)
//    Insert Causes in Batch
    @Insert
    suspend fun batchInsertCauses(causes: List<CauseEntity>)

    // Update a cause
    @Update
    fun updateCause(causeEntity: CauseEntity)

    // Get CauseRepository by unsafe behaviour id
    @Query("SELECT * FROM causes WHERE unsafeBehaviourId = :unsafeBehaviourId")
    fun getCauseByUnsafeBehaviourId(unsafeBehaviourId: UUID): Flow<List<CauseEntity>>

    // Delete all causes
    @Query("DELETE FROM causes")
    fun deleteAllCauses()

    // Get delete cause by unsafe behaviour id
    @Query("DELETE FROM causes WHERE unsafeBehaviourId = :unsafeBehaviourId")
    fun deleteCauseByUnsafeBehaviourId(unsafeBehaviourId: UUID)
}