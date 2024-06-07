package com.uoa.driveafrica.ui.components.screens

//home screen composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Home screen composable with search by date component
@Composable
fun Home() {
    Column(modifier = Modifier
        .fillMaxSize()) {
        SearchBarWithDateButton()
        Spacer(modifier = Modifier.height(5.dp))
        DailyDrivingTips()
    }
}