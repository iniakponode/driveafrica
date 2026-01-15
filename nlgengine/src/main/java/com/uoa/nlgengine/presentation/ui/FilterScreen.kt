package com.uoa.nlgengine.presentation.ui

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.uoa.core.utils.ApiKeyUtils
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import com.uoa.core.utils.PeriodType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun FilterScreen(
    navController: NavController,
    onGenerateReport: (Long?, Long?, PeriodType) -> Unit
) {
    val reportsEnabled = ApiKeyUtils.hasChatGptKey()
    var displayStartDate by remember { mutableStateOf("") }
    var displayEndDate by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var periodType by remember { mutableStateOf(PeriodType.NONE) }

    LaunchedEffect(Unit) {
        // Reset filter state when the screen is recomposed
        displayStartDate = ""
        displayEndDate = ""
        startDate = null
        endDate = null
        periodType = PeriodType.NONE
    }

    // Closure to update dates
    val updateDates = { start: Date, end: Date ->
        setPeriod(
            start, end,
            setDisplayStartDate = { displayStartDate = it },
            setDisplayEndDate = { displayEndDate = it },
            setStartDate = { startDate = it },
            setEndDate = { endDate = it }
        )
    }

    val ctx = LocalContext.current
    val id = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(DRIVER_PROFILE_ID, null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
            if (!reportsEnabled) {
                Text(
                    text = "Reports are disabled until CHAT_GPT_API_KEY is set in local.properties.",
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Text(
                text = "Period",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Blue
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Period Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                PeriodButton("Last Trip", Icons.Filled.DirectionsCar) {
                    periodType = PeriodType.LAST_TRIP
                    startDate = null
                    endDate = null
                    displayStartDate = ""
                    displayEndDate = ""
                }
                PeriodButton("Today", Icons.Filled.Today) {
                    val todayStart = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time
                    val todayEnd = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.time
                    updateDates(todayStart, todayEnd)
                    periodType = PeriodType.TODAY
                }
                PeriodButton("This Week", Icons.Filled.CalendarViewWeek) {
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val weekStart = calendar.time
                    calendar.add(Calendar.DAY_OF_WEEK, 6)
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.set(Calendar.MILLISECOND, 999)
                    val weekEnd = calendar.time
                    updateDates(weekStart, weekEnd)
                    periodType = PeriodType.THIS_WEEK
                }
                PeriodButton("Last Week", Icons.Filled.History) {
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    calendar.add(Calendar.WEEK_OF_YEAR, -1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val lastWeekStart = calendar.time
                    calendar.add(Calendar.DAY_OF_WEEK, 6)
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.set(Calendar.MILLISECOND, 999)
                    val lastWeekEnd = calendar.time
                    updateDates(lastWeekStart, lastWeekEnd)
                    periodType = PeriodType.LAST_WEEK
                }
                PeriodButton("Custom Period", Icons.Filled.EditCalendar) {
                    displayStartDate = ""
                    displayEndDate = ""
                    startDate = null
                    endDate = null
                    periodType = PeriodType.CUSTOM_PERIOD
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Start Date Picker
            DatePicker("Start Date", displayStartDate, onDateChange = { displayStartDate = it }) {
                startDate = it.time
            }

            Spacer(modifier = Modifier.height(16.dp))

            // End Date Picker
            DatePicker("End Date", displayEndDate, onDateChange = { displayEndDate = it }) {
                endDate = it.time
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ActionButton("Clear All", Icons.Filled.Delete) {
                    displayStartDate = ""
                    displayEndDate = ""
                    startDate = null
                    endDate = null
                    periodType = PeriodType.NONE
                }

//                To be activate in second phase
                ActionButton("Generate Report", Icons.Filled.Assessment, enabled = reportsEnabled) {
                    val selectedStartDate = startDate
                    val selectedEndDate = endDate

                    onGenerateReport(
                        selectedStartDate,
                        selectedEndDate,
                        periodType
                    )
                }
            }
        }
    }


@Composable
fun PeriodButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
    ) {
        Icon(icon, contentDescription = null, tint = Color.Blue)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, color = Color.Blue)
    }
}

fun setPeriod(start: Date, end: Date, setDisplayStartDate: (String) -> Unit, setDisplayEndDate: (String) -> Unit, setStartDate: (Long) -> Unit, setEndDate: (Long) -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

    // Update display dates using formatted strings
    setDisplayStartDate(dateFormat.format(start))
    setDisplayEndDate(dateFormat.format(end))

    // Update Long values for start and end dates
    setStartDate(start.time)
    setEndDate(end.time)
}



@Composable
fun ActionButton(text: String, icon: ImageVector, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, color = Color.White)
    }
}


@Composable
fun FilterScreenRoute(navController: NavController) {
    FilterScreen(
        navController = navController,
        onGenerateReport = { start, end, periodType ->
            val startDate = start ?: 0L
            val endDate = end ?: 0L
            navController.navigate("reportScreen/$startDate/$endDate/${periodType.name}")
        }
    )
}

