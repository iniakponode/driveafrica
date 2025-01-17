package com.uoa.sensor.presentation.ui

import com.uoa.sensor.presentation.viewModel.TripViewModel
import android.Manifest
import android.R.attr.data
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import com.uoa.sensor.presentation.viewModel.SensorViewModel
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.Constraints
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.accompanist.permissions.rememberPermissionState
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.Constants.Companion.TRIP_ID
import com.uoa.sensor.services.LocationService
import com.uoa.sensor.services.DataCollectionService
import com.uoa.core.apiServices.workManager.UploadRawSensorDataWorker
//import com.uoa.dbda.presentation.viewModel.UnsafeBehaviourViewModel
import java.util.UUID
import java.util.concurrent.TimeUnit


@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SensorControlScreen(
    sensorViewModel: SensorViewModel = viewModel(),
    tripViewModel: TripViewModel = viewModel(),
) {
    // Observing collection status and vehicle movement state
    val collectionStatus by sensorViewModel.collectionStatus.collectAsState()
    val isVehicleMoving by sensorViewModel.isVehicleMoving.collectAsState()
    val tripStartStatus by sensorViewModel.tripEndStatus.collectAsState() // Correct StateFlow
    val tripUploadSuccess by tripViewModel.tripUploadSuccess.collectAsState()
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val profileIdString = prefs.getString(DRIVER_PROFILE_ID, null)
    val driverProfileId = UUID.fromString(profileIdString)

    // State to manage dialog visibility
    var showSettingsDialog by remember { mutableStateOf(false) }

    var tripID by remember { mutableStateOf<UUID?>(null) }


        LaunchedEffect(tripUploadSuccess) {
            Toast.makeText(context, "Trip Upload successful", Toast.LENGTH_LONG).show()
        }

    // Check and request POST_NOTIFICATIONS permission for Android 13+ devices
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//        val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
//        LaunchedEffect(Unit) {
//            if (!permissionState.hasPermission) {
//                permissionState.launchPermissionRequest()
//            }
//        }
//    }

    LaunchedEffect(Unit) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadRequest = PeriodicWorkRequestBuilder<UploadRawSensorDataWorker>(20, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork("UploadRawData", ExistingPeriodicWorkPolicy.KEEP, uploadRequest)
    }

    // Request POST_NOTIFICATIONS permission for Android 13+ devices
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }


    // Step 1: Request foreground location permissions
    val requiredPermissions = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACTIVITY_RECOGNITION,
        Manifest.permission.FOREGROUND_SERVICE
    )
    val foregroundPermissionState = rememberMultiplePermissionsState(requiredPermissions)

    // Step 2: Request background location permission separately
    val backgroundPermissionState = rememberPermissionState(
        permission = Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    // State for whether background permission has been requested
    var hasRequestedBackgroundPermission by remember { mutableStateOf(false) }

    // Display the PermissionSettingsDialog
    PermissionSettingsDialog(
        showDialog = showSettingsDialog,
        onDismiss = {
            showSettingsDialog = false
        },
        onConfirm = {
            showSettingsDialog = false
            // Open app settings
            val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }
    )

    // Check if permissions are granted and update accordingly
    val permissionsGranted =
        foregroundPermissionState.allPermissionsGranted &&
                backgroundPermissionState.hasPermission &&
                (notificationPermissionState == null || notificationPermissionState.hasPermission)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Display permission status for foreground location
        LocationPermissions(foregroundPermissionState)

        // Add a spacer to separate the permission section from the button
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                when {
                    !foregroundPermissionState.allPermissionsGranted -> {
                        if (foregroundPermissionState.permissions.any { !it.shouldShowRationale }) {
                            // Permission was denied with "don't ask again"
                            showSettingsDialog = true
                        } else {
                            foregroundPermissionState.launchMultiplePermissionRequest()
                        }
                    }

                    !backgroundPermissionState.hasPermission && !hasRequestedBackgroundPermission -> {
                        if (!backgroundPermissionState.shouldShowRationale) {
                            // Background permission was denied with "don't ask again"
                            showSettingsDialog = true
                        } else {
                            hasRequestedBackgroundPermission = true
                            backgroundPermissionState.launchPermissionRequest()
                        }
                    }

                    notificationPermissionState != null && !notificationPermissionState.hasPermission -> {
                        notificationPermissionState.launchPermissionRequest()
                    }

                    else -> {
                        if (!tripStartStatus) {
                            // Start trip logic after all permissions are granted
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
                        } else {
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
                                Toast.makeText(context, "Error: Trip ID is missing.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            },
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(
                text = when {
                    !foregroundPermissionState.allPermissionsGranted -> "Grant Permissions"
                    foregroundPermissionState.allPermissionsGranted && !backgroundPermissionState.hasPermission -> "Grant Background Location Permission"
                    notificationPermissionState != null && !notificationPermissionState.hasPermission -> "Grant Notification Permission"
                    else -> if (tripStartStatus) "End Trip" else "Start Trip"
                }
            )
        }


        // Add a spacer to separate the button from the text displaying vehicle movement status
        Spacer(modifier = Modifier.height(24.dp))

        // Display vehicle movement status
        Text(
            text = "Vehicle is ${if (isVehicleMoving) "Moving" else "Stationary"}\n" +
                    " Data Collection is ${if (collectionStatus) "Ongoing" else "Has Stopped"}",
            modifier = Modifier.padding(vertical = 8.dp)
        )
        // Add a spacer to separate the button from the text displaying vehicle movement status
        Spacer(modifier = Modifier.height(24.dp))
    }
}
