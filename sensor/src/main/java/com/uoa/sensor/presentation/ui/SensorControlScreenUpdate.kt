package com.uoa.sensor.presentation.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.*
import com.uoa.core.utils.DRIVER_PROFILE_ID
import com.uoa.sensor.presentation.viewModel.SensorViewModel
import com.uoa.sensor.presentation.viewModel.TripViewModel
import com.uoa.sensor.services.VehicleMovementService
import com.uoa.sensor.services.VehicleMovementServiceUpdate

import java.util.UUID

/**
 * Production‑ready Composable for controlling vehicle movement detection and displaying trip status.
 * Automatically manages permissions, starts/stops monitoring service, and reflects real‑time state.
 */
@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SensorControlScreenUpdate(
    driverProfileId: UUID,
    sensorViewModel: SensorViewModel = hiltViewModel(),
    tripViewModel: TripViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // 1. Permission states
    val requiredPermissions = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACTIVITY_RECOGNITION.takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q },
        Manifest.permission.POST_NOTIFICATIONS.takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU }
    ).filterNotNull()

    val permissionState = rememberMultiplePermissionsState(requiredPermissions)

    // 2. Determine if all needed permissions are granted
    val permissionsGranted by remember {
        derivedStateOf {
            permissionState.permissions.all { it.hasPermission }
        }
    }

    // 3. Track service start status
    var serviceStarted by rememberSaveable { mutableStateOf(false) }

    // 4. Auto‑start monitoring when permissions granted
    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted && !serviceStarted) {
            Intent(context, VehicleMovementServiceUpdate::class.java).apply {
                putExtra(DRIVER_PROFILE_ID, driverProfileId.toString())
            }.also { intent ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
                else context.startService(intent)
            }
            serviceStarted = true
        }
    }

    // 5. Collect view model state
    val isMoving by sensorViewModel.isVehicleMoving.collectAsState()
    val tripStarted by sensorViewModel.tripStartStatus.collectAsState()
    val collecting by sensorViewModel.collectionStatus.collectAsState()
    val readableAccel by sensorViewModel.readableAcceleration
    val movementType by sensorViewModel.movementType

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Permission request UI
        if (!permissionsGranted) {
            Text("This app requires location, activity, and notification permissions to function.")
            Button(
                onClick = { permissionState.launchMultiplePermissionRequest() },
                enabled = true
            ) {
                Text("Grant Permissions")
            }
            return@Column
        }

        // Monitoring status
        // Live movement‐type display
        Text(
            text = when (movementType) {
                "stationary" -> "No movement detected"
                "walking"    -> "You are walking"
                "running"    -> "You are running"
                "vehicle"    -> "Now in a moving vehicle"
                else         -> "Unknown movement"
            }
        )
        Spacer(Modifier.height(24.dp))

        // Trip status based on all possible combinations
        when {
            movementType == "vehicle" && tripStarted && collecting -> {
                Text("Trip now active and data collection is ongoing.")
            }
            movementType == "vehicle" && tripStarted && !collecting -> {
                Text("Trip started, waiting to start data collection.")
            }
            movementType == "vehicle" && !tripStarted && collecting -> {
                Text("Vehicle movement detected, starting trip.... Data is being collected.")
            }
            movementType == "vehicle" && !tripStarted && !collecting -> {
                Text("Vehicle movement detected, waiting to start trip and data collection....")
            }
            movementType == "stationary" && !tripStarted -> {
                Text("Yet to start trip:\nTrip will start automatically when you start driving.")
            }
            movementType == "stationary" && tripStarted && collecting -> {
                Text(
                    "Ending Trip...\nBecause vehicle has stopped.\nProcessing last collected Data.",
                    Modifier.align(Alignment.CenterHorizontally),
                    )
            }
            movementType == "stationary" && tripStarted && !collecting -> {
                Text("Trip ongoing but data collection is paused while vehicle is being stopped.")
            }
            movementType == "stationary" && !tripStarted && collecting -> {
                Text("Vehicle is stopped, processing las collected data.",
                    Modifier.align(Alignment.CenterHorizontally)
                )
            }
            movementType == "stationary" && !tripStarted && !collecting -> {
                Text("Vehicle is stopped and no trip or data collection in progress.",
                    Modifier.align(Alignment.CenterHorizontally))
            }
            movementType in listOf("walking", "running") && tripStarted && collecting -> {
                Text("Trip ongoing and data collection is active while on foot.",
                    Modifier.align(Alignment.CenterHorizontally))
            }
            movementType in listOf("walking", "running") && tripStarted && !collecting -> {
                Text("Trip ongoing on foot, but data collection is not active.",
                    Modifier.align(Alignment.CenterHorizontally)
                )
            }
            movementType in listOf("walking", "running") && !tripStarted && collecting -> {
                Text("Walking/running detected, data being collected without trip start.",
                    Modifier.align(Alignment.CenterHorizontally)
                )
            }
            movementType in listOf("walking", "running") && !tripStarted && !collecting -> {
                Text("Walking/running detected, trip not started and no data collection.",
                    Modifier.align(Alignment.CenterHorizontally)
                )
            }
            movementType !in listOf("stationary", "walking", "running", "vehicle") && collecting -> {
                Text("Unknown movement detected so trips is stopped.\nNow processing collected data from last trip.",
                    Modifier.align(Alignment.CenterHorizontally)
                )
            }
            movementType !in listOf("stationary", "walking", "running", "vehicle") && !collecting -> {
                Text("Unknown movement detected\nApp cannot start Trip and or Data collection.",
                    Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Manual stop monitoring
        if (serviceStarted) {
            Button(onClick = {
                context.stopService(Intent(context, VehicleMovementServiceUpdate::class.java))
                serviceStarted = false
            }) {
                Text("Stop Monitoring")
            }
        }
    }
}