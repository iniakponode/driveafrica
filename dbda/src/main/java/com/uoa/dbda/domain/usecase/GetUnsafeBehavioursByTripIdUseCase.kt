package com.uoa.dbda.domain.usecase

import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.dbda.repository.UnsafeBehaviourRepositoryImpl
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class GetUnsafeBehavioursByTripIdUseCase(private val repository: UnsafeBehaviourRepositoryImpl) {

    suspend fun execute(tripId: UUID): Flow<List<UnsafeBehaviourModel>> {
        return repository.getUnsafeBehavioursByTripId(tripId)
    }
}
