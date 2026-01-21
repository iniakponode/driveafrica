package com.uoa.ml.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.mlclassifier.data.InferenceResult
import com.uoa.core.mlclassifier.OnnxModelRunner
import com.uoa.core.mlclassifier.data.ModelInference
import com.uoa.core.mlclassifier.data.TripFeatures
import com.uoa.core.model.Trip
import com.uoa.core.notifications.VehicleNotificationManager
import com.uoa.ml.domain.RunClassificationUseCase
import com.uoa.ml.domain.TripClassificationDiagnostics
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class TripClassificationDebugUiState(
    val isRunning: Boolean = false,
    val statusMessage: String? = null,
    val diagnostics: TripClassificationDiagnostics? = null,
    val trips: List<Trip> = emptyList(),
    val selectedTripId: UUID? = null,
    val tripsLoading: Boolean = false,
    val tripsMessage: String? = null,
    val totalTripCount: Int = 0,
    val sanityRunning: Boolean = false,
    val sanityMessage: String? = null,
    val sanityResults: List<SanityCheckResult> = emptyList()
)

data class SanityCheckResult(
    val label: String,
    val features: TripFeatures,
    val inference: ModelInference?,
    val errorMessage: String? = null
)

@HiltViewModel
class TripClassificationDebugViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tripRepository: TripDataRepository,
    private val runClassificationUseCase: RunClassificationUseCase,
    private val onnxModelRunner: OnnxModelRunner
) : ViewModel() {

    private val notificationManager = VehicleNotificationManager(context)
    private val _uiState = MutableStateFlow(TripClassificationDebugUiState())
    val uiState: StateFlow<TripClassificationDebugUiState> = _uiState.asStateFlow()

    fun loadTrips() {
        viewModelScope.launch {
            _uiState.update { it.copy(tripsLoading = true, tripsMessage = null) }
            val updatedState = withContext(Dispatchers.IO) {
                val trips = tripRepository.getAllTrips()
                val sorted = trips.sortedByDescending { it.endTime ?: it.startTime }
                val trimmed = sorted.take(25)
                val selected = _uiState.value.selectedTripId ?: trimmed.firstOrNull()?.id
                _uiState.value.copy(
                    trips = trimmed,
                    selectedTripId = selected,
                    tripsLoading = false,
                    tripsMessage = if (sorted.isEmpty()) "No trips found." else null,
                    totalTripCount = sorted.size
                )
            }
            _uiState.value = updatedState
        }
    }

    fun selectTrip(tripId: UUID) {
        _uiState.update {
            it.copy(
                selectedTripId = tripId,
                diagnostics = null,
                statusMessage = null
            )
        }
    }

    fun runSelectedTripClassification() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRunning = true, statusMessage = null) }
            val nextState = withContext(Dispatchers.IO) {
                val selectedTripId = _uiState.value.selectedTripId
                    ?: return@withContext _uiState.value.copy(
                        isRunning = false,
                        statusMessage = "Select a trip to classify."
                    )
                val selectedTrip = _uiState.value.trips.firstOrNull { it.id == selectedTripId }
                if (selectedTrip == null) {
                    return@withContext _uiState.value.copy(
                        isRunning = false,
                        statusMessage = "Selected trip not found. Refresh the trip list."
                    )
                }
                if (selectedTrip.endTime == null) {
                    return@withContext _uiState.value.copy(
                        isRunning = false,
                        statusMessage = "Trip ${selectedTrip.id} has no end time. End the trip first."
                    )
                }

                val diagnostics = runClassificationUseCase.runWithDiagnostics(selectedTripId)
                val message = buildSummaryMessage(diagnostics)
                logTripDiagnostics(diagnostics)
                _uiState.value.copy(
                    isRunning = false,
                    statusMessage = message,
                    diagnostics = diagnostics
                )
            }

            _uiState.value = nextState
            nextState.statusMessage?.let {
                notificationManager.displayNotification("Debug Trip ML Check", it)
            }
        }
    }

    fun runSanityCheck() {
        viewModelScope.launch {
            _uiState.update { it.copy(sanityRunning = true, sanityMessage = null) }
            val updatedState = withContext(Dispatchers.Default) {
                val cases = listOf(
                    "All zeros" to TripFeatures(
                        hourOfDayMean = 0.0f,
                        dayOfWeekMean = 0.0f,
                        speedStd = 0.0f,
                        courseStd = 0.0f,
                        accelerationYOriginalMean = 0.0f
                    ),
                    "Typical" to TripFeatures(
                        hourOfDayMean = 12.0f,
                        dayOfWeekMean = 3.0f,
                        speedStd = 3.0f,
                        courseStd = 80.0f,
                        accelerationYOriginalMean = 0.65f
                    ),
                    "High variance" to TripFeatures(
                        hourOfDayMean = 23.0f,
                        dayOfWeekMean = 6.0f,
                        speedStd = 25.0f,
                        courseStd = 200.0f,
                        accelerationYOriginalMean = 2.0f
                    )
                )
                val results = cases.map { (label, features) ->
                    try {
                        val inference = onnxModelRunner.runInference(features)
                        logSanityResult(label, features, inference, null)
                        SanityCheckResult(label = label, features = features, inference = inference)
                    } catch (e: Exception) {
                        logSanityResult(label, features, null, e)
                        SanityCheckResult(
                            label = label,
                            features = features,
                            inference = null,
                            errorMessage = e.message ?: "Inference error"
                        )
                    }
                }
                val message = if (results.all { it.inference == null }) {
                    "Sanity check failed for all test inputs."
                } else {
                    null
                }
                _uiState.value.copy(
                    sanityRunning = false,
                    sanityMessage = message,
                    sanityResults = results
                )
            }
            _uiState.value = updatedState
        }
    }

    private fun buildSummaryMessage(diagnostics: TripClassificationDiagnostics): String {
        return when (val inference = diagnostics.inferenceResult) {
            is InferenceResult.Success -> {
                val label = if (inference.isAlcoholInfluenced) "alcohol" else "no influence"
                val prob = inference.probability?.let { String.format(Locale.US, "%.2f", it) } ?: "n/a"
                "Trip ${diagnostics.tripId}: $label (p=$prob). " +
                    "AI inputs ${diagnostics.aiInputsBefore} -> ${diagnostics.aiInputsAfter}"
            }
            is InferenceResult.NotEnoughData -> {
                val reasonSummary = diagnostics.notEnoughReasons
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString { it.title }
                    ?: "Not enough data"
                "Trip ${diagnostics.tripId}: $reasonSummary. " +
                    "AI inputs ${diagnostics.aiInputsBefore} -> ${diagnostics.aiInputsAfter}"
            }
            is InferenceResult.Failure -> {
                val errorMessage = inference.error.message ?: "unknown error"
                "Trip ${diagnostics.tripId}: classification failed ($errorMessage). " +
                    "AI inputs ${diagnostics.aiInputsBefore} -> ${diagnostics.aiInputsAfter}"
            }
        }
    }

    private fun logTripDiagnostics(diagnostics: TripClassificationDiagnostics) {
        val inference = diagnostics.inferenceResult
        val label = when (inference) {
            is InferenceResult.Success ->
                if (inference.isAlcoholInfluenced) "alcohol" else "no influence"
            is InferenceResult.NotEnoughData -> "not enough data"
            is InferenceResult.Failure -> "failed"
        }
        Log.i(
            "TripML",
            "Trip debug output tripId=${diagnostics.tripId} label=$label " +
                "prob=${(inference as? InferenceResult.Success)?.probability ?: "n/a"} " +
                "raw=${formatFloatArray(diagnostics.rawProbabilities)} " +
                "normalized=${formatFloatArray(diagnostics.normalizedProbabilities)}"
        )
    }

    private fun logSanityResult(
        label: String,
        features: TripFeatures,
        inference: ModelInference?,
        error: Throwable?
    ) {
        if (error != null) {
            Log.w("TripML", "Sanity check $label failed: ${error.message}")
            return
        }
        Log.i(
            "TripML",
            "Sanity check $label input=${formatFeatures(features)} " +
                "label=${inference?.rawLabel ?: "n/a"} " +
                "prob=${inference?.probability ?: "n/a"} " +
                "raw=${formatFloatArray(inference?.rawProbabilities)} " +
                "normalized=${formatFloatArray(inference?.normalizedProbabilities)}"
        )
    }

    private fun formatFeatures(features: TripFeatures): String {
        return listOf(
            features.dayOfWeekMean,
            features.hourOfDayMean,
            features.accelerationYOriginalMean,
            features.courseStd,
            features.speedStd
        ).joinToString(prefix = "[", postfix = "]") { value ->
            String.format(Locale.US, "%.4f", value)
        }
    }

    private fun formatFloatArray(values: FloatArray?): String {
        return values?.joinToString(prefix = "[", postfix = "]") { value ->
            String.format(Locale.US, "%.4f", value)
        } ?: "n/a"
    }
}
