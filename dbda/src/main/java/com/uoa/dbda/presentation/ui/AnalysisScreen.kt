package com.uoa.dbda.presentation.ui

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uoa.dbda.presentation.viewModel.AnalysisViewModel
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.UUID

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AnalysisScreen(
    analysisViewModel: AnalysisViewModel = viewModel()
) {
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var selectedTripId by remember { mutableStateOf<UUID?>(null) }
    val analysisResult by analysisViewModel.analysisResult.observeAsState(emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Date pickers for start and end date
        DatePicker(label = "Start Date") { date ->
            startDate = date.time
        }
        DatePicker(label = "End Date") { date ->
            endDate = date.time
        }

        // Button to analyze by date range
        Button(onClick = {
            if (startDate != null && endDate != null) {
                //    suspend fun execute(startDate: Long, endDate: Long): Flow<List<RawSensorData>> {
            val zoneId = ZoneId.systemDefault()
            val sDate = Instant.ofEpochMilli(startDate!!).atZone(zoneId).toLocalDate()
            val eDate = Instant.ofEpochMilli(endDate!!).atZone(zoneId).toLocalDate()
                analysisViewModel.analyseUnsafeBehaviourByDate(sDate!!, eDate!!)
            }
        }) {
            Text("Analyze by Date Range")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Trip ID input for analysis by trip
        OutlinedTextField(
            value = selectedTripId?.toString() ?: "",
            onValueChange = {
                selectedTripId = try {
                    UUID.fromString(it)
                } catch (e: Exception) {
                    null
                }
            },
            label = { Text("Trip ID") }
        )

        // Button to analyze by trip
        Button(onClick = {
            selectedTripId?.let {
                analysisViewModel.analyseUnsafeBehaviourByTrip(it)
            }
        }) {
            Text("Analyze by Trip ID")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display the analysis results
        Log.i("AnalysisScreen", "Analysis Result: $analysisResult")
        analysisResult.forEach { rawSensorData ->
            Text("Sensor Data: ${rawSensorData.sensorType} - ${rawSensorData.values} - ${Date(rawSensorData.timestamp)}")
        }
    }
}
