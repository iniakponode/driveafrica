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
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.ml.presentation.viewmodel.AlcoholInfluenceViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
//import com.uoa.dbda.presentation.viewModel.UnsafeBehaviourViewModel
import java.util.UUID


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SensorControlScreen(
    sensorViewModel: SensorViewModel = viewModel(),
    tripViewModel: TripViewModel = viewModel(),
    alcoholInfluenceViewModel: AlcoholInfluenceViewModel = hiltViewModel()
) {
//    val context = LocalContext.current as ComponentActivity
    val collectionStatus by sensorViewModel.collectionStatus.collectAsState()
    val alcoholInfluence by alcoholInfluenceViewModel.alcoholInfluence.observeAsState()
//    val tripId by tripViewModel.currentTripId.collectAsState()
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val profileIdString = prefs.getString(DRIVER_PROFILE_ID, null)

    val driverProfileId = UUID.fromString(profileIdString)

    val coroutineScope = rememberCoroutineScope()
    val multiplePermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Check WorkManager status on screen entry
    LaunchedEffect(Unit) {
        sensorViewModel.checkWorkManagerStatus()
//        get the stored DriverProfile ID from the shared preferences

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
            val tripID=tripViewModel.updateTripId(UUID.randomUUID())
            if (!multiplePermissionState.allPermissionsGranted && !collectionStatus) {
                multiplePermissionState.launchMultiplePermissionRequest()
            } else if (multiplePermissionState.allPermissionsGranted && !collectionStatus) {
                tripViewModel.startTrip(driverProfileId, tripID)
                sensorViewModel.startSensorCollection("START", true, tripID)
                sensorViewModel.updateCollectionStatus(true)
            }
            else if (!multiplePermissionState.allPermissionsGranted && !collectionStatus) {

                tripViewModel.startTrip(driverProfileId, tripID)
                sensorViewModel.startSensorCollection("START", false, tripID)
                sensorViewModel.updateCollectionStatus(true)
            }
            else {
//                tripViewModel.endTrip(tripID)
                sensorViewModel.stopSensorCollection()
                sensorViewModel.updateCollectionStatus(false)
//              alcoholInfluenceViewModel.classifySaveAndUpdateUnsafeBehaviour(tripID)
//                alcoholInfluenceViewModel.saveInfluenceToCauseTable(tripID)
//                Log.d("Alcohol Class TripID","Classification TripID $tripID")
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

        Text(text = if (collectionStatus) "Collecting Data..." else "Data Collection Stopped, \nAlcohol Influenced: ${alcoholInfluence.toString()}")
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
        multiplePermissionState.allPermissionsGranted && !collectionStatus -> "Start Data Collection"
        else -> "Stop Data Collection"
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