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
    val route: String,
) {
    HOME(
        selectedIcon = R.drawable.home,
        unselectedIconResId = R.drawable.home, // Replace with your actual unselected home icon resource
        titleTextId = R.string.home,
        route = HOME_SCREEN_ROUTE.substringBefore("/")
    ),
    REPORTS(
        selectedIcon = R.drawable.report, // Replace with the correct selected icon if needed
        unselectedIconResId = R.drawable.history, // Replace with your actual report icon resource
        titleTextId = R.string.reports,
        route = REPORT_SCREEN_ROUTE
    ),
    RECORD_TRIP(
        selectedIcon =R.drawable.tips, // Replace with the correct selected icon if needed
        unselectedIconResId = R.drawable.tips, // Replace with your actual record trip icon resource
        titleTextId = R.string.record_trip,
        route = SENSOR_CONTROL_SCREEN_ROUTE
    )
}