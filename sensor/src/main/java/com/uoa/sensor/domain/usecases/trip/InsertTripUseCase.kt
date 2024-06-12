package com.uoa.sensor.domain.usecases.trip

import com.uoa.sensor.data.model.Trip
import com.uoa.sensor.data.repository.TripDataRepository
import java.util.UUID
import javax.inject.Inject

class InsertTripUseCase @Inject constructor(private val tripRepository: TripDataRepository) {
    suspend operator fun invoke(trip: Trip) {
        return tripRepository.insertTrip(trip)
    }
}