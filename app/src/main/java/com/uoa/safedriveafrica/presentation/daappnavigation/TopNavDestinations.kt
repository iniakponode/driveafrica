package com.uoa.safedriveafrica.presentation.daappnavigation

import com.uoa.safedriveafrica.R
import com.uoa.core.utils.HOME_SCREEN_ROUTE
import com.uoa.core.utils.REPORT_SCREEN_ROUTE
import com.uoa.core.utils.SENSOR_CONTROL_SCREEN_ROUTE

// Define the top level destinations for the app
enum class TopLevelDestinations(
    val selectedIcon: Int,
    val unselectedIconResId: Int, // Store the resource ID here
    val titleTextId: Int,
    val route: String,            // Base route (prefix) for matching/highlighting
) {
    HOME(
        selectedIcon = R.drawable.home,
        unselectedIconResId = R.drawable.home,
        titleTextId = R.string.home,
        route = HOME_SCREEN_ROUTE.substringBefore("/")
    ),
    REPORTS(
        selectedIcon = R.drawable.report,
        unselectedIconResId = R.drawable.history,
        titleTextId = R.string.reports,
        route = REPORT_SCREEN_ROUTE.substringBefore("/")
    ),
    RECORD_TRIP(
        selectedIcon = R.drawable.tips,
        unselectedIconResId = R.drawable.tips,
        titleTextId = R.string.record_trip,
        route = SENSOR_CONTROL_SCREEN_ROUTE.substringBefore("/")
    )
}