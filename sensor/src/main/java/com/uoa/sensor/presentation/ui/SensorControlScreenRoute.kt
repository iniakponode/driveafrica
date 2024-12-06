package com.uoa.sensor.presentation.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.uoa.sensor.presentation.viewModel.SensorViewModel
import com.uoa.sensor.presentation.viewModel.TripViewModel

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SensorControlScreenRoute(
    sensorViewModel: SensorViewModel = viewModel(),
    tripViewModel: TripViewModel = viewModel()
) {
    SensorControlScreen(sensorViewModel, tripViewModel)
}