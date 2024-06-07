package com.uoa.driveafrica.ui.components.screens

// Driving tips screen composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.uoa.driveafrica.ui.components.search.SearchByDateComponent

// Driving tips screen composable
@Composable
fun DrivingTips() {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        SearchByDateComponent()
        Spacer(modifier = Modifier.height(16.dp))
        Text("Driving Tips", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Text("1. Always wear your seatbelt", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("2. Do not use your phone while driving", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("3. Do not drink and drive", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("4. Obey traffic rules", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("5. Do not speed", style = MaterialTheme.typography.bodyMedium)
    }
}