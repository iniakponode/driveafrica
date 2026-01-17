package com.uoa.driverprofile.presentation.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.uoa.alcoholquestionnaire.presentation.ui.questionnairenavigation.navigateToQuestionnaire
import com.uoa.core.apiServices.models.auth.FleetStatusResponse
import com.uoa.core.model.DrivingTip
import com.uoa.core.utils.ApiKeyUtils
import com.uoa.core.utils.Constants.Companion.DRIVER_EMAIL_ID
import com.uoa.core.utils.Constants.Companion.LAST_QUESTIONNAIRE_DAY
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.FILTER_SCREEN_ROUTE
import com.uoa.core.utils.JOIN_FLEET_ROUTE
import com.uoa.core.utils.SENSOR_CONTROL_SCREEN_ROUTE
import com.uoa.driverprofile.R
import com.uoa.driverprofile.presentation.ui.composables.TipList
import com.uoa.driverprofile.presentation.ui.navigation.navigateToDrivingTipDetailsScreen
import com.uoa.driverprofile.presentation.viewmodel.DrivingTipsViewModel
import com.uoa.driverprofile.presentation.viewmodel.FleetStatusViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import androidx.lifecycle.compose.collectAsStateWithLifecycle


@Composable
fun HomeScreen(
    gptDrivingTips: List<DrivingTip>,
    geminiDrivingTips: List<DrivingTip>,
    onDrivingTipClick: (UUID) -> Unit,
    onRecordTripClick: () -> Unit,
    onViewReportsClick: () -> Unit,
    onQuestionnaireClick: () -> Unit,
    onVehicleMonitorClick: () -> Unit,
    reportsEnabled: Boolean,
    tipsLoading: Boolean,
    showReminder: Boolean,
    onDismissReminder: () -> Unit
    ,
    fleetStatus: FleetStatusResponse?,
    fleetStatusLoading: Boolean,
    fleetStatusError: String?,
    onJoinFleetClick: () -> Unit
) {



    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val savedEmail = prefs.getString(DRIVER_EMAIL_ID, null)
    val aiFeaturesEnabled = ApiKeyUtils.hasChatGptKey() || ApiKeyUtils.hasGeminiKey()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        FleetStatusBanner(
            fleetStatus = fleetStatus,
            isLoading = fleetStatusLoading,
            errorMessage = fleetStatusError,
            onJoinFleetClick = onJoinFleetClick
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (!aiFeaturesEnabled || !reportsEnabled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AI features are disabled until API keys are set in local.properties.",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (showReminder) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Fill today's alcohol check-in?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        TextButton(onClick = onQuestionnaireClick) { Text("Fill in") }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = onDismissReminder) { Text("Skip for now") }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(
            text = "Hi ${savedEmail}!\nYour Driving Tips Today",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (tipsLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Checking if tips are available...")
            }
        } else if (gptDrivingTips.isEmpty() && geminiDrivingTips.isEmpty()) {
            Text("No Tips Available for today now")
        } else {
            if (gptDrivingTips.isNotEmpty()) {

                Spacer(modifier = Modifier.height(8.dp))
                TipList(
                    tips = gptDrivingTips,
                    source = "GPT",
                    onDrivingTipClick = onDrivingTipClick
                )
            }


            if (geminiDrivingTips.isNotEmpty()) {

                Spacer(modifier = Modifier.height(8.dp))
                TipList(
                    tips = geminiDrivingTips,
                    source = "Gemini",
                    onDrivingTipClick = onDrivingTipClick
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onQuestionnaireClick, modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.Filled.Assessment,
                contentDescription = "Daily questionnaire icon"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Daily Alcohol Questionnaire")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRecordTripClick, modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.Filled.DirectionsCar,
                contentDescription = "Record trip icon"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Record Trip")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onViewReportsClick, modifier = Modifier.fillMaxWidth(), enabled = reportsEnabled) {
            Icon(
                Icons.Filled.BarChart,
                contentDescription = "View reports icon"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "View Reports")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onVehicleMonitorClick, modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.Filled.Speed,
                contentDescription = "Vehicle monitor icon"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Vehicle Detection Monitor")
        }
    }
}

@Composable
private fun FleetStatusBanner(
    fleetStatus: FleetStatusResponse?,
    isLoading: Boolean,
    errorMessage: String?,
    onJoinFleetClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            when (fleetStatus?.status?.lowercase(Locale.ROOT)) {
                "assigned" -> {
                    Text(
                        text = "Fleet: ${fleetStatus.fleet?.name ?: "Unknown"}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    fleetStatus.vehicle?.let { vehicle ->
                        Text(
                            text = "Vehicle: ${vehicle.licensePlate} ${vehicle.make ?: ""} ${vehicle.model ?: ""}".trim(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                "pending" -> {
                    Text(
                        text = "Join request pending",
                        style = MaterialTheme.typography.titleMedium
                    )
                    fleetStatus.pendingRequest?.let { request ->
                        Text(
                            text = "Fleet: ${request.fleetName ?: "Unknown"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Submitted: ${request.requestedAt ?: "Unknown"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                else -> {
                    Text(
                        text = "You're not part of a fleet yet",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (!isLoading) {
                        Button(
                            onClick = onJoinFleetClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(text = stringResource(R.string.join_fleet_cta))
                        }
                    } else {
                        Text(
                            text = "Checking fleet status...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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





@Composable
fun HomeScreenRoute(
    navController: NavController,
    drivingTipsViewModel: DrivingTipsViewModel = hiltViewModel(),
    fleetStatusViewModel: FleetStatusViewModel = hiltViewModel(),
    profileId: UUID
) {


    val gpt_drivingTips by drivingTipsViewModel.gptDrivingTips.observeAsState(emptyList())
    val gemini_tips by drivingTipsViewModel.geminiDrivingTips.observeAsState(emptyList())
    val tipsLoading by drivingTipsViewModel.tipsLoading.observeAsState(false)
    val fleetState by fleetStatusViewModel.state.collectAsStateWithLifecycle()
    Log.d("HomeScreenRoute", "Observed GPT ${gpt_drivingTips.size} driving tips")
    Log.d("HomeScreenRoute", "Observed Gemini ${gemini_tips.size} driving tips")

    val onDrivingTipClick: (UUID) -> Unit = { tipId ->
        Log.d("HomeScreenRoute", "Navigating to details screen for tip: $tipId")
        navController.navigateToDrivingTipDetailsScreen(tipId)
    }

    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val lastDay = prefs.getString(LAST_QUESTIONNAIRE_DAY, null)
    val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
    var showReminder by remember { mutableStateOf(lastDay != today) }
    LaunchedEffect(profileId) {
        drivingTipsViewModel.refreshTips(profileId)
        val token = com.uoa.core.utils.SecureTokenStorage(context).getToken()
        if (!token.isNullOrBlank()) {
            fleetStatusViewModel.refreshFleetStatus()
        }
    }

    HomeScreen(
        gptDrivingTips = gpt_drivingTips,
        geminiDrivingTips = gemini_tips,
        onDrivingTipClick = onDrivingTipClick,
        onRecordTripClick = { navController.navigate(SENSOR_CONTROL_SCREEN_ROUTE) },
        onViewReportsClick = { navController.navigate(FILTER_SCREEN_ROUTE) },
        onQuestionnaireClick = { navController.navigateToQuestionnaire() },
        onVehicleMonitorClick = { navController.navigate("vehicleDetectionMonitor") },
        reportsEnabled = ApiKeyUtils.hasChatGptKey(),
        tipsLoading = tipsLoading,
        showReminder = showReminder,
        onDismissReminder = { showReminder = false }
        ,
        fleetStatus = fleetState.fleetStatus,
        fleetStatusLoading = fleetState.isLoading,
        fleetStatusError = fleetState.errorMessage,
        onJoinFleetClick = { navController.navigate(JOIN_FLEET_ROUTE) }
    )
}
