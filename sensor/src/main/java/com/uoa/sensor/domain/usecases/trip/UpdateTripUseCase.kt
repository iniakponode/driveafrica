package com.uoa.sensor.domain.usecases.trip

import android.util.Log
import com.uoa.core.database.repository.AIModelInputRepository
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.database.repository.TripSummaryRepository
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.core.model.SyncState
import com.uoa.core.model.Trip
import com.uoa.core.utils.buildTripSummary
import kotlinx.coroutines.flow.first
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

class UpdateTripUseCase @Inject constructor(
    private val tripRepository: TripDataRepository,
    private val tripSummaryRepository: TripSummaryRepository,
    private val locationRepository: LocationRepository,
    private val unsafeBehaviourRepository: UnsafeBehaviourRepository,
    private val aiModelInputRepository: AIModelInputRepository
) {
    suspend operator fun invoke(
                        tripId: UUID,
                        influence:String,
                        alcoholProbability: Float? = null,
                        userAlcoholResponse: String? = null
    ) {
        Log.i("Checked", "I am called to Update Trip, TripId: $tripId")
//        CoroutineScope(Dispatchers.IO).launch {
            val tripToFromdb = tripRepository.getTripById(tripId)
            if (tripToFromdb!=null){
                val tripToUpdate = tripToFromdb.copy(
                    endTime = System.currentTimeMillis(),
                    endDate = Date(),
                    influence=influence,
                    sync = false,
                    alcoholProbability = alcoholProbability,
                    userAlcoholResponse = userAlcoholResponse ?: tripToFromdb.userAlcoholResponse
                )
                tripRepository.updateTrip(tripToUpdate)
                Log.i("Trip", "Update successful")

                try {
                    if (tripToUpdate.driverPId == null) {
                        Log.w("TripSummary", "Skipping summary generation; missing driver id for $tripId")
                    } else {
                        val locations = locationRepository.getLocationDataByTripId(tripId)
                        val unsafeBehaviours = unsafeBehaviourRepository
                            .getUnsafeBehavioursByTripId(tripId)
                            .first()
                        val summary = buildTripSummary(tripToUpdate, locations, unsafeBehaviours)
                        tripSummaryRepository.insertTripSummary(summary)
                        tripRepository.updateTrip(tripToUpdate.copy(syncState = SyncState.SUMMARY_READY))
                        Log.i("TripSummary", "Trip summary saved for $tripId")
                    }
                } catch (e: Exception) {
                    Log.e("TripSummary", "Failed to build summary for $tripId: ${e.message}", e)
                }
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

