package com.uoa.sensor.presentation.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.uoa.sensor.presentation.ui.screens.VehicleDetectionMonitorScreen

const val VEHICLE_DETECTION_MONITOR_ROUTE = "vehicleDetectionMonitor"

fun NavController.navigateToVehicleDetectionMonitor() {
    this.navigate(VEHICLE_DETECTION_MONITOR_ROUTE)
}

fun NavGraphBuilder.vehicleDetectionMonitorScreen() {
    composable(route = VEHICLE_DETECTION_MONITOR_ROUTE) {
        VehicleDetectionMonitorScreen()
    }
}

