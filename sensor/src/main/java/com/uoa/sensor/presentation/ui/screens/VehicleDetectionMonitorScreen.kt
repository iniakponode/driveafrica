package com.uoa.sensor.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uoa.sensor.presentation.viewModel.VehicleDetectionViewModel
import com.uoa.sensor.presentation.viewModel.VehicleDetectionUiState

/**
 * Real-time vehicle detection monitoring screen
 * Shows GPS speed, variance, state, and all detection parameters
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetectionMonitorScreen(
    viewModel: VehicleDetectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vehicle Detection Monitor") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current State Card
            StateCard(state = uiState.currentState)

            // GPS Speed Card
            GpsSpeedCard(
                speedMs = uiState.speedMs,
                speedKmh = uiState.speedKmh,
                speedMph = uiState.speedMph,
                accuracy = uiState.accuracy,
                threshold = uiState.speedThresholdMph
            )

            // Motion Analysis Card
            MotionAnalysisCard(
                variance = uiState.variance,
                classification = uiState.classification,
                timerProgress = uiState.timerProgress
            )

            // Detection Thresholds Card
            ThresholdsCard(
                vehicleMin = uiState.varianceMin,
                vehicleMax = uiState.varianceMax,
                walkingThreshold = uiState.walkingThreshold,
                speedThreshold = uiState.speedThresholdMph,
                stoppedThreshold = uiState.stoppedThresholdMph
            )

            // Trip Info Card
            if (uiState.isRecording) {
                TripInfoCard(
                    tripDuration = uiState.tripDuration,
                    tripId = uiState.tripId
                )
            }
        }
    }
}

@Composable
fun StateCard(state: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (state) {
                "IDLE" -> Color(0xFFE3F2FD) // Light blue
                "VERIFYING" -> Color(0xFFFFF9C4) // Light yellow
                "RECORDING" -> Color(0xFFC8E6C9) // Light green
                "POTENTIAL_STOP" -> Color(0xFFFFE0B2) // Light orange
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CURRENT STATE",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = when (state) {
                    "IDLE" -> Color(0xFF1976D2)
                    "VERIFYING" -> Color(0xFFF57C00)
                    "RECORDING" -> Color(0xFF388E3C)
                    "POTENTIAL_STOP" -> Color(0xFFE64A19)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (state) {
                    "IDLE" -> "Waiting for motion"
                    "VERIFYING" -> "Checking GPS speed..."
                    "RECORDING" -> "🚗 Recording trip"
                    "POTENTIAL_STOP" -> "Vehicle stopped"
                    else -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun GpsSpeedCard(
    speedMs: Double,
    speedKmh: Double,
    speedMph: Double,
    accuracy: Float,
    threshold: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "📍 GPS SPEED",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Speed in m/s
            SpeedRow(
                label = "Speed (m/s)",
                value = "%.2f m/s".format(speedMs),
                subtitle = "Raw GPS value"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Speed in km/h
            SpeedRow(
                label = "Speed (km/h)",
                value = "%.1f km/h".format(speedKmh),
                subtitle = "Metric"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Speed in mph - HIGHLIGHTED
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF59D) // Highlight yellow
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Speed (mph)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "⬅ COMPARE WITH DASHBOARD",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE65100), // Orange
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "%.1f mph".format(speedMph),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // GPS Accuracy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Accuracy:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Text(
                    text = "%.1f meters".format(accuracy),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        accuracy < 10 -> Color(0xFF4CAF50) // Green - Excellent
                        accuracy < 20 -> Color(0xFFFFA726) // Orange - Good
                        else -> Color(0xFFF44336) // Red - Poor
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Threshold
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Vehicle Threshold:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Text(
                    text = "> %.1f mph".format(threshold),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SpeedRow(label: String, value: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun MotionAnalysisCard(
    variance: Double,
    classification: String,
    timerProgress: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🔍 MOTION ANALYSIS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Variance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Variance:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "%.3f m/s²".format(variance),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Classification
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (classification) {
                        "VEHICLE MOTION" -> Color(0xFFC8E6C9) // Light green
                        "Stationary" -> Color(0xFFE0E0E0) // Gray
                        "Walking/Running" -> Color(0xFFFFCDD2) // Light red
                        else -> Color(0xFFF5F5F5)
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Classification",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = classification,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (classification) {
                            "VEHICLE MOTION" -> Color(0xFF388E3C)
                            "Stationary" -> Color.Gray
                            "Walking/Running" -> Color(0xFFC62828)
                            else -> Color.Black
                        }
                    )
                }
            }

            // Timer Progress (if active)
            if (timerProgress > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Column {
                    Text(
                        text = "Detection Timer:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = timerProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "%.1f / 5.0 seconds".format(timerProgress * 5),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun ThresholdsCard(
    vehicleMin: Double,
    vehicleMax: Double,
    walkingThreshold: Double,
    speedThreshold: Double,
    stoppedThreshold: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "⚙️ DETECTION THRESHOLDS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            ThresholdRow("Vehicle Variance", "%.2f - %.2f m/s²".format(vehicleMin, vehicleMax))
            ThresholdRow("Walking Threshold", "> %.2f m/s²".format(walkingThreshold))
            ThresholdRow("Speed Threshold", "> %.1f mph".format(speedThreshold))
            ThresholdRow("Stopped Threshold", "< %.1f mph".format(stoppedThreshold))
        }
    }
}

@Composable
fun ThresholdRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TripInfoCard(tripDuration: String, tripId: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFC8E6C9) // Light green
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🚗 ACTIVE TRIP",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF388E3C)
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Duration:", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = tripDuration,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Trip ID: $tripId",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

