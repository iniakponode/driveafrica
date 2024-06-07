package com.uoa.driveafrica.ui.components.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector

data class NavItem(
    val title: String,
    val route: String,
    val icon: ImageVector,
)

val navItems: List<NavItem> = listOf(
    NavItem(
        title = "Home",
        route = ScreenType.HOME.name,
        icon = Icons.Filled.Home,
    ),
    NavItem(
        title = "Reports",
        route = ScreenType.REPORTS.name,
        icon = Icons.Default.AddCircle
    ),
    NavItem(
        title = "Driving Tips",
        route = ScreenType.DRIVING_TIPS.name,
        icon = Icons.Default.Info
    ),
    NavItem(
        title = "Record Trip",
        route = ScreenType.Record_Trip.name,
        icon = Icons.Default.PlayArrow
    )
)
