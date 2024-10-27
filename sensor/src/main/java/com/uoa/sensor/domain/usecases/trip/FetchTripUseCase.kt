package com.uoa.sensor.domain.usecases.trip

import com.uoa.sensor.repository.TripDataRepositoryImpl
import java.util.UUID
import javax.inject.Inject

class FetchTripUseCase @Inject constructor(private val tripRepository: TripDataRepositoryImpl) {
    suspend operator fun invoke(value: UUID) = tripRepository.getTripById(value)
}