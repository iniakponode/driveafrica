package com.uoa.driveafrica.ui.components.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Search by date composable with start date and end date with
// a button to labelled get tips
@Composable
fun SearchByDateComponent() {
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    Column {
        DatePicker("Start Date", startDate) { selectedDate ->
            startDate = selectedDate
        }
        Spacer(modifier = Modifier.height(16.dp))
        DatePicker("End Date", endDate) { selectedDate ->
            endDate = selectedDate
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            // Call your API here with the selected start and end dates
        }) {
            Text("Get Tips")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePicker(label: String, date: String, onDateSelected: (String) -> Unit) {
    val context = LocalContext.current
    Button(onClick = {
        val datePickerDialog = android.app.DatePickerDialog(context)
        datePickerDialog.setOnDateSetListener { _, year, month, dayOfMonth ->
            val selectedDate = "$dayOfMonth/${month + 1}/$year"
            onDateSelected(selectedDate)
        }
        datePickerDialog.show()
    }) {
        Text(date.ifEmpty { label })
    }
}