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
import com.uoa.sensor.presentation.viewModel.SensorViewModel
import com.uoa.sensor.services.VehicleMovementServiceUpdate
import com.uoa.sensor.R
import com.uoa.sensor.utils.displaySpeedLimitKmh
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SensorControlScreenUpdate(
    navController: NavController,
    sensorViewModel: SensorViewModel = hiltViewModel()
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
            Intent(context, VehicleMovementServiceUpdate::class.java).also { intent ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
                else context.startService(intent)
            }
            serviceStarted = true
        }
    }

    // 3) State
    val tripStarted by sensorViewModel.tripStartStatus.collectAsState()
    val collecting by sensorViewModel.collectionStatus.collectAsState()
    val drivingState by sensorViewModel.drivingState.collectAsState()
    val drivingLastUpdate by sensorViewModel.drivingLastUpdate.collectAsState()
    val isGpsStale by sensorViewModel.isGpsStale.collectAsState()
    val distanceTravelled by sensorViewModel.distanceTravelled.collectAsState()
    val pathPoints by sensorViewModel.pathPoints.collectAsState()
    val roads by sensorViewModel.nearbyRoads.collectAsState()
    val speedLimit by sensorViewModel.speedLimit.collectAsState()
    val currentLocation by sensorViewModel.currentLocation.collectAsState()
    val drivingSpeedMps by sensorViewModel.drivingSpeedMps.collectAsState()
    val isVehicleMoving by sensorViewModel.isVehicleMoving.collectAsState()

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
            Text(stringResource(R.string.sensor_permission_explanation))
            Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Grant Permissions")
            }
            return@Column
        }

        val lastUpdateText = if (drivingLastUpdate > 0L) {
            SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(drivingLastUpdate))
        } else {
            "No updates yet"
        }

        // Navigation button to Vehicle Detection Monitor
        Button(
            onClick = { navController.navigate("vehicleDetectionMonitor") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🚗 Open Vehicle Monitor")
        }
        Spacer(Modifier.height(8.dp))

        // Trip status
        when {
            drivingState == com.uoa.sensor.motion.DrivingStateManager.DrivingState.RECORDING && tripStarted && collecting ->
                Text("Trip now active and data collection is ongoing.")
            drivingState == com.uoa.sensor.motion.DrivingStateManager.DrivingState.RECORDING && tripStarted && !collecting ->
                Text("Trip started, waiting to start data collection.")
            drivingState == com.uoa.sensor.motion.DrivingStateManager.DrivingState.RECORDING && !tripStarted && collecting ->
                Text("Vehicle movement detected, starting trip.... Data is being collected.")
            drivingState == com.uoa.sensor.motion.DrivingStateManager.DrivingState.RECORDING && !tripStarted && !collecting ->
                Text("Vehicle movement detected, waiting to start trip and data collection....")
            drivingState == com.uoa.sensor.motion.DrivingStateManager.DrivingState.IDLE && !tripStarted ->
                Text("Yet to start trip:\nTrip will start automatically when you start driving.")
            drivingState == com.uoa.sensor.motion.DrivingStateManager.DrivingState.IDLE && tripStarted && collecting ->
                Text("Trip Ended\nBecause vehicle has stopped.\n")
            drivingState == com.uoa.sensor.motion.DrivingStateManager.DrivingState.IDLE && tripStarted && !collecting ->
                Text("Trip ongoing but data collection is paused while vehicle is being stopped.")
            drivingState == com.uoa.sensor.motion.DrivingStateManager.DrivingState.IDLE && !tripStarted && collecting ->
                Text("Vehicle is stopped, processing las collected data.")
            drivingState == com.uoa.sensor.motion.DrivingStateManager.DrivingState.IDLE && !tripStarted && !collecting ->
                Text("Vehicle is stopped and no trip or data collection in progress.")
            drivingState == com.uoa.sensor.motion.DrivingStateManager.DrivingState.POTENTIAL_STOP && tripStarted && collecting ->
                Text("Vehicle stopped, monitoring before ending trip.")
            drivingState == com.uoa.sensor.motion.DrivingStateManager.DrivingState.POTENTIAL_STOP && tripStarted && !collecting ->
                Text("Vehicle stopped; data collection paused.")
            drivingState == com.uoa.sensor.motion.DrivingStateManager.DrivingState.VERIFYING ->
                Text("Verifying movement before starting trip.")
        }

        Spacer(Modifier.height(12.dp))

        // Use the last known point if the current location becomes null
        val displayLocation = currentLocation ?: pathPoints.lastOrNull()

        displayLocation?.let { location ->
            // Fixed-height map so the rest can scroll
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
            ) {
                val roadName = roads.firstOrNull()?.name
                val displaySpeedLimit = displaySpeedLimitKmh(context, speedLimit)
                MapComposable(
                    context = context,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    roads = roads,
                    currentRoadName = roadName,
                    speedLimitKmh = displaySpeedLimit,
                    path = pathPoints,
                    isVehicleMoving = isVehicleMoving,
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
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    val speedText = if (drivingSpeedMps <= 0.0) "--" else "%.1f".format(drivingSpeedMps * 3.6)
                    Text(
                        text = "Current Speed: $speedText ${stringResource(R.string.unit_kilometers_per_hour)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (isGpsStale) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "GPS stale - using fallback speed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
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
                val displaySpeedLimit = displaySpeedLimitKmh(context, speedLimit)
                val speedText = if (displaySpeedLimit <= 0) "--" else displaySpeedLimit.toString()
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
