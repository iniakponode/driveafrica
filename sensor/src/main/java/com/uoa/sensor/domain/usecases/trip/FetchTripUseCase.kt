package com.uoa.sensor.domain.usecases.trip

import com.uoa.sensor.data.repository.TripDataRepository
import javax.inject.Inject

class FetchTripUseCase @Inject constructor(private val tripRepository: TripDataRepository) {
    suspend operator fun invoke(value: Long) = tripRepository.getTripById(value)
}