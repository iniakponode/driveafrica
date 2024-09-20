package com.uoa.driveafrica.daappnavigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.uoa.driveafrica.R

//enum class TopLevelDestinations (val selectedIcon: ImageVector,
//                                 val unselectedIcon:ImageVector,
//                                 val titleTextId: Int,
//                                 val iconTextId: Int) {
//    HOME(
//        selectedIcon = Icons.Filled.Home,
//        unselectedIcon = Icons.Filled.Home,
//        titleTextId = R.string.home,
//        iconTextId = R.string.home
//    ),
//    REPORTS(
//        selectedIcon = Icons.Filled.Home,
//        unselectedIcon = Icon(
//            painter = painterResource(id = R.drawable.report), // Use your icon's resource ID here
//            contentDescription = "Custom icon",
//            tint = Color.Unspecified // You can set a color tint if needed
//        )
//        titleTextId = R.string.reports,
//        iconTextId = R.string.reports
//    ),
//
//    RECORD_TRIP(
//        selectedIcon = Icons.Filled.Home,
//        unselectedIcon = Icons.Filled.Home,
//        titleTextId = R.string.record_trip,
//        iconTextId = R.string.record_trip
//    ),
////    SEARCH(
////        selectedIcon = Icons.Filled.Home,
////        unselectedIcon = Icons.Filled.Home,
////        titleTextId = R.string.search,
////        iconTextId = R.string.search
////    ),
//    //    DRIVING_TIPS(
////        selectedIcon = Icons.Filled.Home,
////        unselectedIcon = Icons.Filled.Home,
////        titleTextId = R.string.driving_tips,
////        iconTextId = R.string.driving_tips
////    ),
////    ONBOARDING(
////        selectedIcon = Icons.Filled.Home,
////        unselectedIcon = Icons.Filled.Home,
////        titleTextId = R.string.onboarding,
////        iconTextId = R.string.onboarding
////    ),
////    ANALYSIS(
////        selectedIcon = Icons.Filled.Home,
////        unselectedIcon = Icons.Filled.Home,
////        titleTextId = R.string.analysis,
////        iconTextId = R.string.analysis
////    )
//
//}

enum class TopLevelDestinations(
    val selectedIcon: Int,
    val unselectedIconResId: Int, // Store the resource ID here
    val titleTextId: Int,
    val iconTextId: Int
) {
    HOME(
        selectedIcon = R.drawable.home,
        unselectedIconResId = R.drawable.home, // Replace with your actual unselected home icon resource
        titleTextId = R.string.home,
        iconTextId = R.string.home
    ),
    REPORTS(
        selectedIcon = R.drawable.report, // Replace with the correct selected icon if needed
        unselectedIconResId = R.drawable.history, // Replace with your actual report icon resource
        titleTextId = R.string.reports,
        iconTextId = R.string.reports
    ),
    RECORD_TRIP(
        selectedIcon =R.drawable.tips, // Replace with the correct selected icon if needed
        unselectedIconResId = R.drawable.tips, // Replace with your actual record trip icon resource
        titleTextId = R.string.record_trip,
        iconTextId = R.string.record_trip
    )
}