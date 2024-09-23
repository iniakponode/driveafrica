package com.uoa.core.database.repository

import com.uoa.core.database.entities.CauseEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface CauseRepository {
    suspend fun getCauseByUnsafeBehaviourId(id: UUID): Flow<List<CauseEntity>>

    suspend fun insertCause(cause: CauseEntity)

    suspend fun batchInsertCauses(causes: List<CauseEntity>)

    suspend fun updateCause(cause: CauseEntity)

    suspend fun deleteCauseByUnsafeBehaviourId(id: UUID)
}