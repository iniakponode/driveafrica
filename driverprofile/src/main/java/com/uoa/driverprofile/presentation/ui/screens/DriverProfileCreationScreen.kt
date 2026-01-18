package com.uoa.driverprofile.presentation.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.util.Patterns
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.uoa.driverprofile.R
import com.uoa.core.apiServices.models.driverProfile.DriverProfileResponse
import com.uoa.core.utils.Constants.Companion.DRIVER_EMAIL_ID
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.driverprofile.presentation.ui.navigation.navigateToHomeScreen
import com.uoa.driverprofile.presentation.viewmodel.AuthEvent
import com.uoa.driverprofile.presentation.viewmodel.AuthViewModel
import com.uoa.driverprofile.presentation.viewmodel.DriverProfileViewModel
import com.uoa.driverprofile.presentation.model.FleetEnrollmentChoice
import com.uoa.core.utils.JOIN_FLEET_ROUTE
import com.uoa.core.utils.ONBOARDING_FORM_ROUTE
import java.util.UUID
import java.util.Locale
import androidx.core.net.toUri
import androidx.core.content.edit
import com.uoa.driverprofile.presentation.model.RegistrationMode

private const val MAX_PASSWORD_BYTES = 72

private fun areNotificationsAllowed(context: Context): Boolean {
    val notificationManager = NotificationManagerCompat.from(context)
    val channelEnabled = notificationManager.areNotificationsEnabled()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        permissionGranted && channelEnabled
    } else {
        channelEnabled
    }
}

@Composable
fun DriverProfileCreationScreen(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isError: Boolean,
    @StringRes emailErrorMessageResId: Int?,
    isPasswordError: Boolean,
    isPasswordTooLong: Boolean,
    fleetChoice: FleetEnrollmentChoice?,
    onFleetChoiceChange: (FleetEnrollmentChoice) -> Unit,
    hasInviteCode: Boolean,
    onHasInviteCodeChange: (Boolean) -> Unit,
    showFleetChoiceError: Boolean,
    onSubmit: () -> Unit,
    isLoading: Boolean,
    statusMessage: String?,
    currentDriverProfile: DriverProfileResponse?,
    notificationsEnabled: Boolean,
    showRequestPermissionButton: Boolean,
    showOpenSettingsButton: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    authMode: AuthMode,
    onAuthModeChange: (AuthMode) -> Unit,
    authStatusMessage: String?,
    authStatusIsError: Boolean = false
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AuthModeToggleChip(
                label = stringResource(R.string.auth_mode_register),
                selected = authMode == AuthMode.Register,
                onClick = { onAuthModeChange(AuthMode.Register) }
            )
            AuthModeToggleChip(
                label = stringResource(R.string.auth_mode_login),
                selected = authMode == AuthMode.Login,
                onClick = { onAuthModeChange(AuthMode.Login) }
            )
        }
        val statusColor = if (authStatusIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        authStatusMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = statusColor,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        NotificationPermissionCard(
            notificationsEnabled = notificationsEnabled,
            showRequestPermissionButton = showRequestPermissionButton,
            showOpenSettingsButton = showOpenSettingsButton,
            onRequestPermission = onRequestPermission,
            onOpenSettings = onOpenSettings
        )

        Card(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (authMode == AuthMode.Register) {
                    FleetEnrollmentSelector(
                        selection = fleetChoice,
                        onSelect = onFleetChoiceChange,
                        hasInviteCode = hasInviteCode,
                        onHasInviteCodeChange = onHasInviteCodeChange,
                        showError = showFleetChoiceError
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text(text = stringResource(R.string.onboarding_email_label)) },
                    singleLine = true,
                    isError = isError,
                    supportingText = { Text(text = stringResource(R.string.onboarding_email_helper)) },
                    trailingIcon = { Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Email
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (!isLoading) onSubmit() }
                    )
                )

                if (isError) {
                    Text(
                        text = stringResource(emailErrorMessageResId ?: R.string.onboarding_error_empty),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text(text = stringResource(R.string.onboarding_password_label)) },
                    singleLine = true,
                    isError = isPasswordError,
                    supportingText = { Text(text = stringResource(R.string.onboarding_password_helper)) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Password
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (!isLoading) onSubmit() }
                    )
                )

                if (isPasswordError) {
                    Text(
                        text = stringResource(R.string.onboarding_password_error),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (isPasswordTooLong) {
                    Text(
                        text = stringResource(R.string.onboarding_password_too_long),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                statusMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                currentDriverProfile?.let { driver ->
                    Text(
                        text = stringResource(R.string.onboarding_signed_in, driver.email),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                val actionLabel = if (authMode == AuthMode.Login) {
                    stringResource(R.string.auth_mode_login)
                } else {
                    stringResource(R.string.onboarding_button)
                }
                Button(
                    onClick = onSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isLoading && (authMode != AuthMode.Register || fleetChoice != null),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .height(24.dp)
                                .width(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    } else {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(text = actionLabel)
                }
            }
        }
    }
}

@Composable
private fun FleetEnrollmentSelector(
    selection: FleetEnrollmentChoice?,
    onSelect: (FleetEnrollmentChoice) -> Unit,
    hasInviteCode: Boolean,
    onHasInviteCodeChange: (Boolean) -> Unit,
    showError: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.registration_fleet_choice_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.registration_fleet_choice_required),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FleetChoiceRow(
            selected = selection == FleetEnrollmentChoice.Independent,
            title = stringResource(R.string.registration_fleet_choice_independent_title),
            body = stringResource(R.string.registration_fleet_choice_independent_body),
            onClick = { onSelect(FleetEnrollmentChoice.Independent) }
        )
        FleetChoiceRow(
            selected = selection == FleetEnrollmentChoice.HaveFleet,
            title = stringResource(R.string.registration_fleet_choice_have_fleet_title),
            body = stringResource(R.string.registration_fleet_choice_have_fleet_body),
            onClick = { onSelect(FleetEnrollmentChoice.HaveFleet) }
        )
        if (selection == FleetEnrollmentChoice.HaveFleet) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = hasInviteCode,
                    onCheckedChange = onHasInviteCodeChange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.registration_fleet_choice_invite_toggle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = stringResource(R.string.registration_fleet_choice_invite_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 48.dp)
            )
        }
        if (showError) {
            Text(
                text = stringResource(R.string.registration_fleet_choice_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun FleetChoiceRow(
    selected: Boolean,
    title: String,
    body: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun NotificationPermissionCard(
    notificationsEnabled: Boolean,
    showRequestPermissionButton: Boolean,
    showOpenSettingsButton: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.notification_permission_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.notification_permission_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Text(
                text = if (notificationsEnabled) {
                    stringResource(R.string.notification_permission_enabled)
                } else {
                    stringResource(R.string.notification_permission_disabled)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )

            Text(
                text = stringResource(R.string.notification_permission_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            )

            if (showRequestPermissionButton || showOpenSettingsButton) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showRequestPermissionButton) {
                        Button(
                            onClick = onRequestPermission,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(text = stringResource(R.string.notification_permission_action))
                        }
                    }
                    if (showOpenSettingsButton) {
                        TextButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = stringResource(R.string.notification_permission_settings))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationOnboardingCard(
    notificationsEnabled: Boolean,
    showRequestPermissionButton: Boolean,
    showOpenSettingsButton: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.notification_onboarding_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.notification_onboarding_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Text(
                text = if (notificationsEnabled) {
                    stringResource(R.string.notification_permission_enabled)
                } else {
                    stringResource(R.string.notification_permission_disabled)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )

            Text(
                text = stringResource(R.string.notification_onboarding_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            )

            if (showRequestPermissionButton || showOpenSettingsButton) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showRequestPermissionButton) {
                        Button(
                            onClick = onRequestPermission,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(text = stringResource(R.string.notification_permission_action))
                        }
                    }
                    if (showOpenSettingsButton) {
                        TextButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = stringResource(R.string.notification_permission_settings))
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun OnboardingInfoRoute(
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    var notificationsEnabled by rememberSaveable { mutableStateOf(areNotificationsAllowed(context)) }
    var hasRequestedNotificationPermission by rememberSaveable { mutableStateOf(false) }
    val requestNotificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        notificationsEnabled = areNotificationsAllowed(context)
    }
    val requestNotificationPermission: () -> Unit = {
        hasRequestedNotificationPermission = true
        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = areNotificationsAllowed(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val shouldShowRationale = activity?.let {
        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.POST_NOTIFICATIONS)
    } ?: false
    val canRequestNotifications = true
    val showRequestPermissionButton = canRequestNotifications &&
        !notificationsEnabled &&
        (!hasRequestedNotificationPermission || shouldShowRationale)
    val showOpenSettingsButton = !notificationsEnabled &&
        (!canRequestNotifications || (hasRequestedNotificationPermission && !shouldShowRationale))

    LaunchedEffect(showRequestPermissionButton) {
        if (showRequestPermissionButton && !hasRequestedNotificationPermission) {
            requestNotificationPermission()
        }
    }

    val onboardingHighlights = listOf(
        stringResource(R.string.onboarding_detail_assignment),
        stringResource(R.string.onboarding_detail_questionnaire),
        stringResource(R.string.onboarding_detail_trip)
    )
    val dataHandlingBullets = listOf(
        stringResource(R.string.onboarding_data_bullet_1),
        stringResource(R.string.onboarding_data_bullet_2),
        stringResource(R.string.onboarding_data_bullet_3)
    )
    val scrollState = rememberScrollState()
    val shouldShowScrollHint by remember {
        derivedStateOf { scrollState.maxValue > 0 }
    }
    val isAtBottom by remember {
        derivedStateOf { scrollState.value >= scrollState.maxValue }
    }
    val canContinue = notificationsEnabled && (!shouldShowScrollHint || isAtBottom)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.onboarding_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f)
            )
            if (shouldShowScrollHint) {
                Text(
                    text = stringResource(R.string.onboarding_scroll_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        NotificationOnboardingCard(
            notificationsEnabled = notificationsEnabled,
            showRequestPermissionButton = showRequestPermissionButton,
            showOpenSettingsButton = showOpenSettingsButton,
            onRequestPermission = requestNotificationPermission,
            onOpenSettings = { openNotificationSettings(context) }
        )

        Card(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                onboardingHighlights.forEach { highlight ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = highlight,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        BackendSyncSummaryCard()

        DataHandlingCard(
            bullets = dataHandlingBullets,
            onOpenPrivacyPolicy = { openPrivacyPolicy(context) }
        )

        SensorPermissionCard()

        Button(
            onClick = {
                if (!notificationsEnabled) {
                    requestNotificationPermission()
                } else if (isAtBottom) {
                    onContinue()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.medium,
            enabled = canContinue
        ) {
            Text(text = stringResource(R.string.onboarding_continue_button))
        }

        if (shouldShowScrollHint && !isAtBottom) {
            Text(
                text = stringResource(R.string.onboarding_scroll_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (!notificationsEnabled) {
            Text(
                text = stringResource(R.string.notification_onboarding_continue_blocked),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

    }
}

@Composable
private fun BackendSyncSummaryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.onboarding_backend_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DataHandlingCard(
    bullets: List<String>,
    onOpenPrivacyPolicy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.onboarding_data_promise_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            bullets.forEach { bullet ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = bullet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
            Text(
                text = stringResource(R.string.onboarding_data_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )
            Text(
                text = stringResource(R.string.onboarding_privacy_policy),
                style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onOpenPrivacyPolicy() }
            )
        }
    }
}

@Composable
private fun SensorPermissionCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_sensor_permission_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    if (intent.resolveActivity(context.packageManager) == null) {
        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(fallback)
    } else {
        context.startActivity(intent)
    }
}

private fun openPrivacyPolicy(context: Context) {
    val uri = "https://datahub.safedriveafrica.com/privacy".toUri()
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

@Composable
private fun AuthModeToggleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        modifier = Modifier
            .defaultMinSize(minWidth = 120.dp)
    ) {
        Text(text = label)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun DriverProfileCreationRoute(
    navController: NavController,
    driverProfileViewModel: DriverProfileViewModel = hiltViewModel(),
    onShowSnackbar: suspend (String, String?) -> Boolean,
    registrationMode: RegistrationMode
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val savedProfileId = prefs.getString(DRIVER_PROFILE_ID, null)
    var emailState by rememberSaveable { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var emailErrorMessageResId by remember { mutableStateOf<Int?>(null) }
    val isLoading by driverProfileViewModel.isLoading.collectAsStateWithLifecycle()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val statusMessage by driverProfileViewModel.creationMessage.collectAsStateWithLifecycle()
    val currentDriverProfile by driverProfileViewModel.currentDriverProfile.collectAsStateWithLifecycle()
    var isPasswordTooLong by rememberSaveable { mutableStateOf(false) }
    var pendingUploadWork by rememberSaveable { mutableStateOf(false) }
    var pendingProfileId by rememberSaveable { mutableStateOf<UUID?>(null) }
    var notificationsEnabled by rememberSaveable { mutableStateOf(areNotificationsAllowed(context)) }
    var hasRequestedNotificationPermission by rememberSaveable { mutableStateOf(false) }
    var authMode by rememberSaveable { mutableStateOf(AuthMode.Register) }
    var fleetChoiceKey by rememberSaveable { mutableStateOf<String?>(null) }
    var hasInviteCode by rememberSaveable { mutableStateOf(false) }
    var showFleetChoiceError by rememberSaveable { mutableStateOf(false) }
    val requestNotificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        notificationsEnabled = areNotificationsAllowed(context)
    }
    val requestNotificationPermission: () -> Unit = {
        hasRequestedNotificationPermission = true
        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = areNotificationsAllowed(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val shouldShowRationale = activity?.let {
        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.POST_NOTIFICATIONS)
    } ?: false
    val canRequestNotifications = true
    val showRequestPermissionButton = canRequestNotifications &&
        !notificationsEnabled &&
        (!hasRequestedNotificationPermission || shouldShowRationale)
    val showOpenSettingsButton = !notificationsEnabled &&
        (!canRequestNotifications || (hasRequestedNotificationPermission && !shouldShowRationale))
    var showPermissionSnackbar by remember { mutableStateOf(false) }
    val notificationPermissionMessage = stringResource(R.string.notification_permission_snackbar)
    val notificationPermissionActionLabel = stringResource(R.string.notification_permission_action)

    LaunchedEffect(savedProfileId) {
        if (savedProfileId != null) {
            val profileId = try {
                UUID.fromString(savedProfileId)
            } catch (_: IllegalArgumentException) {
                null
            }
            if (profileId != null) {
                // Only auto-navigate if we already have a token to avoid pre-auth API calls
                val hasToken = com.uoa.core.utils.SecureTokenStorage(context).getToken()?.isNotBlank() == true
                if (hasToken) {
                    navController.navigateToHomeScreen(profileId)
                }
            } else {
                prefs.edit { remove(DRIVER_PROFILE_ID) }
                prefs.edit {remove(DRIVER_EMAIL_ID)}
            }
        }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            onShowSnackbar(it, null)
            driverProfileViewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(authState.errorMessage) {
        authState.errorMessage?.let {
            onShowSnackbar(it, null)
            authViewModel.clearError()
        }
    }

    LaunchedEffect(authViewModel.events) {
        authViewModel.events.collect { event ->
            if (event is AuthEvent.Authenticated) {
                val currentAuthState = authViewModel.state.value
                val fleetStatus = currentAuthState.fleetStatus
                val fleetStatusValue = fleetStatus?.status?.lowercase(Locale.ROOT)
                val fleetChoice = fleetChoiceKey?.let {
                    runCatching { FleetEnrollmentChoice.valueOf(it) }.getOrNull()
                }
                val wantsInviteCode = fleetChoice == FleetEnrollmentChoice.HaveFleet && hasInviteCode
                val hasFleetAssignment = currentAuthState.fleetAssignment != null ||
                    fleetStatusValue == "assigned" ||
                    fleetStatusValue == "pending"
                val shouldNavigateHome = authMode == AuthMode.Login ||
                    fleetChoice == FleetEnrollmentChoice.Independent ||
                    !wantsInviteCode ||
                    hasFleetAssignment
                if (shouldNavigateHome) {
                    if (notificationsEnabled) {
                        navController.navigateToHomeScreen(event.driverProfileId) {
                            popUpTo(ONBOARDING_FORM_ROUTE) { inclusive = true }
                        }
                    } else {
                        pendingUploadWork = true
                        pendingProfileId = event.driverProfileId
                        showPermissionSnackbar = true
                    }
                } else {
                    navController.navigate(JOIN_FLEET_ROUTE) {
                        popUpTo(ONBOARDING_FORM_ROUTE) { inclusive = true }
                    }
                }
            }
        }
    }

    LaunchedEffect(showPermissionSnackbar) {
        if (showPermissionSnackbar && !notificationsEnabled) {
            val actionPerformed = onShowSnackbar(notificationPermissionMessage, notificationPermissionActionLabel)
            if (actionPerformed) {
                requestNotificationPermission()
            }
        }
    }

    LaunchedEffect(notificationsEnabled, pendingUploadWork) {
        if (notificationsEnabled && pendingUploadWork) {
            driverProfileViewModel.triggerUploadWork()
        }
    }

    LaunchedEffect(notificationsEnabled, pendingProfileId) {
        val profileId = pendingProfileId
        if (notificationsEnabled && profileId != null) {
            navController.navigateToHomeScreen(profileId)
        }
    }

    var passwordState by rememberSaveable { mutableStateOf("") }
    var isPasswordError by remember { mutableStateOf(false) }
    val fleetChoice = fleetChoiceKey?.let {
        runCatching { FleetEnrollmentChoice.valueOf(it) }.getOrNull()
    }
    val onSubmit: () -> Unit = onSubmit@{
        val trimmedEmail = emailState.trim()
        val trimmedPassword = passwordState.trim()
        val emailInvalid = trimmedEmail.isBlank()
        val emailFormatInvalid = trimmedEmail.isNotEmpty() && !isValidEmail(trimmedEmail)
        val passwordInvalid = trimmedPassword.length < 6

        val passwordByteCount = trimmedPassword.encodeToByteArray().size
        isPasswordTooLong = passwordByteCount > MAX_PASSWORD_BYTES

        val fleetChoiceMissing = authMode == AuthMode.Register && fleetChoice == null
        showFleetChoiceError = fleetChoiceMissing
        emailErrorMessageResId = when {
            emailInvalid -> R.string.onboarding_error_empty
            emailFormatInvalid -> R.string.onboarding_error_invalid_email
            else -> null
        }
        isError = emailInvalid || emailFormatInvalid
        isPasswordError = passwordInvalid

        if (fleetChoiceMissing || emailInvalid || emailFormatInvalid || passwordInvalid || isPasswordTooLong) {
            return@onSubmit
        }

        if (authMode == AuthMode.Register) {
            driverProfileViewModel.createDriverProfile(
                trimmedEmail,
                trimmedPassword,
                fleetChoice ?: FleetEnrollmentChoice.Independent,
                hasInviteCode
            ) { success, profileId ->
                if (success && profileId != null) {
                    authViewModel.register(profileId, trimmedEmail, trimmedPassword)
                } else {
                    Log.e("DriverProfile", "Failed to create profile")
                }
            }
        } else {
            authViewModel.login(trimmedEmail, trimmedPassword)
        }
    }

    DriverProfileCreationScreen(
        email = emailState,
        onEmailChange = { newEmail ->
            emailState = newEmail
            val trimmedEmail = newEmail.trim()
            trimmedEmail.isBlank()
            trimmedEmail.isNotEmpty() && !isValidEmail(trimmedEmail)
        },
        password = passwordState,
        onPasswordChange = { newPassword ->
            passwordState = newPassword
            isPasswordTooLong = newPassword.encodeToByteArray().size > MAX_PASSWORD_BYTES
        },
        fleetChoice = fleetChoice,
        onFleetChoiceChange = { choice ->
            fleetChoiceKey = choice.name
            if (choice == FleetEnrollmentChoice.Independent) {
                hasInviteCode = false
            }
        },
        hasInviteCode = hasInviteCode,
        onHasInviteCodeChange = { enabled -> hasInviteCode = enabled },
        showFleetChoiceError = showFleetChoiceError,
        isError = isError,
        emailErrorMessageResId = emailErrorMessageResId,
        isPasswordError = isPasswordError,
        isPasswordTooLong = isPasswordTooLong,
        onSubmit = onSubmit,
        isLoading = isLoading || authState.isLoading,
        statusMessage = statusMessage,
        currentDriverProfile = currentDriverProfile,
        notificationsEnabled = notificationsEnabled,
        showRequestPermissionButton = showRequestPermissionButton,
        showOpenSettingsButton = showOpenSettingsButton,
        onRequestPermission = requestNotificationPermission,
        onOpenSettings = { openNotificationSettings(context) },
        authMode = authMode,
        onAuthModeChange = { mode ->
            authMode = mode
        },
        authStatusMessage = authState.errorMessage ?: authState.successMessage,
        authStatusIsError = authState.errorMessage != null
    )
}

enum class AuthMode {
    Register,
    Login
}

private fun isValidEmail(email: String): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(email).matches()
}
