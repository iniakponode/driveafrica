package com.uoa.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.ui.graphics.vector.ImageVector
import com.uoa.core.R

enum class BottomTabs(
    val title: String,
    val route: String,
    val icon: ImageVector
) {
    HOME("Home", "HOME", Icons.Filled.Home),
    REPORTS("Reports", "REPORTS", Icons.Filled.BarChart),
    DRIVING_TIPS("Driving Tips", "DRIVING_TIPS", Icons.Filled.TipsAndUpdates),
    Record_Trip("Record Trip", "Record_Trip", Icons.Filled.DirectionsCar),
    ANALYSIS("Analysis", "ANALYSIS", Icons.Filled.Insights)
}
