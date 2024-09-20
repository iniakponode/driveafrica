package com.uoa.dbda.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.uoa.core.model.UnsafeBehaviourModel
@Composable
fun AnalysisResultItem(result: UnsafeBehaviourModel) {
    // Implement the UI for displaying a single analysis result
    Column(modifier = Modifier.padding(8.dp)) {
        if (result.behaviorType.isNotEmpty()) {
            Text("Type: ${result.behaviorType}")
            Text("Timestamp: ${result.timestamp}")
            Text("Location ID: ${result.locationId}")
            Text("Trip ID: ${result.tripId}")
            // Add more fields as needed
        } else {
            Text("No results found")
        }
    }
}