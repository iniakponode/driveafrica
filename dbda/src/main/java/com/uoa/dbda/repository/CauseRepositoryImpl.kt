package com.uoa.dbda.repository

import android.util.Log
import com.uoa.core.database.daos.CauseDao
import com.uoa.core.database.entities.CauseEntity
import com.uoa.core.database.repository.CauseRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class CauseRepositoryImpl @Inject constructor(private val causeDao: CauseDao
): CauseRepository {
    // Add implementation here
    override suspend fun getCauseByUnsafeBehaviourId(id: UUID): Flow<List<CauseEntity>> {
        return causeDao.getCauseByUnsafeBehaviourId(id)
    }

    override suspend fun insertCause(cause: CauseEntity) {
        Log.d("InsertCause", "Inserting cause with unsafeBehaviourId: ${cause.unsafeBehaviourId}")
        causeDao.insertCause(cause)
    }

    override suspend fun batchInsertCauses(causes: List<CauseEntity>) {
        causeDao.batchInsertCauses(causes)
    }


    override suspend fun updateCause(cause: CauseEntity) {
        causeDao.updateCause(cause)
    }

    override suspend fun deleteCauseByUnsafeBehaviourId(id: UUID) {
        causeDao.deleteCauseByUnsafeBehaviourId(id)
    }


}