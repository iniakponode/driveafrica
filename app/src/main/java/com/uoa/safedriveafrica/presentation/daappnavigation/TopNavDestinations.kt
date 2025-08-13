package com.uoa.safedriveafrica.presentation.daappnavigation

import com.uoa.safedriveafrica.R

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
        route = "homeScreen",
    ),
    REPORTS(
        selectedIcon = R.drawable.report, // Replace with the correct selected icon if needed
        unselectedIconResId = R.drawable.history, // Replace with your actual report icon resource
        titleTextId = R.string.reports,
        route = "filterScreen",
    ),
    RECORD_TRIP(
        selectedIcon =R.drawable.tips, // Replace with the correct selected icon if needed
        unselectedIconResId = R.drawable.tips, // Replace with your actual record trip icon resource
        titleTextId = R.string.record_trip,
        route = "sensorControlScreen",
    )
}
