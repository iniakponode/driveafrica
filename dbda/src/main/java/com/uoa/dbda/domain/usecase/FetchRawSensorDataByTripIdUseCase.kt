package com.uoa.dbda.domain.usecase

import com.uoa.dbda.repository.UnsafeBehaviourRepository
import java.util.UUID
import javax.inject.Inject

class FetchRawSensorDataByTripId @Inject constructor(
    private val unsafeBehaviourRepository: UnsafeBehaviourRepository
) {
    suspend fun execute(tripId: UUID) {
        unsafeBehaviourRepository.getSensorDataByTripId(tripId)
    }
}