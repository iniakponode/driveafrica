package com.uoa.sensor.domain.usecases.trip

import com.uoa.core.model.Trip
import com.uoa.sensor.repository.TripDataRepositoryImpl
import javax.inject.Inject

class InsertTripUseCase @Inject constructor(private val tripRepository: TripDataRepositoryImpl) {
    suspend operator fun invoke(trip: Trip) {
        return tripRepository.insertTrip(trip)
    }
}