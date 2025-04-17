@file:OptIn(ExperimentalPermissionsApi::class)

package com.uoa.sensor.presentation.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.Constants.Companion.TRIP_ID
import com.uoa.sensor.presentation.viewModel.SensorViewModel
import com.uoa.sensor.presentation.viewModel.TripViewModel
import com.uoa.sensor.services.DataCollectionService
import com.uoa.sensor.services.VehicleMovementService
import java.util.UUID
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.uoa.core.apiServices.workManager.UploadAllDataWorker

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
//fun SensorControlScreen(
//    sensorViewModel: SensorViewModel = hiltViewModel(),
//    tripViewModel: TripViewModel = hiltViewModel(),
//    driverProfileId: UUID
//) {
//    val context = LocalContext.current
//    val serviceStarted = remember { mutableStateOf(false) }
//
//    // Update required permissions to include FOREGROUND_SERVICE_LOCATION.
//    val foregroundPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//        listOf(
//            Manifest.permission.ACCESS_FINE_LOCATION,
//            Manifest.permission.ACCESS_COARSE_LOCATION,
//            Manifest.permission.ACTIVITY_RECOGNITION,
//            Manifest.permission.FOREGROUND_SERVICE
//        )
//    } else {
//        listOf(
//            Manifest.permission.ACCESS_FINE_LOCATION,
//            Manifest.permission.ACCESS_COARSE_LOCATION
//        )
//    }
//
//    val foregroundPermissionState = rememberMultiplePermissionsState(foregroundPermissions)
//
//    val backgroundPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//        rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
//    } else null
//
//    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
//    } else null
//
//    val permissionsGranted = foregroundPermissionState.allPermissionsGranted &&
//            (backgroundPermissionState == null || backgroundPermissionState.hasPermission) &&
//            (notificationPermissionState == null || notificationPermissionState.hasPermission)
//
//    // Start the VehicleMovementService only if permissions are granted.
//    LaunchedEffect(permissionsGranted) {
//        if (permissionsGranted && !serviceStarted.value) {
//            val movementIntent = Intent(context, VehicleMovementService::class.java)
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                context.startForegroundService(movementIntent)
//            } else {
//                context.startService(movementIntent)
//            }
//
//            serviceStarted.value = true
//            Log.d("SensorControlScreen", "VehicleMovementService started successfully.")
//        } else if (!permissionsGranted) {
//            Log.d("SensorControlScreen", "Required permissions not granted, service not started.")
//        }
//    }
//
//    // Collect state from the view model.
//    val collectionStatus by sensorViewModel.collectionStatus.collectAsState()
//    val isVehicleMoving by sensorViewModel.isVehicleMoving.collectAsState()
//    val tripStartStatus by sensorViewModel.tripStartStatus.collectAsState()
//    val linAcceleReading by sensorViewModel.linAcceleReading
//    val readableAcceleration by sensorViewModel.readableAcceleration
//    // Observe currentTripId from the ViewModel.
//    val currentTripId by tripViewModel.currentTripId.collectAsState()
//
//    // State for displaying a settings dialog.
//    var showSettingsDialog by remember { mutableStateOf(false) }
//    var tripID by remember { mutableStateOf<UUID?>(null) }
//
//    // Display a dialog guiding the user to app settings if a permission was permanently denied.
//    PermissionSettingsDialog(
//        showDialog = showSettingsDialog,
//        onDismiss = { showSettingsDialog = false },
//        onConfirm = {
//            showSettingsDialog = false
//            val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS).apply {
//                data = Uri.fromParts("package", context.packageName, null)
//            }
//            context.startActivity(intent)
//        }
//    )
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        // Show UI to request permissions.
//        LocationPermissions(foregroundPermissionState)
//        Spacer(modifier = Modifier.height(24.dp))
//        Button(
//            onClick = {
//                when {
//                    !foregroundPermissionState.allPermissionsGranted -> {
//                        foregroundPermissionState.launchMultiplePermissionRequest()
//                    }
//                    backgroundPermissionState != null && !backgroundPermissionState.hasPermission -> {
//                        backgroundPermissionState.launchPermissionRequest()
//                    }
//                    notificationPermissionState != null && !notificationPermissionState.hasPermission -> {
//                        notificationPermissionState.launchPermissionRequest()
//                    }
//                    else -> {
//                        if (!tripStartStatus) {
//                            // Only allow trip start if the vehicle is moving.
//                            if (isVehicleMoving) {
//                                tripID = UUID.randomUUID()
//                                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
//                                prefs.edit().putString(TRIP_ID, tripID.toString()).apply()
//                                tripViewModel.updateTripId(tripID!!)
//                                tripViewModel.startTrip(driverProfileId, tripID!!)
//                                val sensorIntent = Intent(context, DataCollectionService::class.java).apply {
//                                    putExtra("TRIP_ID", tripID.toString())
//                                }
//                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                                    context.startForegroundService(sensorIntent)
//                                } else {
//                                    context.startService(sensorIntent)
//                                }
//                                Toast.makeText(context, "Trip Started Successfully.", Toast.LENGTH_LONG).show()
//                            } else {
//                                Toast.makeText(context, "Trip can only start when the vehicle is moving.", Toast.LENGTH_LONG).show()
//                            }
//                        } else {
//                            // End trip logic.
//                            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
//                            val tripIdString = prefs.getString(TRIP_ID, null)
//                            tripID = tripIdString?.let { UUID.fromString(it) }
//                            if (tripID != null) {
//                                tripViewModel.endTrip(tripID!!)
//                                prefs.edit().remove(TRIP_ID).apply()
//                                tripViewModel.clearTripID()
//                                val sensorDataCollectionIntent = Intent(context, DataCollectionService::class.java)
//                                context.stopService(sensorDataCollectionIntent)
//                                Toast.makeText(context, "Trip Ended Successfully.", Toast.LENGTH_LONG).show()
//                                tripID = null
//                            } else {
//                                Toast.makeText(context, "Error: Trip ID is missing.", Toast.LENGTH_LONG).show()
//                            }
//                        }
//                    }
//                }
//            },
//            modifier = Modifier.padding(vertical = 8.dp)
//        ) {
//            Text(
//                text = when {
//                    !foregroundPermissionState.allPermissionsGranted -> "Grant Permissions"
//                    backgroundPermissionState != null && !backgroundPermissionState.hasPermission -> "Grant Background Location Permission"
//                    notificationPermissionState != null && !notificationPermissionState.hasPermission -> "Grant Notification Permission"
//                    else -> if (tripStartStatus) "End Trip" else "Start Trip"
//                }
//            )
//        }


        fun SensorControlScreen(
            sensorViewModel: SensorViewModel = hiltViewModel(),
            tripViewModel: TripViewModel = hiltViewModel(),
            driverProfileId: UUID
        ) {
            val context = LocalContext.current
            val serviceStarted = remember { mutableStateOf(false) }
            var tripID by remember { mutableStateOf<UUID?>(null) }



//    LaunchedEffect(Unit) {
//        val constraints = Constraints.Builder()
//            .setRequiredNetworkType(NetworkType.CONNECTED)
//            .build()
//
//        val uploadRequest = PeriodicWorkRequestBuilder<UploadAllDataWorker>(5, TimeUnit.MINUTES)
//            .setConstraints(constraints)
//            .build()
//
//        WorkManager.getInstance(context)
//            .enqueueUniquePeriodicWork("UploadRawData", ExistingPeriodicWorkPolicy.KEEP, uploadRequest)
//    }


            // Define permission lists.
            val foregroundPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACTIVITY_RECOGNITION,
                    Manifest.permission.FOREGROUND_SERVICE
                )
            } else {
                listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }

            val foregroundPermissionState = rememberMultiplePermissionsState(foregroundPermissions)
            val backgroundPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else null
            val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
            } else null

            val permissionsGranted = foregroundPermissionState.allPermissionsGranted &&
                    (backgroundPermissionState == null || backgroundPermissionState.hasPermission) &&
                    (notificationPermissionState == null || notificationPermissionState.hasPermission)

            // Launch the service if permissions are granted.
            LaunchedEffect(permissionsGranted) {
                if (permissionsGranted && !serviceStarted.value) {
                    val movementIntent = Intent(context, VehicleMovementService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(movementIntent)
                    } else {
                        context.startService(movementIntent)
                    }
                    serviceStarted.value = true
                    Log.d("SensorControlScreen", "VehicleMovementService started successfully.")
                } else if (!permissionsGranted) {
                    Log.d("SensorControlScreen", "Required permissions not granted, service not started.")
                }
            }



            // Collect state from ViewModel.
            val isVehicleMoving by sensorViewModel.isVehicleMoving.collectAsState()
            val tripStartStatus by sensorViewModel.tripStartStatus.collectAsState()
            val currentTripId by tripViewModel.currentTripId.collectAsState()
            val linAcceleReading by sensorViewModel.linAcceleReading
            val readableAcceleration by sensorViewModel.readableAcceleration
            val collectionStatus by sensorViewModel.collectionStatus.collectAsState()

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)


            // UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Button(
                    onClick = {
                        when {
                    !foregroundPermissionState.allPermissionsGranted -> {
                        foregroundPermissionState.launchMultiplePermissionRequest()
                    }
                    backgroundPermissionState != null && !backgroundPermissionState.hasPermission -> {
                        backgroundPermissionState.launchPermissionRequest()
                    }
                    notificationPermissionState != null && !notificationPermissionState.hasPermission -> {
                        notificationPermissionState.launchPermissionRequest()
                    }

                            else -> {
                                if (!tripStartStatus) {
                                    // Start trip logic after all permissions are granted
                                    if (isVehicleMoving) {
                                        tripID = UUID.randomUUID()
                                        prefs.edit().putString(TRIP_ID, tripID.toString()).apply()
                                        tripViewModel.updateTripId(tripID!!)
                                        tripViewModel.startTrip(driverProfileId, tripID!!)

                                        // Start DataCollectionService
                                        val sensorIntent = Intent(context, DataCollectionService::class.java).apply {
                                            putExtra("TRIP_ID", tripID.toString())
                                        }
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            context.startForegroundService(sensorIntent)
                                        } else {
                                            context.startService(sensorIntent)
                                        }
                                        Toast.makeText(context, "Trip Started Successfully.", Toast.LENGTH_LONG).show()
                                }else{
                                        Toast.makeText(context, "Trip can only start when the vehicle is moving.", Toast.LENGTH_LONG).show()
                                    }

                                }
                                else {
                                    // End trip and data collection
                                    val tripIdString = prefs.getString(TRIP_ID, null)
                                    tripID = tripIdString?.let { UUID.fromString(it) }
                                    if (tripID != null) {
                                        tripViewModel.endTrip(tripID!!)
                                        prefs.edit().remove("TRIP_ID").apply()
                                        tripViewModel.clearTripID()

                                        // Stop DataCollectionService
                                        val sensorDataCollectionIntent = Intent(context, DataCollectionService::class.java)
                                        context.stopService(sensorDataCollectionIntent)
                                        Toast.makeText(context, "Trip Ended Successfully.", Toast.LENGTH_LONG).show()

                                        tripID = null
                                    } else {
                                        Toast.makeText(context, "Trip Ended Successfully.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = when {
                            // 1) If we still need foreground permissions
                            !foregroundPermissionState.allPermissionsGranted -> "Grant Permissions"

                            // 2) If background permission is required
                            backgroundPermissionState?.hasPermission == false -> "Grant Background Location Permission"

                            // 3) If notification permission is required
                            notificationPermissionState?.hasPermission == false -> "Grant Notification Permission"

                            // 4) If trip is started but collection is not active yet
                            tripStartStatus && !collectionStatus -> "Processing and saving last sensor data\nto end trip..."

                            // 5) If trip isn’t started
                            !tripStartStatus -> "Start Trip"

                            // 6) Otherwise...
                            else -> "End Trip"
                        }
                    )
                }


        Spacer(modifier = Modifier.height(24.dp))
        // Display continuously updated vehicle movement and collection status.
        Text(
            text = if (isVehicleMoving) "Movement detected at $linAcceleReading, i.e: $readableAcceleration" else "Movement is too slow, not sure it's a vehicle: $linAcceleReading, i.e: $readableAcceleration",
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Text(
            text = "New Data Collection is ${if (collectionStatus && tripStartStatus) "Ongoing" else "Stopped"}",
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}


