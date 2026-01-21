package com.uoa.safedriveafrica.presentation.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uoa.core.mlclassifier.data.InferenceResult
import com.uoa.core.mlclassifier.data.ModelInference
import com.uoa.core.mlclassifier.data.TripFeatures
import com.uoa.core.model.Trip
import com.uoa.ml.domain.TripClassificationDiagnostics
import com.uoa.ml.presentation.viewmodel.SanityCheckResult
import com.uoa.ml.presentation.viewmodel.TripClassificationDebugUiState
import com.uoa.ml.presentation.viewmodel.TripClassificationDebugViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TripMlDebugRoute(onNavigateBack: () -> Unit) {
    val viewModel: TripClassificationDebugViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.loadTrips()
    }
    TripMlDebugScreen(
        state = state,
        onRunCheck = { viewModel.runSelectedTripClassification() },
        onRefreshTrips = { viewModel.loadTrips() },
        onSelectTrip = { viewModel.selectTrip(it) },
        onRunSanityCheck = { viewModel.runSanityCheck() },
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun TripMlDebugScreen(
    state: TripClassificationDebugUiState,
    onRunCheck: () -> Unit,
    onRefreshTrips: () -> Unit,
    onSelectTrip: (java.util.UUID) -> Unit,
    onRunSanityCheck: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Trip ML Debug",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onNavigateBack) {
                Text(text = "Back")
            }
        }

        Button(
            onClick = onRunCheck,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Run Selected Trip ML Check")
        }

        if (state.isRunning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = cardColors) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Note",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Only trips recorded after the raw-accelerometer fix will reflect updated ML inputs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        state.statusMessage?.let { message ->
            Card(modifier = Modifier.fillMaxWidth(), colors = cardColors) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Summary",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        SanityCheckSection(
            state = state,
            cardColors = cardColors,
            onRunSanityCheck = onRunSanityCheck
        )

        TripListSection(
            state = state,
            cardColors = cardColors,
            dateFormat = dateFormat,
            onRefreshTrips = onRefreshTrips,
            onSelectTrip = onSelectTrip
        )

        state.diagnostics?.let { diagnostics ->
            TripSection(diagnostics = diagnostics, dateFormat = dateFormat, cardColors = cardColors)
            CountsSection(diagnostics = diagnostics, cardColors = cardColors)
            FeatureStateSection(diagnostics = diagnostics, cardColors = cardColors)
            FeaturesSection(diagnostics = diagnostics, cardColors = cardColors)
            InferenceSection(diagnostics = diagnostics, cardColors = cardColors)
            ReasonsSection(diagnostics = diagnostics, cardColors = cardColors)
            WarningsSection(diagnostics = diagnostics, cardColors = cardColors)
        }
    }
}

@Composable
private fun SanityCheckSection(
    state: TripClassificationDebugUiState,
    cardColors: CardColors,
    onRunSanityCheck: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = cardColors
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Model sanity check",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Runs three synthetic inputs to verify the model output changes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onRunSanityCheck,
                enabled = !state.sanityRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Run Sanity Check")
            }
            if (state.sanityRunning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            state.sanityMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.sanityResults.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.sanityResults.forEach { result ->
                        SanityResultRow(result = result)
                    }
                }
            }
        }
    }
}

@Composable
private fun SanityResultRow(result: SanityCheckResult) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = result.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Input: ${formatFeatures(result.features)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val inference = result.inference
        if (inference != null) {
            Text(
                text = "Result: ${formatInference(inference)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        result.errorMessage?.let { error ->
            Text(
                text = "Error: $error",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun formatInference(inference: ModelInference): String {
    val prob = inference.probability?.let { formatFloat(it) } ?: "n/a"
    val raw = formatFloatArray(inference.rawProbabilities)
    val normalized = formatFloatArray(inference.normalizedProbabilities)
    return "label=${inference.rawLabel ?: "n/a"}, prob=$prob, raw=$raw, norm=$normalized"
}

private fun formatFeatures(features: TripFeatures): String {
    return listOf(
        features.dayOfWeekMean,
        features.hourOfDayMean,
        features.accelerationYOriginalMean,
        features.courseStd,
        features.speedStd
    ).joinToString(prefix = "[", postfix = "]") { value ->
        formatFloat(value)
    }
}

@Composable
private fun TripListSection(
    state: TripClassificationDebugUiState,
    cardColors: CardColors,
    dateFormat: SimpleDateFormat,
    onRefreshTrips: () -> Unit,
    onSelectTrip: (java.util.UUID) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = cardColors
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trips",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onRefreshTrips, enabled = !state.tripsLoading) {
                    Text(text = "Refresh")
                }
            }

            if (state.tripsLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            state.tripsMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (state.trips.isNotEmpty()) {
                Text(
                    text = "Showing ${state.trips.size} of ${state.totalTripCount} trips.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.trips.forEach { trip ->
                        TripRow(
                            trip = trip,
                            isSelected = trip.id == state.selectedTripId,
                            dateFormat = dateFormat,
                            onSelectTrip = { onSelectTrip(trip.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TripRow(
    trip: Trip,
    isSelected: Boolean,
    dateFormat: SimpleDateFormat,
    onSelectTrip: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.RadioButton(
            selected = isSelected,
            onClick = onSelectTrip
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = trip.id.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = buildTripTimeLabel(trip, dateFormat),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun buildTripTimeLabel(trip: Trip, dateFormat: SimpleDateFormat): String {
    val start = dateFormat.format(Date(trip.startTime))
    val end = trip.endTime?.let { dateFormat.format(Date(it)) } ?: "active"
    return "Start: $start | End: $end"
}

@Composable
private fun TripSection(
    diagnostics: TripClassificationDiagnostics,
    dateFormat: SimpleDateFormat,
    cardColors: CardColors
) {
    DebugSection(
        title = "Trip",
        rows = listOf(
            "Trip ID" to diagnostics.tripId.toString(),
            "Driver Profile ID" to (diagnostics.driverProfileId?.toString() ?: "missing"),
            "Start time" to dateFormat.format(Date(diagnostics.tripStartTime)),
            "End time" to dateFormat.format(Date(diagnostics.tripEndTime)),
            "Duration (sec)" to diagnostics.durationSeconds.toString(),
            "Training timezone" to diagnostics.trainingTimeZoneId,
            "AI inputs" to "${diagnostics.aiInputsBefore} -> ${diagnostics.aiInputsAfter}"
        ),
        cardColors = cardColors
    )
}

@Composable
private fun CountsSection(
    diagnostics: TripClassificationDiagnostics,
    cardColors: CardColors
) {
    DebugSection(
        title = "Counts",
        rows = listOf(
            "Locations" to diagnostics.locationCount.toString(),
            "Raw sensors (with location)" to diagnostics.rawSensorWithLocationCount.toString(),
            "Raw sensors (all)" to diagnostics.rawSensorCount.toString(),
            "Accel samples" to diagnostics.accelCount.toString(),
            "Speed samples" to diagnostics.speedCount.toString(),
            "Course samples" to diagnostics.courseCount.toString(),
            "GPS points" to diagnostics.gpsPointCount.toString()
        ),
        cardColors = cardColors
    )
}

@Composable
private fun FeatureStateSection(
    diagnostics: TripClassificationDiagnostics,
    cardColors: CardColors
) {
    val featureState = diagnostics.featureState
    val rows = if (featureState == null) {
        listOf("Status" to "No trip_feature_state row")
    } else {
        listOf(
            "Accel count" to featureState.accelCount.toString(),
            "Speed count" to featureState.speedCount.toString(),
            "Course count" to featureState.courseCount.toString(),
            "Accel mean" to formatDouble(featureState.accelMean),
            "Speed M2" to formatDouble(featureState.speedM2),
            "Course M2" to formatDouble(featureState.courseM2)
        )
    }

    DebugSection(
        title = "Feature state",
        rows = rows,
        cardColors = cardColors
    )
}

@Composable
private fun FeaturesSection(
    diagnostics: TripClassificationDiagnostics,
    cardColors: CardColors
) {
    val tripFeatures = diagnostics.tripFeatures
    val modelVector = if (tripFeatures != null) {
        "${formatFloat(tripFeatures.dayOfWeekMean)}, " +
            "${formatFloat(tripFeatures.hourOfDayMean)}, " +
            "${formatFloat(tripFeatures.accelerationYOriginalMean)}, " +
            "${formatFloat(tripFeatures.courseStd)}, " +
            "${formatFloat(tripFeatures.speedStd)}"
    } else {
        "n/a"
    }

    DebugSection(
        title = "Features",
        rows = listOf(
            "Day of week mean" to formatFloat(diagnostics.dayOfWeekMean),
            "Hour of day mean" to formatFloat(diagnostics.hourOfDayMean),
            "Accel mean" to formatFloat(diagnostics.accelMean),
            "Speed std (raw)" to formatFloat(diagnostics.speedStd),
            "Course std (raw)" to formatFloat(diagnostics.courseStd),
            "Speed std (final)" to formatFloat(diagnostics.finalSpeedStd),
            "Course std (final)" to formatFloat(diagnostics.finalCourseStd),
            "Model input order" to "day_of_week, hour_of_day, accel_y_mean, course_std, speed_std",
            "Model input vector" to modelVector
        ),
        cardColors = cardColors
    )
}

@Composable
private fun InferenceSection(
    diagnostics: TripClassificationDiagnostics,
    cardColors: CardColors
) {
    val inference = diagnostics.inferenceResult
    val label = when (inference) {
        is InferenceResult.Success ->
            if (inference.isAlcoholInfluenced) "alcohol" else "no influence"
        is InferenceResult.NotEnoughData -> "not enough data"
        is InferenceResult.Failure -> "failed"
    }
    val probability = if (inference is InferenceResult.Success) {
        inference.probability?.let { formatFloat(it) } ?: "n/a"
    } else {
        "n/a"
    }
    val error = if (inference is InferenceResult.Failure) {
        inference.error.message ?: "unknown error"
    } else {
        "n/a"
    }

    DebugSection(
        title = "Inference",
        rows = listOf(
            "Result" to label,
            "Probability" to probability,
            "Raw label" to (diagnostics.rawLabel?.toString() ?: "n/a"),
            "Raw probabilities" to formatFloatArray(diagnostics.rawProbabilities),
            "Normalized probabilities" to formatFloatArray(diagnostics.normalizedProbabilities),
            "Error" to error
        ),
        cardColors = cardColors
    )
}

@Composable
private fun ReasonsSection(
    diagnostics: TripClassificationDiagnostics,
    cardColors: CardColors
) {
    if (diagnostics.notEnoughReasons.isEmpty()) {
        return
    }
    val rows = diagnostics.notEnoughReasons.map { reason ->
        reason.title to reason.detail
    }
    DebugSection(
        title = "Not enough data reasons",
        rows = rows,
        cardColors = cardColors
    )
}

@Composable
private fun WarningsSection(
    diagnostics: TripClassificationDiagnostics,
    cardColors: CardColors
) {
    if (diagnostics.warnings.isEmpty()) {
        return
    }
    val rows = diagnostics.warnings.map { warning ->
        "Warning" to warning
    }
    DebugSection(
        title = "Warnings",
        rows = rows,
        cardColors = cardColors
    )
}

@Composable
private fun DebugSection(
    title: String,
    rows: List<Pair<String, String>>,
    cardColors: CardColors
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = cardColors
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            rows.forEach { (label, value) ->
                DebugRow(label = label, value = value)
            }
        }
    }
}

@Composable
private fun DebugRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.45f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.55f)
        )
    }
}

private fun formatFloat(value: Float?): String {
    return value?.let { String.format(Locale.US, "%.4f", it) } ?: "n/a"
}

private fun formatDouble(value: Double?): String {
    return value?.let { String.format(Locale.US, "%.4f", it) } ?: "n/a"
}

private fun formatFloatArray(values: FloatArray?): String {
    return values?.joinToString(prefix = "[", postfix = "]") { value ->
        String.format(Locale.US, "%.4f", value)
    } ?: "n/a"
}
