package com.uoa.sensor.domain.usecases.trip

import android.util.Log
import com.uoa.core.database.repository.TripDataRepository
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

class UpdateTripUseCase @Inject constructor(private val tripRepository: TripDataRepository) {
    suspend operator fun invoke(
                        tripId: UUID,
                        influence:String
    ) {
        Log.i("Checked", "I am called to Update Trip, TripId: $tripId")
//        CoroutineScope(Dispatchers.IO).launch {
            val tripToFromdb = tripRepository.getTripById(tripId)
            if (tripToFromdb!=null){
                val tripToUpdate = tripToFromdb.copy(
                    endTime = System.currentTimeMillis(),
                    endDate = Date(),
                    influence=influence,
                    sync = false
                )
                tripRepository.updateTrip(tripToUpdate)
                Log.i("Trip", "Update successful")
            }
            else{
                Log.i("Trip", "No Trip to update")
            }

//        }
        Log.i("TripID", "Trip ended with id: $tripId")
    }
}

class GetTripByIdUseCase @Inject constructor(private val tripRepository: TripDataRepository){
    suspend operator fun invoke(tripId: UUID): Trip? {
        return tripRepository.getTripById(tripId)
    }
}

