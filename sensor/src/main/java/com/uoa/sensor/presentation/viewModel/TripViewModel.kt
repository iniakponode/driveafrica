package com.uoa.sensor.presentation.viewModel
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.core.mlclassifier.data.InferenceResult
import com.uoa.core.model.Trip
import com.uoa.ml.domain.BatchInsertCauseUseCase
import com.uoa.ml.domain.BatchUpDateUnsafeBehaviourCauseUseCase
import com.uoa.ml.domain.RunClassificationUseCase
import com.uoa.ml.domain.SaveInfluenceToCause
import com.uoa.ml.domain.UpDateUnsafeBehaviourCauseUseCase
import com.uoa.sensor.domain.usecases.trip.FetchTripUseCase
import com.uoa.sensor.domain.usecases.trip.InsertTripUseCase
import com.uoa.sensor.domain.usecases.trip.UpdateTripUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlin.invoke

@HiltViewModel
class TripViewModel @Inject constructor(
    private val insertTripUseCase: InsertTripUseCase,
    private val updateTripUseCase: UpdateTripUseCase,
    private val runClassificationUseCase: RunClassificationUseCase,
    private val upDateUnsafeBehaviourCauseUseCase: UpDateUnsafeBehaviourCauseUseCase,
    private val saveInfluenceToCause: SaveInfluenceToCause,
    private val batchInsertCauseUseCase: BatchInsertCauseUseCase,
    private val batchUpDateUnsafeBehaviourCauseUseCase: BatchUpDateUnsafeBehaviourCauseUseCase,

) : ViewModel() {

    private val _currentTripId = MutableStateFlow<UUID?>(null)
    val currentTripId: StateFlow<UUID?> get() = _currentTripId

    fun startTrip(driverProfileId: UUID?, tripId: UUID){
        viewModelScope.launch {
            Log.i("TripViewModel", "StartTrip called for tripId: $tripId")
            val startTime = System.currentTimeMillis()
            val trip = Trip(driverPId = driverProfileId,
                startTime = startTime, endTime = null,
                startDate = Date(),
                endDate = null,
                id = tripId,
                influence = ""
            )
            insertTripUseCase(trip)
            _currentTripId.value = tripId
            _currentTripId.emit(tripId)
            Log.i("TripID", "Trip started with id: $tripId")
        }
    }

    fun updateTripId(tripId: UUID) {
        _currentTripId.value = tripId
    }

    fun clearTripID() {
        _currentTripId.value = null
    }


    /**
     * Ends a trip by performing classification and updating trip status based on alcohol influence.
     *
     * @param tripId The UUID of the trip to end.
     */
    fun endTrip(tripId: UUID) {
        viewModelScope.launch {
            Log.i("TripViewModel", "endTrip called for tripId: $tripId")

            // Step 1: Run classification using the ML Usecase Update Trip invoke function
            when (val inferenceResult = runClassificationUseCase.invoke(tripId)) {
                is InferenceResult.Success -> {
                    val alcInfluence = inferenceResult.alcoholInfluence
                    Log.i("TripViewModel", "Alcohol Influence Detected: $alcInfluence")

                    // Step 2: Update trip status based on classification result
                    if (alcInfluence) {
                        try {
                            updateTripUseCase.invoke(tripId, "alcohol")
                            Log.i("TripViewModel", "Trip updated with 'alcohol' influence successfully.")
                        } catch (updateException: Exception) {
                            Log.e("TripViewModel", "Failed to update trip with 'alcohol' influence: ${updateException.message}", updateException)
                            // Optional: Handle update failure (e.g., retry, notify user)
                        }
                    } else {
                        try {
                            updateTripUseCase.invoke(tripId, "No influence")
                            Log.i("TripViewModel", "Trip updated with 'No influence' successfully.")
                        } catch (updateException: Exception) {
                            Log.e("TripViewModel", "Failed to update trip with 'No influence': ${updateException.message}", updateException)
                            // Optional: Handle update failure (e.g., retry, notify user)
                        }
                    }

                    // Optional: Additional actions based on alcohol influence
                    Log.i("TripViewModel", "Alcohol Influence: $alcInfluence")
                    // batchUpDateUnsafeBehaviourCauseUseCase.invoke(tripId, alcInfluence)
                    Log.i("TripViewModel", "Influence updated to Unsafe Behaviour table")
                }

                is InferenceResult.Failure -> {
                    val error = inferenceResult.error
                    Log.e("TripViewModel", "Classification failed for tripId: $tripId with error: ${error.message}", error)

                    // Step 3: Handle inference failure
                    // Decide how to handle failures: retry, notify user, mark trip with error status, etc.
                    // Example: Notify user via UI (implementation depends on your architecture)
                    // showErrorToUser("Failed to analyze trip. Please try again.")
                }
            }
        }
    }
}


