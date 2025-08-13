package com.uoa.safedriveafrica.presentation.daappnavigation

import com.uoa.safedriveafrica.R
import com.uoa.core.utils.FILTER_SCREEN_ROUTE
import com.uoa.core.utils.HOME_SCREEN_ROUTE
import com.uoa.core.utils.SENSOR_CONTROL_SCREEN_ROUTE

// Define the top level destinations for the app
enum class TopLevelDestinations(
    val route: String,
    val selectedIcon: Int,
    val unselectedIconResId: Int, // Store the resource ID here
    val titleTextId: Int,
) {
    HOME(
        route = HOME_SCREEN_ROUTE,
        selectedIcon = R.drawable.home,
        unselectedIconResId = R.drawable.home, // Replace with your actual unselected home icon resource
        titleTextId = R.string.home,
    ),
    REPORTS(
        route = FILTER_SCREEN_ROUTE,
        selectedIcon = R.drawable.report, // Replace with the correct selected icon if needed
        unselectedIconResId = R.drawable.history, // Replace with your actual report icon resource
        titleTextId = R.string.reports,
    ),
    RECORD_TRIP(
        route = SENSOR_CONTROL_SCREEN_ROUTE,
        selectedIcon = R.drawable.tips, // Replace with the correct selected icon if needed
        unselectedIconResId = R.drawable.tips, // Replace with your actual record trip icon resource
        titleTextId = R.string.record_trip,
    )
}