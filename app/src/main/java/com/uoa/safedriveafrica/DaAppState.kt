package com.uoa.safedriveafrica

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.uoa.core.ui.TrackDisposableJank
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.network.NetworkMonitor
//import com.uoa.core.utils.TimeZoneMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.uoa.safedriveafrica.presentation.daappnavigation.TopLevelDestinations
import com.uoa.core.utils.REPORT_SCREEN_ROUTE
import com.uoa.core.utils.FILTER_SCREEN_ROUTE
import com.uoa.core.utils.SENSOR_CONTROL_SCREEN_ROUTE
import androidx.compose.ui.platform.LocalContext
import com.uoa.core.utils.ENTRYPOINT_ROUTE

@Composable
fun rememberDAAppState(
    networkMonitor: NetworkMonitor,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    navController: NavHostController = rememberNavController(),
): DAAppState {
    // Get the current context from Composition
    val context = LocalContext.current.applicationContext

    NavigationTrackingSideEffect(navController)
    return remember(
        networkMonitor,
        coroutineScope,
        navController,
        context,
    ) {
        DAAppState(
            networkMonitor = networkMonitor,
            navController = navController,
            coroutineScope = coroutineScope,
            context = context // Pass it here
        )
    }
}

@Stable
class DAAppState(
    networkMonitor: NetworkMonitor,
    val navController: NavHostController,
//    timeZoneMonitor: TimeZoneMonitor,
//    tripDataRepo: TripDataRepositoryImpl,
//    driverProfileRepo: DriverProfileRepository,
    coroutineScope: CoroutineScope,
    private val context: Context
) {
    val currentDestination: NavDestination?
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination

    val currentTopLevelDestination: TopLevelDestinations?
        @Composable get() = when {
            currentDestination?.route?.startsWith(TopLevelDestinations.HOME.route) == true ->
                TopLevelDestinations.HOME
            currentDestination?.route?.startsWith(TopLevelDestinations.REPORTS.route) == true ->
                TopLevelDestinations.REPORTS
            currentDestination?.route?.startsWith(TopLevelDestinations.RECORD_TRIP.route) == true ->
                TopLevelDestinations.RECORD_TRIP
            else -> null
        }

    val isOffline = networkMonitor.isOnline
        .map (Boolean::not )
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    //    Top level destinations to be used in the bottom navigation bar
    val topLevelDestinations: List<TopLevelDestinations> = TopLevelDestinations.entries.toList()

//    val currentTimeZone = timeZoneMonitor.currentTimeZone
//        .stateIn(
//            coroutineScope,
//            SharingStarted.WhileSubscribed(5_000),
//            TimeZone.currentSystemDefault(),
//        )

    /**
     * UI logic for navigating to a top level destination in the app. Top level destinations have
     * only one copy of the destination of the back stack, and save and restore state whenever you
     * navigate to and from it.
     *
     * @param topLevelDestination: The destination the app needs to navigate to.
     */
    fun navigateToTopLevelDestination(topLevelDestination: TopLevelDestinations) {
        val topLevelNavOptions = navOptions {
            // Pop up to the start destination of the graph to avoid building
            // up a large stack of destinations on the back stack
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            // Avoid multiple copies of the same destination when reselecting
            launchSingleTop = true
            // Restore state when reselecting a previously selected item
            restoreState = true
        }


        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedProfileId = prefs.getString(DRIVER_PROFILE_ID, null)

        val resolvedRoute = if (topLevelDestination == TopLevelDestinations.HOME) {
            if (savedProfileId != null) {
                topLevelDestination.route.replace("{$DRIVER_PROFILE_ID}", savedProfileId)
            } else {
                ENTRYPOINT_ROUTE
            }
        } else {
            topLevelDestination.route
        }

        navController.navigate(resolvedRoute, topLevelNavOptions)
    }

    fun canNavigateBack(): Boolean = navController.previousBackStackEntry != null


}
    /**
     * Stores information about navigation events to be used with JankStats
     */
    @Composable
    private fun NavigationTrackingSideEffect(navController: NavHostController) {
        TrackDisposableJank(navController) { metricsHolder ->
            val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
                metricsHolder.state?.putState("Navigation", destination.route.toString())
            }

            navController.addOnDestinationChangedListener(listener)

            onDispose {
                navController.removeOnDestinationChangedListener(listener)
            }
        }
    }
