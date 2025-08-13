package com.uoa.safedriveafrica.presentation.daappnavigation

import com.uoa.safedriveafrica.R

// Define the top level destinations for the app
enum class TopLevelDestinations(
    val selectedIcon: Int,
    val unselectedIconResId: Int,
    val titleTextId: Int,
) {
    HOME(
        selectedIcon = R.drawable.home,
        unselectedIconResId = R.drawable.home,
        titleTextId = R.string.home,
    ),
    REPORTS(
        selectedIcon = R.drawable.report,
        unselectedIconResId = R.drawable.history,
        titleTextId = R.string.reports,
    ),
    RECORD_TRIP(
        selectedIcon = R.drawable.tips,
        unselectedIconResId = R.drawable.tips,
        titleTextId = R.string.record_trip,
    )
}