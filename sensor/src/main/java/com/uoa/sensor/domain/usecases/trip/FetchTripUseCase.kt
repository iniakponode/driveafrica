package com.uoa.sensor.domain.usecases.trip

import com.uoa.sensor.data.repository.TripDataRepository
import java.util.UUID
import javax.inject.Inject

class FetchTripUseCase @Inject constructor(private val tripRepository: TripDataRepository) {
    suspend operator fun invoke(value: UUID) = tripRepository.getTripById(value)
}