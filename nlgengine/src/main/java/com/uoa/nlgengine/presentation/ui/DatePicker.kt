package com.uoa.nlgengine.presentation.ui

import androidx.compose.runtime.Composable

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DatePicker(label: String, date: String, onDateChange: (String) -> Unit, onDateSelected: (Date) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            val selectedDate = calendar.time
            val formattedDate = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(selectedDate)
            onDateChange(formattedDate)
            onDateSelected(selectedDate)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    OutlinedTextField(
        value = date,
        onValueChange = { /* Read-only */ },
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { datePickerDialog.show() }) {
                Icon(Icons.Filled.DateRange, contentDescription = "Select Date")
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { datePickerDialog.show() }
    )
}


