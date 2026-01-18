package com.uoa.safedriveafrica.presentation.settings

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.uoa.safedriveafrica.R
import com.uoa.core.apiServices.models.auth.FleetStatusResponse
import com.uoa.core.apiServices.workManager.enqueueImmediateUploadWork
import com.uoa.core.utils.Constants.Companion.AUTO_TRIP_DETECTION_ENABLED
import com.uoa.core.utils.Constants.Companion.MONITORING_PERMISSIONS_REQUESTED
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.Constants.Companion.REGISTRATION_FLEET_CHOICE
import com.uoa.core.utils.Constants.Companion.TRIP_DETECTION_SENSITIVITY
import com.uoa.core.utils.JOIN_FLEET_ROUTE
import com.uoa.core.utils.PreferenceUtils
import com.uoa.core.utils.SecureTokenStorage
import com.uoa.driverprofile.presentation.model.FleetEnrollmentChoice
import com.uoa.driverprofile.presentation.viewmodel.FleetStatusViewModel
import com.uoa.ml.presentation.viewmodel.TripClassificationDebugViewModel
import java.util.Locale
import androidx.core.content.edit

@Composable
fun SettingsRoute(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    var autoTripEnabled by rememberSaveable {
        mutableStateOf(prefs.getBoolean(AUTO_TRIP_DETECTION_ENABLED, false))
    }
    var allowMeteredUploads by rememberSaveable {
        mutableStateOf(PreferenceUtils.isMeteredUploadsAllowed(context))
    }
    var sensitivity by rememberSaveable {
        mutableStateOf(prefs.getString(TRIP_DETECTION_SENSITIVITY, "balanced") ?: "balanced")
    }
    var notificationsEnabled by rememberSaveable {
        mutableStateOf(areNotificationsAllowed(context))
    }
    val debugViewModel: TripClassificationDebugViewModel = hiltViewModel()
    val fleetStatusViewModel: FleetStatusViewModel = hiltViewModel()
    val fleetState by fleetStatusViewModel.state.collectAsStateWithLifecycle()
    val tokenStorage = remember { SecureTokenStorage(context) }
    val fleetChoice = remember {
        prefs.getString(REGISTRATION_FLEET_CHOICE, null)
    }
    var driverProfileId by rememberSaveable {
        mutableStateOf(prefs.getString(com.uoa.core.utils.Constants.DRIVER_PROFILE_ID, null))
    }
    val showJoinWithoutStatus = fleetChoice == FleetEnrollmentChoice.Independent.name
    val isDebuggable =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    val requestNotificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        notificationsEnabled = areNotificationsAllowed(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = areNotificationsAllowed(context)
                driverProfileId = prefs.getString(com.uoa.core.utils.Constants.DRIVER_PROFILE_ID, null)
                if (!tokenStorage.getToken().isNullOrBlank()) {
                    fleetStatusViewModel.refreshFleetStatus()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        if (!tokenStorage.getToken().isNullOrBlank()) {
            fleetStatusViewModel.refreshFleetStatus()
        }
    }

    SettingsScreen(
        notificationsEnabled = notificationsEnabled,
        canRequestNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
        onRequestNotificationPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                openNotificationSettings(context)
            }
        },
        onOpenNotificationSettings = { openNotificationSettings(context) },
        autoTripEnabled = autoTripEnabled,
        onAutoTripToggle = { enabled ->
            prefs.edit {
                putBoolean(AUTO_TRIP_DETECTION_ENABLED, enabled)
            }
            if (!enabled) {
                prefs.edit {
                    putBoolean(MONITORING_PERMISSIONS_REQUESTED, false)
                }
            }
        },
        tripSensitivity = sensitivity,
        onTripSensitivityChange = { newValue ->
//            sensitivity = newValue
            prefs.edit {
                putString(TRIP_DETECTION_SENSITIVITY, newValue)
            }
        },
        allowMeteredUploads = allowMeteredUploads,
        onAllowMeteredUploadsChange = { allowed ->
//            allowMeteredUploads = allowed
            PreferenceUtils.setMeteredUploadsAllowed(context, allowed)
        },
        fleetStatus = fleetState.fleetStatus,
        fleetStatusLoading = fleetState.isLoading,
        fleetStatusError = fleetState.errorMessage,
        showJoinWithoutStatus = showJoinWithoutStatus,
        onJoinFleetClick = { navController.navigate(JOIN_FLEET_ROUTE) },
        onCancelJoinRequest = { fleetStatusViewModel.cancelJoinRequest() },
        onRefreshFleetStatus = { fleetStatusViewModel.refreshFleetStatus() },
        onSyncNow = { enqueueImmediateUploadWork(context) },
        showDebugActions = isDebuggable,
        onDebugTripClassification = { debugViewModel.runLatestTripClassification() },
        driverProfileId = driverProfileId
    )
}

@Composable
fun SettingsScreen(
    notificationsEnabled: Boolean,
    canRequestNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    autoTripEnabled: Boolean,
    onAutoTripToggle: (Boolean) -> Unit,
    tripSensitivity: String,
    onTripSensitivityChange: (String) -> Unit,
    allowMeteredUploads: Boolean,
    onAllowMeteredUploadsChange: (Boolean) -> Unit,
    fleetStatus: FleetStatusResponse?,
    fleetStatusLoading: Boolean,
    fleetStatusError: String?,
    showJoinWithoutStatus: Boolean,
    onJoinFleetClick: () -> Unit,
    onCancelJoinRequest: () -> Unit,
    onRefreshFleetStatus: () -> Unit,
    onSyncNow: () -> Unit,
    showDebugActions: Boolean,
    onDebugTripClassification: () -> Unit,
    driverProfileId: String?
) {
    val clipboardManager = LocalClipboardManager.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = cardColors
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (notificationsEnabled) {
                        "Notifications are enabled."
                    } else {
                        "Notifications are disabled."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (!notificationsEnabled) {
                    Button(onClick = onRequestNotificationPermission) {
                        Text(text = "Enable notifications")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                if (!notificationsEnabled || !canRequestNotificationPermission) {
                    TextButton(onClick = onOpenNotificationSettings) {
                        Text(text = "Open notification settings")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = cardColors
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Driver Profile ID",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = driverProfileId ?: "Not available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Use this ID when requesting account or data deletion.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!driverProfileId.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = {
                        clipboardManager.setText(AnnotatedString(driverProfileId))
                    }) {
                        Text(text = "Copy ID")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        FleetMembershipCard(
            fleetStatus = fleetStatus,
            isLoading = fleetStatusLoading,
            errorMessage = fleetStatusError,
            showJoinWithoutStatus = showJoinWithoutStatus,
            onJoinFleetClick = onJoinFleetClick,
            onCancelJoinRequest = onCancelJoinRequest,
            onRefreshFleetStatus = onRefreshFleetStatus
        )

        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = cardColors
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto Trip Detection",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Enable background trip detection when you start driving.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoTripEnabled,
                    onCheckedChange = onAutoTripToggle
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = cardColors
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Trip Detection Sensitivity",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                SensitivityOptionRow(
                    selected = tripSensitivity == "high",
                    title = "High",
                    description = "Detect slower movement sooner.",
                    enabled = autoTripEnabled,
                    onSelect = { onTripSensitivityChange("high") }
                )
                SensitivityOptionRow(
                    selected = tripSensitivity == "balanced",
                    title = "Balanced",
                    description = "Recommended default.",
                    enabled = autoTripEnabled,
                    onSelect = { onTripSensitivityChange("balanced") }
                )
                SensitivityOptionRow(
                    selected = tripSensitivity == "low",
                    title = "Low",
                    description = "Reduce false trip starts.",
                    enabled = autoTripEnabled,
                    onSelect = { onTripSensitivityChange("low") }
                )
                if (!autoTripEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enable Auto Trip Detection to apply sensitivity changes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = cardColors
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Allow Metered Uploads",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Allow uploads over mobile data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = allowMeteredUploads,
                    onCheckedChange = onAllowMeteredUploadsChange
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = cardColors
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Manual Sync",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Upload any pending data now.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onSyncNow, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Sync now")
                }
            }
        }

        if (showDebugActions) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = cardColors
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Debug",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Run the trip ML classifier for the latest trip.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onDebugTripClassification,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Run Trip ML Check")
                    }
                }
            }
        }
    }
}

@Composable
private fun SensitivityOptionRow(
    selected: Boolean,
    title: String,
    description: String,
    enabled: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            enabled = enabled
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FleetMembershipCard(
    fleetStatus: FleetStatusResponse?,
    isLoading: Boolean,
    errorMessage: String?,
    showJoinWithoutStatus: Boolean,
    onJoinFleetClick: () -> Unit,
    onCancelJoinRequest: () -> Unit,
    onRefreshFleetStatus: () -> Unit
) {
    val status = fleetStatus?.status?.lowercase(Locale.ROOT)
    val canJoin = status == null || status == "none"
    val showJoin = status == "none" || (status == null && showJoinWithoutStatus)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_fleet_membership_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            when (status) {
                "assigned" -> {
                    Text(
                        text = stringResource(
                            R.string.settings_fleet_membership_assigned,
                            fleetStatus.fleet?.name ?: "Unknown"
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(
                            R.string.settings_fleet_membership_vehicle_group,
                            fleetStatus.vehicleGroup?.name ?: "Not assigned"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                "pending" -> {
                    Text(
                        text = stringResource(R.string.settings_fleet_membership_pending),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    fleetStatus.pendingRequest?.let { request ->
                        Text(
                            text = stringResource(
                                R.string.settings_fleet_membership_pending_fleet,
                                request.fleetName ?: "Unknown"
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = stringResource(
                                R.string.settings_fleet_membership_pending_requested,
                                request.requestedAt ?: "Unknown"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = onCancelJoinRequest,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        Text(text = stringResource(R.string.settings_fleet_cancel_button))
                    }
                }
                else -> {
                    Text(
                        text = stringResource(R.string.settings_fleet_membership_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (showJoin) {
                        Button(
                            onClick = onJoinFleetClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = !isLoading
                        ) {
                            Text(text = stringResource(R.string.settings_fleet_join_button))
                        }
                    } else if (canJoin) {
                        Text(
                            text = stringResource(R.string.settings_fleet_status_unavailable),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!isLoading) {
                TextButton(onClick = onRefreshFleetStatus) {
                    Text(text = stringResource(R.string.settings_fleet_refresh))
                }
            }

            errorMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun areNotificationsAllowed(context: Context): Boolean {
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(fallback)
    }
}
