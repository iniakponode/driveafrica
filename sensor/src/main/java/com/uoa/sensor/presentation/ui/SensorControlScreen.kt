package com.uoa.sensor.presentation.ui

import com.uoa.sensor.presentation.viewModel.TripViewModel
import android.Manifest
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.rememberPermissionState
import com.uoa.core.mlclassifier.MinMaxValuesLoader
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.ml.presentation.viewmodel.AlcoholInfluenceViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
//import com.uoa.dbda.presentation.viewModel.UnsafeBehaviourViewModel
import java.util.UUID


@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SensorControlScreen(
    sensorViewModel: SensorViewModel = viewModel(),
    tripViewModel: TripViewModel = viewModel(),
    alcoholInfluenceViewModel: AlcoholInfluenceViewModel = hiltViewModel()
) {
    val collectionStatus by sensorViewModel.collectionStatus.collectAsState()
    val isVehicleMoving by sensorViewModel.isVehicleMoving.collectAsState()
    val alcoholInfluence by alcoholInfluenceViewModel.alcoholInfluence.observeAsState()
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val profileIdString = prefs.getString(DRIVER_PROFILE_ID, null)
    val driverProfileId = UUID.fromString(profileIdString)



    // Check and request POST_NOTIFICATIONS permission for Android 13+ devices
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        val context = LocalContext.current.applicationContext
        LaunchedEffect(Unit) {
            // Initialize MinMaxValuesLoader with the application context
            // Get the application context using LocalContext.current

            if (!permissionState.hasPermission) {
                permissionState.launchPermissionRequest()
            }
        }
    }

    // Step 1: Request foreground location permissions
    val foregroundPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.FOREGROUND_SERVICE
        )
    )

    val coroutineScope = rememberCoroutineScope()

    // Step 2: Request background location permission separately
    val backgroundPermissionState = rememberPermissionState(
        permission = Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    // Check WorkManager status on screen entry
    LaunchedEffect(Unit) {
        sensorViewModel.checkWorkManagerStatus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Step 1: Display foreground location permissions
        LocationPermissions(foregroundPermissionState)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val tripID = tripViewModel.updateTripId(UUID.randomUUID())

            // Request foreground permissions first
            if (!foregroundPermissionState.allPermissionsGranted) {
                foregroundPermissionState.launchMultiplePermissionRequest()
            } else if (foregroundPermissionState.allPermissionsGranted && !backgroundPermissionState.hasPermission) {
                // If foreground permissions are granted, request background permission next
                backgroundPermissionState.launchPermissionRequest()
            } else if (foregroundPermissionState.allPermissionsGranted && backgroundPermissionState.hasPermission && !collectionStatus) {
                // Start trip if all permissions are granted
                tripViewModel.startTrip(driverProfileId, tripID)
                sensorViewModel.startSensorCollection("START", true, tripID)
                sensorViewModel.updateCollectionStatus(true)
                sensorViewModel.checkWorkManagerStatus()
            } else {
                // End trip
                sensorViewModel.stopSensorCollection()
                sensorViewModel.updateCollectionStatus(false)
            }
        }) {
            Text(
                text = when {
                    !foregroundPermissionState.allPermissionsGranted -> "Grant Location Permissions"
                    foregroundPermissionState.allPermissionsGranted && !backgroundPermissionState.hasPermission -> "Grant Background Location Permission"
                    else -> if (!collectionStatus) "Start Trip" else "End Trip"
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}





@Composable
fun SensorControlScreenRoute(
    sensorViewModel: SensorViewModel = viewModel(),
    tripViewModel: TripViewModel = viewModel()
) {
    SensorControlScreen(sensorViewModel, tripViewModel)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun getButtonText(
    multiplePermissionState: MultiplePermissionsState,
    collectionStatus: Boolean
): String {
    return when {
        !multiplePermissionState.allPermissionsGranted && !collectionStatus -> "Grant Location Permissions"
        multiplePermissionState.allPermissionsGranted && !collectionStatus -> "Start Trip"
        else -> "End Trip"
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissions(foregroundPermissionState: MultiplePermissionsState) {
    val allPermissionsGranted = foregroundPermissionState.allPermissionsGranted
    val shouldShowRationale = foregroundPermissionState.shouldShowRationale

    if (!allPermissionsGranted) {
        Text(
            text = if (shouldShowRationale) {
                "Location permissions are needed to collect data."
            } else {
                "Please grant location permissions."
            }
        )
    }
}