package com.uoa.safedriveafrica.permissions

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.uoa.core.utils.Constants.Companion.AUTO_TRIP_DETECTION_ENABLED
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.MONITORING_PERMISSIONS_REQUESTED
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.PreferenceUtils
import com.uoa.sensor.services.VehicleMovementServiceUpdate

@Composable
fun VehicleMonitoringGate() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val gateViewModel: MonitoringGateViewModel = hiltViewModel()
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var hasRequestedPermissions by rememberSaveable {
        mutableStateOf(prefs.getBoolean(MONITORING_PERMISSIONS_REQUESTED, false))
    }
    var autoMonitoringEnabled by rememberSaveable {
        mutableStateOf(prefs.getBoolean(AUTO_TRIP_DETECTION_ENABLED, false))
    }
    val driverProfileId = remember { mutableStateOf(PreferenceUtils.getDriverProfileId(context)) }
    val permissionsGranted = remember {
        mutableStateOf(hasRequiredPermissions(context, requiredPermissions()))
    }
    val isProfileReady by gateViewModel.profileReady.collectAsStateWithLifecycle()

    val requestPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val granted = hasRequiredPermissions(context, requiredPermissions())
        permissionsGranted.value = granted
        prefs.edit().putBoolean(MONITORING_PERMISSIONS_REQUESTED, true).apply()
        hasRequestedPermissions = true
        if (granted) {
            startMonitoringIfReady(context, isProfileReady)
        }
    }

    DisposableEffect(lifecycleOwner, prefs) {
        val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == DRIVER_PROFILE_ID) {
                driverProfileId.value = PreferenceUtils.getDriverProfileId(context)
            }
            if (key == MONITORING_PERMISSIONS_REQUESTED) {
                hasRequestedPermissions = prefs.getBoolean(MONITORING_PERMISSIONS_REQUESTED, false)
            }
            if (key == AUTO_TRIP_DETECTION_ENABLED) {
                autoMonitoringEnabled = prefs.getBoolean(AUTO_TRIP_DETECTION_ENABLED, false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionsGranted.value = hasRequiredPermissions(context, requiredPermissions())
                gateViewModel.refresh(driverProfileId.value)
                if (permissionsGranted.value && autoMonitoringEnabled) {
                    startMonitoringIfReady(context, isProfileReady)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    LaunchedEffect(autoMonitoringEnabled, hasRequestedPermissions, permissionsGranted.value) {
        if (autoMonitoringEnabled && !permissionsGranted.value && !hasRequestedPermissions) {
            val activity = context as? Activity ?: return@LaunchedEffect
            requestPermissionsLauncher.launch(requiredPermissions().toTypedArray())
        }
    }

    LaunchedEffect(driverProfileId.value) {
        gateViewModel.refresh(driverProfileId.value)
    }

    val shouldBlockMonitoring =
        autoMonitoringEnabled && driverProfileId.value != null && !isProfileReady

    LaunchedEffect(permissionsGranted.value, driverProfileId.value, autoMonitoringEnabled, isProfileReady) {
        if (permissionsGranted.value && autoMonitoringEnabled && isProfileReady) {
            startMonitoringIfReady(context, isProfileReady)
        } else if (!autoMonitoringEnabled) {
            stopMonitoringIfRunning(context)
        } else if (shouldBlockMonitoring) {
            stopMonitoringIfRunning(context)
        }
    }

    if (shouldBlockMonitoring) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = "Finish profile setup") },
            text = {
                Text(
                    text = "Monitoring is paused because your driver profile isn't saved on this device. " +
                        "Please log out and log back in, then tap Refresh."
                )
            },
            confirmButton = {
                TextButton(onClick = { gateViewModel.refresh(driverProfileId.value) }) {
                    Text(text = "Refresh")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    prefs.edit()
                        .putBoolean(AUTO_TRIP_DETECTION_ENABLED, false)
                        .apply()
                    autoMonitoringEnabled = false
                }) {
                    Text(text = "Turn off auto monitoring")
                }
            }
        )
    }
}

private fun requiredPermissions(): List<String> {
    val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
    }
    return permissions
}

private fun hasRequiredPermissions(context: Context, permissions: List<String>): Boolean {
    return permissions.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}

private fun startMonitoringIfReady(context: Context, isProfileReady: Boolean) {
    PreferenceUtils.getDriverProfileId(context) ?: return
    if (!isProfileReady) return
    if (isServiceRunning(context, VehicleMovementServiceUpdate::class.java)) {
        return
    }
    val intent = Intent(context, VehicleMovementServiceUpdate::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ContextCompat.startForegroundService(context, intent)
    } else {
        context.startService(intent)
    }
}

private fun stopMonitoringIfRunning(context: Context) {
    if (!isServiceRunning(context, VehicleMovementServiceUpdate::class.java)) {
        return
    }
    context.stopService(Intent(context, VehicleMovementServiceUpdate::class.java))
}

private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        ?: return false
    @Suppress("DEPRECATION")
    val services = manager.getRunningServices(Int.MAX_VALUE)
    return services.any { it.service.className == serviceClass.name }
}
