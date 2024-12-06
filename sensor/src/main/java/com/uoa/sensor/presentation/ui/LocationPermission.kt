package com.uoa.sensor.presentation.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState

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