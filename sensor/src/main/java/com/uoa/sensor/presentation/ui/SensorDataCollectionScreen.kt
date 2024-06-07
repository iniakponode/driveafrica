//package com.uoa.sensor.presentation.ui
//
//import android.Manifest
//import android.os.Build
//import android.widget.Toast
//import androidx.activity.ComponentActivity
//import androidx.annotation.RequiresApi
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.Button
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import com.google.accompanist.permissions.ExperimentalPermissionsApi
//import com.google.accompanist.permissions.MultiplePermissionsState
//import com.google.accompanist.permissions.rememberMultiplePermissionsState
//import com.uoa.sensor.presentation.viewModel.SensorViewModel
//
//@RequiresApi(Build.VERSION_CODES.O)
//@OptIn(ExperimentalPermissionsApi::class)
//@Composable
//fun SensorDataCollectionScreen(
//    sensorViewModel: SensorViewModel = viewModel()
//) {
//    val context = LocalContext.current as ComponentActivity
//    val collectionStatus by sensorViewModel.collectionStatus.collectAsState()
//
//    val multiplePermissionState = rememberMultiplePermissionsState(
//        permissions = listOf(
//            Manifest.permission.ACCESS_FINE_LOCATION,
//            Manifest.permission.ACCESS_COARSE_LOCATION
//        )
//    )
//
//    // React to collection status change
//    LaunchedEffect(collectionStatus) {
//        Toast.makeText(context, collectionStatus.toString(), Toast.LENGTH_SHORT).show()
//    }
//
//    // React to permission status change
//    LaunchedEffect(multiplePermissionState.permissions) {
//        if (multiplePermissionState.allPermissionsGranted) {
//            if (!collectionStatus) {
//                sensorViewModel.startSensorCollection("START")
//                sensorViewModel.updateCollectionStatus(true)
//            }
//        } else {
//            Toast.makeText(context, "Permissions not granted", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        LocationPermissions(multiplePermissionState)
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        Button(onClick = {
//            if (collectionStatus) {
//                sensorViewModel.stopSensorCollection("STOP")
//                sensorViewModel.updateCollectionStatus(false)
//            } else {
//                multiplePermissionState.launchMultiplePermissionRequest()
//            }
//        }) {
//            Text(
//                text =
//                if (!multiplePermissionState.allPermissionsGranted and !collectionStatus){
//                    "Grant Location Permissions"
//                }
//                else if (multiplePermissionState.allPermissionsGranted and !collectionStatus) {
//                    "Start Data Collection"
//                }
//                else
//                "Stop Data Collection")
//        }
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        Text(text = if (collectionStatus) "Collecting Data..." else "Data Collection Stopped")
//    }
//}
//
//@OptIn(ExperimentalPermissionsApi::class)
//@Composable
//fun LocationPermissions(multiplePermissionState: MultiplePermissionsState) {
//    val allPermissionsGranted = multiplePermissionState.allPermissionsGranted
//    val shouldShowRationale = multiplePermissionState.shouldShowRationale
//
//    if (!allPermissionsGranted) {
//        Text(
//            text = if (shouldShowRationale) {
//                "Location permissions are needed to collect data."
//            } else {
//                "Please grant location permissions."
//            }
//        )
//    }
//}
//
