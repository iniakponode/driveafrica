package com.uoa.driveafrica

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
import com.uoa.core.utils.internetconnectivity.NetworkMonitor
//import com.uoa.core.utils.TimeZoneMonitor
import com.uoa.core.utils.HOME_SCREEN_ROUTE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.uoa.driveafrica.presentation.daappnavigation.TopLevelDestinations
import com.uoa.nlgengine.presentation.ui.reportNavigation.FILTER_SCREEN_ROUTE
import com.uoa.nlgengine.presentation.ui.reportNavigation.REPORT_SCREEN_ROUTE
import com.uoa.sensor.presentation.ui.sensornavigation.SENSOR_CONTROL_SCREEN_ROUTE

@Composable
fun rememberDAAppState(
    networkMonitor: NetworkMonitor,
//    timeZoneMonitor: TimeZoneMonitor,
//    tripDataRepo: TripDataRepositoryImpl,
//    driverProfileRepo: DriverProfileRepository,
    coroutineScope: CoroutineScope= rememberCoroutineScope(),
    navController: NavHostController= rememberNavController(),
): DAAppState {
    NavigationTrackingSideEffect(navController)
    return remember(
        networkMonitor,
//        timeZoneMonitor,
//        tripDataRepo,
//        driverProfileRepo,
        coroutineScope,
        navController,) {
        DAAppState(
            networkMonitor = networkMonitor,
//            timeZoneMonitor = timeZoneMonitor,
//            tripDataRepo=tripDataRepo,
//            driverProfileRepo=driverProfileRepo,
            coroutineScope = coroutineScope,
            navController = navController,
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
    coroutineScope: CoroutineScope
) {
    val currentDestination: NavDestination?
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination

    val currentTopLevelDestination: TopLevelDestinations?
        @Composable get() = when (currentDestination?.route) {
            HOME_SCREEN_ROUTE -> TopLevelDestinations.HOME
            REPORT_SCREEN_ROUTE -> TopLevelDestinations.REPORTS
//            SEARCH_ROUTE -> TopLevelDestinations.SEARCH
            SENSOR_CONTROL_SCREEN_ROUTE -> TopLevelDestinations.RECORD_TRIP
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
            // Pop up to the start destination of the graph to
            // avoid building up a large stack of destinations
            // on the back stack as users select items
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            // Avoid multiple copies of the same destination when
            // reselecting the same item
            launchSingleTop = true
            // Restore state when reselecting a previously selected item
            restoreState = true
        }

        when (topLevelDestination) {
            TopLevelDestinations.REPORTS -> navController.navigate(FILTER_SCREEN_ROUTE, topLevelNavOptions)
//            TopLevelDestinations.SEARCH -> navController.navigate(SENSOR_CONTROL_SCREEN_ROUTE, topLevelNavOptions)
            TopLevelDestinations.RECORD_TRIP -> navController.navigate(SENSOR_CONTROL_SCREEN_ROUTE, topLevelNavOptions)
            TopLevelDestinations.HOME -> navController.navigate(HOME_SCREEN_ROUTE, topLevelNavOptions)
        }
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
