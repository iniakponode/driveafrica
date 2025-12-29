package com.uoa.sensor.presentation.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.*
import com.uoa.core.utils.DRIVER_PROFILE_ID
import com.uoa.sensor.presentation.viewModel.SensorViewModel
import com.uoa.sensor.presentation.viewModel.TripViewModel
import com.uoa.sensor.services.VehicleMovementServiceUpdate
import com.uoa.sensor.R

import java.util.UUID

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SensorControlScreenUpdate(
    navController: NavController,
    driverProfileId: UUID,
    sensorViewModel: SensorViewModel = hiltViewModel(),
    tripViewModel: TripViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // 1) Permissions
    val requiredPermissions = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACTIVITY_RECOGNITION.takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q },
        Manifest.permission.POST_NOTIFICATIONS.takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU }
    ).filterNotNull()
    val permissionState = rememberMultiplePermissionsState(requiredPermissions)

    val permissionsGranted by remember {
        derivedStateOf { permissionState.permissions.all { it.hasPermission } }
    }

    // 2) Service start
    var serviceStarted by rememberSaveable { mutableStateOf(false) }

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

    // 3) State
    val tripStarted by sensorViewModel.tripStartStatus.collectAsState()
    val collecting by sensorViewModel.collectionStatus.collectAsState()
    val movementType by sensorViewModel.movementType
    val distanceTravelled by sensorViewModel.distanceTravelled.collectAsState()
    val pathPoints by sensorViewModel.pathPoints.collectAsState()
    val roads by sensorViewModel.nearbyRoads.collectAsState()
    val speedLimit by sensorViewModel.speedLimit.collectAsState()
    val currentLocation by sensorViewModel.currentLocation.collectAsState()
    val fusedSpeedMps by sensorViewModel.fusedSpeedMps.collectAsState()

    // ✅ Make whole screen scrollable
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp)
            .navigationBarsPadding()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!permissionsGranted) {
            Text("This app requires location, activity, and notification permissions to function.")
            Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Grant Permissions")
            }
            return@Column
        }

        // Navigation button to Vehicle Detection Monitor
        Button(
            onClick = { navController.navigate("vehicleDetectionMonitor") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🚗 Open Vehicle Monitor")
        }
        Spacer(Modifier.height(16.dp))

        // Movement status
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

        // Trip status
        when {
            movementType == "vehicle" && tripStarted && collecting ->
                Text("Trip now active and data collection is ongoing.")
            movementType == "vehicle" && tripStarted && !collecting ->
                Text("Trip started, waiting to start data collection.")
            movementType == "vehicle" && !tripStarted && collecting ->
                Text("Vehicle movement detected, starting trip.... Data is being collected.")
            movementType == "vehicle" && !tripStarted && !collecting ->
                Text("Vehicle movement detected, waiting to start trip and data collection....")
            movementType == "stationary" && !tripStarted ->
                Text("Yet to start trip:\nTrip will start automatically when you start driving.")
            movementType == "stationary" && tripStarted && collecting ->
                Text("Trip Ended\nBecause vehicle has stopped.\n")
            movementType == "stationary" && tripStarted && !collecting ->
                Text("Trip ongoing but data collection is paused while vehicle is being stopped.")
            movementType == "stationary" && !tripStarted && collecting ->
                Text("Vehicle is stopped, processing las collected data.")
            movementType == "stationary" && !tripStarted && !collecting ->
                Text("Vehicle is stopped and no trip or data collection in progress.")
            movementType in listOf("walking", "running") && tripStarted && collecting ->
                Text("Trip ongoing and data collection is active while on foot.")
            movementType in listOf("walking", "running") && tripStarted && !collecting ->
                Text("Trip ongoing on foot, but data collection is not active.")
            movementType in listOf("walking", "running") && !tripStarted && collecting ->
                Text("Walking/running detected, data being collected without trip start.")
            movementType in listOf("walking", "running") && !tripStarted && !collecting ->
                Text("Walking/running detected, trip not started and no data collection.")
            movementType !in listOf("stationary", "walking", "running", "vehicle") && collecting ->
                Text("Unknown movement detected so trip is stopped.\nNow processing collected data from last trip.")
            movementType !in listOf("stationary", "walking", "running", "vehicle") && !collecting ->
                Text("Unknown movement detected\nApp cannot start Trip and or Data collection.")
        }

        Spacer(Modifier.height(24.dp))

        // Use the last known point if the current location becomes null
        val displayLocation = currentLocation ?: pathPoints.lastOrNull()

        displayLocation?.let { location ->
            // Fixed-height map so the rest can scroll
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                MapComposable(
                    context = context,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    roads = roads,
                    path = pathPoints,
                    modifier = Modifier
                        .matchParentSize()
                        .clipToBounds()
                )
                if (serviceStarted) {
                    Button(
                        onClick = {
                            context.stopService(Intent(context, VehicleMovementServiceUpdate::class.java))
                            serviceStarted = false
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Stop Monitoring")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Info cards — no Box.align here (we're in a Column)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Map,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                R.string.distance_travelled,
                                distanceTravelled / 1000,
                                stringResource(R.string.unit_kilometers)
                            ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

            }
        }
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                val speedText = if (currentLocation == null) "--" else "%.1f".format(fusedSpeedMps * 3.6)
                Text(
                    text = "Current Speed: $speedText ${stringResource(R.string.unit_kilometers_per_hour)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(8.dp))
                val speedText = if (currentLocation == null) "--" else speedLimit.toString()
                Text(
                    text = stringResource(
                        R.string.speed_limit,
                        speedText,
                        stringResource(R.string.unit_kilometers_per_hour)
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}