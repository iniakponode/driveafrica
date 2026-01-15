package com.uoa.safedriveafrica

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.uoa.safedriveafrica.presentation.daappnavigation.DAAppNavHost
import com.uoa.safedriveafrica.presentation.daappnavigation.DaAppNavigationBarItem
import com.uoa.safedriveafrica.presentation.daappnavigation.TopLevelDestinations
import com.uoa.core.apiServices.session.SessionManager
import com.uoa.core.utils.DISCLAIMER_ROUTE
import com.uoa.core.utils.ENTRYPOINT_ROUTE
import com.uoa.core.utils.FILTER_SCREEN_ROUTE
import com.uoa.core.utils.REPORT_SCREEN_ROUTE
import com.uoa.core.utils.SETTINGS_ROUTE
import com.uoa.core.utils.SPLASH_ROUTE
import com.uoa.core.utils.WELCOME_ROUTE
import com.uoa.core.utils.ONBOARDING_SCREEN_ROUTE
import com.uoa.safedriveafrica.permissions.VehicleMonitoringGate
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun DAApp(
    appState: DAAppState,
    sessionManager: SessionManager,
    initialRoute: String? = null,
    onInitialRouteConsumed: (() -> Unit)? = null
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val isOffline by appState.isOffline.collectAsStateWithLifecycle()
    val currentRoute = appState.currentDestination?.route?.substringBefore("/")
    val hideBarsRoutes = setOf(
        SPLASH_ROUTE,
        WELCOME_ROUTE,
        DISCLAIMER_ROUTE,
        ONBOARDING_SCREEN_ROUTE,
        ENTRYPOINT_ROUTE,
    )
    val chromeVisible = currentRoute != null && currentRoute !in hideBarsRoutes

    VehicleMonitoringGate()

    LaunchedEffect(initialRoute) {
        if (!initialRoute.isNullOrBlank()) {
            appState.navController.navigate(initialRoute) {
                launchSingleTop = true
            }
            onInitialRouteConsumed?.invoke()
        }
    }

    LaunchedEffect(isOffline) {
        if (isOffline) {
            val result = snackbarHostState.showSnackbar(
                message = "No internet connection",
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.Dismissed) return@LaunchedEffect
        } else {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = "You are back online",
                duration = SnackbarDuration.Short
            )
        }
    }

    LaunchedEffect(sessionManager) {
        sessionManager.sessionExpiredEvents.collect {
            snackbarHostState.showSnackbar(
                message = "Session expired. Please log in again.",
                duration = SnackbarDuration.Short
            )
            appState.clearStoredDriverProfile()
            appState.navController.navigate(ENTRYPOINT_ROUTE) {
                popUpTo(appState.navController.graph.findStartDestination().id) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        topBar = {
            if (chromeVisible) {
                DATopBar(appState)
            }
        },
        bottomBar = {
            if (chromeVisible) {
                DABottomBar(
                    destinations = appState.topLevelDestinations,
                    onNavigateToDestination = appState::navigateToTopLevelDestination,
                    currentDestination = appState.currentDestination
                )
            }
        }
    ) { padding ->
        DAContent(padding, appState, snackbarHostState = snackbarHostState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DATopBar(appState: DAAppState) {
    val currentRoute = appState.currentDestination?.route
    val baseRoute = currentRoute?.substringBefore("/")
    val showBackIcon = appState.canNavigateBack() ||
        (currentRoute?.startsWith(FILTER_SCREEN_ROUTE) == true) ||
        (currentRoute?.startsWith(REPORT_SCREEN_ROUTE) == true)
    val showSettings = baseRoute != SETTINGS_ROUTE

    TopAppBar(
        title = {
            Text(stringResource(id = com.uoa.dbda.R.string.app_name))
        },
        navigationIcon = {
            if (showBackIcon) {
                IconButton(
                    onClick = {
                        if (appState.canNavigateBack()) {
                            appState.navController.navigateUp()
                        } else {
                            appState.navigateToTopLevelDestination(TopLevelDestinations.HOME)
                        }
                    }
                ) {
                    Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Back")
                }
            }
        },
        actions = {
            if (showSettings) {
                IconButton(
                    onClick = { appState.navController.navigate(SETTINGS_ROUTE) }
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            }
        }
    )
}

@Composable
fun DABottomBar(
    destinations: List<TopLevelDestinations>,
    onNavigateToDestination: (TopLevelDestinations) -> Unit,
    currentDestination: NavDestination?
) {
    NavigationBar {
        val currentRoute = currentDestination?.route?.substringBefore("/")
        destinations.forEach { destination ->
            val selected = currentRoute == destination.route
            DaAppNavigationBarItem(
                selected = selected,
                onClick = { onNavigateToDestination(destination) },
                unselectedIcon = {
                    Icon(
                        painterResource(destination.unselectedIconResId),
                        contentDescription = null
                    )
                },
                selectedIcon = {
                    Icon(
                        painterResource(destination.selectedIcon),
                        contentDescription = null
                    )
                },

                label = {
                    Text(
                        text = stringResource(id = destination.titleTextId)
                    )
                },
                enabled = true,
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun DAContent(padding: PaddingValues, appState: DAAppState, snackbarHostState: SnackbarHostState) {
    Box(modifier = Modifier
        .padding(padding)
        .fillMaxSize()
    ) {
        DAAppNavHost(
            appState = appState,
            onShowSnackbar = { message, action ->
                snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = action,
                    duration = SnackbarDuration.Short,
                ) == SnackbarResult.ActionPerformed
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
