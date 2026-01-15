package com.uoa.ml.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.core.database.repository.AIModelInputRepository
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.mlclassifier.data.InferenceResult
import com.uoa.core.notifications.VehicleNotificationManager
import com.uoa.ml.domain.RunClassificationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TripClassificationDebugViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tripRepository: TripDataRepository,
    private val aiModelInputRepository: AIModelInputRepository,
    private val runClassificationUseCase: RunClassificationUseCase
) : ViewModel() {

    private val notificationManager = VehicleNotificationManager(context)

    fun runLatestTripClassification() {
        viewModelScope.launch {
            val resultMessage = withContext(Dispatchers.IO) {
                val trips = tripRepository.getAllTrips()
                val latestTrip = trips.maxByOrNull { it.endTime ?: it.startTime }
                    ?: return@withContext "No trips found to classify."
                if (latestTrip.endTime == null) {
                    return@withContext "Trip ${latestTrip.id} has no end time. End the trip first."
                }

                val beforeCount = aiModelInputRepository
                    .getAiModelInputInputByTripId(latestTrip.id)
                    .size

                val inference = runClassificationUseCase.invoke(latestTrip.id)

                val afterCount = aiModelInputRepository
                    .getAiModelInputInputByTripId(latestTrip.id)
                    .size

                when (inference) {
                    is InferenceResult.Success -> {
                        val label = if (inference.isAlcoholInfluenced) "alcohol" else "no influence"
                        val prob = inference.probability?.let {
                            String.format(Locale.US, "%.2f", it)
                        } ?: "n/a"
                        "Trip ${latestTrip.id}: $label (p=$prob). AI inputs $beforeCount -> $afterCount"
                    }
                    is InferenceResult.NotEnoughData -> {
                        "Trip ${latestTrip.id}: not enough data. AI inputs $beforeCount -> $afterCount"
                    }
                    is InferenceResult.Failure -> {
                        "Trip ${latestTrip.id}: classification failed (${inference.error.message}). " +
                            "AI inputs $beforeCount -> $afterCount"
                    }
                }
            }

            Log.i("TripDebug", resultMessage)
            notificationManager.displayNotification("Debug Trip ML Check", resultMessage)
        }
    }
}
