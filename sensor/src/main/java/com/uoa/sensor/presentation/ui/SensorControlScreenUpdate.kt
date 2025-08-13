package com.uoa.sensor.presentation.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Looper
import android.annotation.SuppressLint
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
import com.google.accompanist.permissions.*
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.uoa.core.utils.DRIVER_PROFILE_ID
import com.uoa.sensor.presentation.viewModel.SensorViewModel
import com.uoa.sensor.presentation.viewModel.TripViewModel
import com.uoa.sensor.presentation.viewModel.RoadViewModel
import com.uoa.sensor.services.VehicleMovementServiceUpdate
import com.uoa.sensor.R
import org.osmdroid.util.GeoPoint

import java.util.UUID

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SensorControlScreenUpdate(
    driverProfileId: UUID,
    sensorViewModel: SensorViewModel = hiltViewModel(),
    tripViewModel: TripViewModel = hiltViewModel(),
    roadViewModel: RoadViewModel = hiltViewModel()
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
    val isMoving by sensorViewModel.isVehicleMoving.collectAsState()
    val tripStarted by sensorViewModel.tripStartStatus.collectAsState()
    val collecting by sensorViewModel.collectionStatus.collectAsState()
    val readableAccel by sensorViewModel.readableAcceleration
    val movementType by sensorViewModel.movementType
    val distanceTravelled by sensorViewModel.distanceTravelled.collectAsState()
    val pathPoints by sensorViewModel.pathPoints.collectAsState()
    val roads by roadViewModel.nearbyRoads.collectAsState()

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var currentLocation by remember { mutableStateOf<GeoPoint?>(null) }

    @SuppressLint("MissingPermission")
    DisposableEffect(permissionsGranted) {
        if (permissionsGranted) {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { location ->
                        val point = GeoPoint(location.latitude, location.longitude)
                        currentLocation = point
                        sensorViewModel.addLocation(point)
                        roadViewModel.fetchNearbyRoads(location.latitude, location.longitude, 0.05)
                    }
                }
            }
            fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
            onDispose { fusedLocationClient.removeLocationUpdates(callback) }
        } else {
            onDispose { }
        }
    }

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

        currentLocation?.let { location ->
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

                roads.firstOrNull()?.speedLimit?.let { limit ->
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
                            Text(
                                text = stringResource(
                                    R.string.speed_limit,
                                    limit,
                                    stringResource(R.string.unit_kilometers_per_hour)
                                ),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}