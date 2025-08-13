package com.uoa.safedriveafrica.presentation.daappnavigation

import com.uoa.safedriveafrica.R
import com.uoa.core.utils.HOME_SCREEN_ROUTE
import com.uoa.core.utils.REPORT_SCREEN_ROUTE
import com.uoa.core.utils.SENSOR_CONTROL_SCREEN_ROUTE

// Top-level destinations with a full route for navigation and a baseRoute for bottom-bar highlighting.
enum class TopLevelDestinations(
    val route: String,            // Full route (may include arguments/placeholders)
    val baseRoute: String,        // Base prefix used to detect/highlight current destination
    val selectedIcon: Int,
    val unselectedIconResId: Int,
    val titleTextId: Int,
) {
    HOME(
        route = HOME_SCREEN_ROUTE,
        baseRoute = HOME_SCREEN_ROUTE.substringBefore("/"),
        selectedIcon = R.drawable.home,
        unselectedIconResId = R.drawable.home,
        titleTextId = R.string.home,
    ),
    REPORTS(
        route = REPORT_SCREEN_ROUTE,
        baseRoute = REPORT_SCREEN_ROUTE.substringBefore("/"),
        selectedIcon = R.drawable.report,
        unselectedIconResId = R.drawable.history,
        titleTextId = R.string.reports,
    ),
    RECORD_TRIP(
        route = SENSOR_CONTROL_SCREEN_ROUTE,
        baseRoute = SENSOR_CONTROL_SCREEN_ROUTE.substringBefore("/"),
        selectedIcon = R.drawable.tips,
        unselectedIconResId = R.drawable.tips,
        titleTextId = R.string.record_trip,
    )
}