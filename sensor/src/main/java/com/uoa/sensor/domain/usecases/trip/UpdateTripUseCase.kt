package com.uoa.sensor.domain.usecases.trip

import com.uoa.core.database.entities.TripEntity
import com.uoa.sensor.data.model.Trip
import com.uoa.sensor.data.repository.TripDataRepository
import com.uoa.sensor.data.toEntity
import javax.inject.Inject

class UpdateTripUseCase @Inject constructor(private val tripRepository: TripDataRepository) {
    suspend operator fun invoke(trip: Trip) {
        tripRepository.updateTrip(trip)
    }
}

