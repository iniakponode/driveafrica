package com.uoa.driverprofile.presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.uoa.driverprofile.R
import com.uoa.driverprofile.presentation.viewmodel.FleetStatusViewModel
import com.uoa.driverprofile.presentation.viewmodel.JoinFleetViewModel
import com.uoa.core.utils.PreferenceUtils
import com.uoa.driverprofile.presentation.ui.navigation.navigateToHomeScreen
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinFleetScreenRoute(
    navController: NavController,
    joinFleetViewModel: JoinFleetViewModel = hiltViewModel(),
    fleetStatusViewModel: FleetStatusViewModel = hiltViewModel()
) {
    val uiState by joinFleetViewModel.state.collectAsStateWithLifecycle()
    val fleetState by fleetStatusViewModel.state.collectAsStateWithLifecycle()
    val joinRequestRefresh = remember { joinFleetViewModel.refreshTrigger }
    val context = androidx.compose.ui.platform.LocalContext.current
    val storedProfileId = remember {
        PreferenceUtils.getDriverProfileId(context)
    }

    LaunchedEffect(joinRequestRefresh) {
        joinRequestRefresh.collectLatest {
            fleetStatusViewModel.refreshFleetStatus()
        }
    }

    LaunchedEffect(fleetState.fleetStatus) {
        val status = fleetState.fleetStatus?.status?.lowercase(Locale.ROOT)
        if ((status == "assigned" || status == "pending") && storedProfileId != null) {
            navController.navigateToHomeScreen(storedProfileId) {
                popUpTo(navController.graph.startDestinationId) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    val inviteCodeState = rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.join_fleet_title)) },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text(stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.join_fleet_prompt),
                style = MaterialTheme.typography.bodyLarge
            )

            OutlinedTextField(
                value = inviteCodeState.value,
                onValueChange = { inviteCodeState.value = it },
                label = { Text(stringResource(R.string.join_fleet_code_label)) },
                isError = uiState.errorMessage != null,
                singleLine = true,
                enabled = !uiState.isLoading && uiState.requestStatus?.lowercase(Locale.ROOT) != "pending",
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Ascii
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        joinFleetViewModel.submitJoinRequest(inviteCodeState.value)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = { joinFleetViewModel.submitJoinRequest(inviteCodeState.value) },
                enabled = !uiState.isLoading && uiState.requestStatus?.lowercase(Locale.ROOT) != "pending",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(24.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(text = if (uiState.requestStatus?.lowercase(Locale.ROOT) == "pending") {
                    stringResource(R.string.join_fleet_pending_button)
                } else {
                    stringResource(R.string.join_fleet_submit)
                })
            }

            if (uiState.success) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.join_fleet_success_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        uiState.fleetName?.let { fleet ->
                            Text(
                                text = stringResource(R.string.join_fleet_success_fleet, fleet),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        uiState.requestStatus?.let { status ->
                            Text(
                                text = stringResource(R.string.join_fleet_success_status, status),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.join_fleet_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
