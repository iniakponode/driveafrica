//package com.uoa.sensor.presentation.ui
//
//// In app module
//
//import android.Manifest
//import android.os.Build
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.activity.viewModels
//import androidx.annotation.RequiresApi
//import com.uoa.sensor.presentation.viewModel.SensorViewModel
//import dagger.hilt.android.AndroidEntryPoint
//
//@AndroidEntryPoint
//class SensorActivity : ComponentActivity(), PermissionRequester {
//
//    private val sensorViewModel: SensorViewModel by viewModels()
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private val requestPermissionLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
//            // Handle permission results
//            sensorViewModel.processPermissionResult(
//                permissions.keys.toTypedArray(),
//                permissions.values.map { if (it) 1 else 0 }.toIntArray()
//            )
//        }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContent {
//            SensorControlScreen(sensorViewModel = sensorViewModel)
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    override fun requestPermissions() {
//        requestPermissionLauncher.launch(
//            arrayOf(
//                Manifest.permission.ACCESS_FINE_LOCATION,
//                Manifest.permission.BODY_SENSORS
//            )
//        )
//    }
//}
