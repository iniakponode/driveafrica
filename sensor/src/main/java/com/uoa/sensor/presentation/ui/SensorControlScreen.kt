package com.uoa.sensor.presentation.ui

import com.uoa.sensor.presentation.viewModel.TripViewModel
import android.Manifest
import android.os.Build
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import com.uoa.sensor.presentation.viewModel.SensorViewModel
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.UUID


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SensorControlScreen(
    sensorViewModel: SensorViewModel = viewModel(),
    tripViewModel: TripViewModel = viewModel()
) {
//    val context = LocalContext.current as ComponentActivity
    val collectionStatus by sensorViewModel.collectionStatus.collectAsState()
//    val tripId by tripViewModel.currentTripId.collectAsState()

    val multiplePermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Check WorkManager status on screen entry
    LaunchedEffect(Unit) {
        sensorViewModel.checkWorkManagerStatus()
    }

//    // React to collection status change
//    LaunchedEffect(collectionStatus) {
//        Toast.makeText(context, collectionStatus.toString(), Toast.LENGTH_SHORT).show()
//    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LocationPermissions(multiplePermissionState)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (!multiplePermissionState.allPermissionsGranted && !collectionStatus) {
                multiplePermissionState.launchMultiplePermissionRequest()
            } else if (multiplePermissionState.allPermissionsGranted && !collectionStatus) {
                val tripID=tripViewModel.updateTripId(UUID.randomUUID())
                tripViewModel.startTrip(driverProfileId = 1234L, tripID)
                sensorViewModel.startSensorCollection("START", true, tripID)
                sensorViewModel.updateCollectionStatus(true)
            }
            else if (!multiplePermissionState.allPermissionsGranted && !collectionStatus) {
                val tripID=tripViewModel.updateTripId(UUID.randomUUID())
                tripViewModel.startTrip(driverProfileId = 1234L, tripID)
                sensorViewModel.startSensorCollection("START", false, tripID)
                sensorViewModel.updateCollectionStatus(true)
            }
            else {
                tripViewModel.endTrip()
                sensorViewModel.stopSensorCollection()
                sensorViewModel.updateCollectionStatus(false)
            }
        }) {
            Text(
                text = when {
                    !multiplePermissionState.allPermissionsGranted && !collectionStatus -> "Grant Location Permissions"
                    multiplePermissionState.allPermissionsGranted && !collectionStatus -> "Start Data Collection"
                    else -> "Stop Data Collection"
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = if (collectionStatus) "Collecting Data..." else "Data Collection Stopped")
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissions(multiplePermissionState: MultiplePermissionsState) {
    val allPermissionsGranted = multiplePermissionState.allPermissionsGranted
    val shouldShowRationale = multiplePermissionState.shouldShowRationale

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





//fun startSavingRealTimeData(viewModel: SensorViewModel) {
//    // Implement your logic to start saving real-time data to the database
//}
