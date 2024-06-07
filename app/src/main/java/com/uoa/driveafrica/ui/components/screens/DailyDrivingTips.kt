package com.uoa.driveafrica.ui.components.screens

// Daily driving tips screen composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Daily driving tips screen composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyDrivingTips() {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
//        SearchBar(active = false,
//            onSearch = {},
//            content = { Text(text ="Search Tips")},
//            onActiveChange = {},
//            onQueryChange = {},
//            query = ""
//        )
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