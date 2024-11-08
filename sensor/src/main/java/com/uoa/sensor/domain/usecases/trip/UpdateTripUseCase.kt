package com.uoa.sensor.domain.usecases.trip

import android.util.Log
import com.uoa.core.model.Trip
import com.uoa.sensor.repository.TripDataRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class UpdateTripUseCaseOld @Inject constructor(private val tripRepository: TripDataRepositoryImpl) {
    suspend operator fun invoke(trip: Trip) {
        val tripToUpdate=trip.copy(
            endTime = System.currentTimeMillis(),
            endDate = Date()
        )
        tripRepository.updateTrip(tripToUpdate)
    }
}

class UpdateTripUseCase @Inject constructor(private val tripRepository: TripDataRepositoryImpl) {
    operator fun invoke(
                        tripId: UUID,
                        influence:String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val tripToFromdb = tripRepository.getTripById(tripId)
            val tripToUpdate = tripToFromdb!!.copy(
                endTime = System.currentTimeMillis(),
                endDate = Date(),
                influence=influence
            )
            tripRepository.updateTrip(tripToUpdate)
        }
        Log.d("TripID", "Trip ended with id: $tripId")
    }
}

