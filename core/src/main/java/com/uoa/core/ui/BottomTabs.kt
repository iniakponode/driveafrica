package com.uoa.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector
import com.uoa.core.R

enum class BottomTabs(
    val title: String,
    val route: String,
    val icon: ImageVector
) {
    HOME("Home", "HOME", Icons.Filled.Home),
    REPORTS("Reports", "REPORTS", Icons.Filled.Menu),
    DRIVING_TIPS("Driving Tips", "DRIVING_TIPS", Icons.Default.Info),
    Record_Trip("Record Trip", "Record_Trip", Icons.Default.PlayArrow),
    ANALYSIS("Analysis", "ANALYSIS",Icons.Filled.AddCircle)
}
