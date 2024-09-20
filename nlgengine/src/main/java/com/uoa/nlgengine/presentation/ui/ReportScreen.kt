package com.uoa.nlgengine.presentation.ui

// A composable screen that displays the AI generated Driving Behaviour report to the user
// Beutifully designed with Jetpack Compose and scrollable with a back button and navigation
// to previous screen.

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.layout.FlowColumnScopeInstance.align
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.uoa.core.ui.DAAppTopNavBar
import com.uoa.nlgengine.data.model.UnsafeBehaviorChartEntry
import com.uoa.nlgengine.presentation.ui.theme.NLGEngineTheme
import com.uoa.nlgengine.presentation.viewmodel.LocalUnsafeBehavioursViewModel
import com.uoa.nlgengine.presentation.viewmodel.LocationRoadViewModel
import com.uoa.nlgengine.presentation.viewmodel.chatgpt.ChatGPTViewModel
import com.uoa.nlgengine.util.PeriodType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ReportScreen(
    navController: NavController,
    reportContent: String,
    reportPeriod: Pair<LocalDate, LocalDate>? = null,
    periodType: PeriodType,
    chartData: List<UnsafeBehaviorChartEntry> = emptyList()

) {

    NLGEngineTheme {
        Scaffold(
            topBar = {
                DAAppTopNavBar(
                    navigateBack = { navController.navigateUp() },
                    navigateHome = { navController.navigate("homeScreen") }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.onPrimary)
                    .padding(paddingValues)
                    .padding(5.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Text(
                        text = "Your Driving Behaviour Report",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val formattedDate = LocalDate.now()
                            .format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                        Text(
                            text = "Date of Report: $formattedDate",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                item {
                    // Display the period type with styling
                    val periodText = when (periodType) {
                        PeriodType.TODAY -> "Report for Today"
                        PeriodType.THIS_WEEK -> "Report for This Week"
                        PeriodType.LAST_WEEK -> "Report for Last Week"
                        PeriodType.CUSTOM_PERIOD -> {
                            val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")
                            val formattedStartDate = reportPeriod?.first?.format(formatter) ?: "N/A"
                            val formattedEndDate = reportPeriod?.second?.format(formatter) ?: "N/A"
                            "Report Period: From $formattedStartDate To $formattedEndDate"
                        }
                        PeriodType.LAST_TRIP -> "Report for the Last Trip"
                        else -> "No Filter Selected"
                    }
                    Text(
                        text = periodText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    )
                }
                item {
                    // Use a card to display the report content
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = reportContent,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = 24.sp
                                ),
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                                    .align(Alignment.End)
                            )
                        }
                    }
                }
//                item {
//                    Text(
//                        text = "Unsafe Behaviours Over Time",
//                        style = MaterialTheme.typography.titleMedium.copy(
//                            fontWeight = FontWeight.Bold,
//                            color = MaterialTheme.colorScheme.primary
//                        ),
//                        modifier = Modifier.padding(vertical = 8.dp)
//                    )
//
//                    if (chartData.isEmpty()) {
//                        Text(
//                            text = "No data available for the chart.",
//                            style = MaterialTheme.typography.bodyMedium,
//                            modifier = Modifier.padding(16.dp)
//                        )
//                    } else {
//                        // Display the chart
//                        ChartScreen(chartData = chartData)
//                    }
//                }
            }
        }
    }
}


@Composable
fun ReportScreenRoute(
    navController: NavController,
    periodType: PeriodType,
    startDate: Long,
    endDate: Long,
    chatGPTViewModel: ChatGPTViewModel = hiltViewModel(),
    unsafeBehavioursViewModel: LocalUnsafeBehavioursViewModel = hiltViewModel(),
    locationAddressViewModel: LocationRoadViewModel = hiltViewModel()
) {
    // Convert the start and end date to LocalDate
    val zoneId = ZoneId.systemDefault()
    val sDate = Instant.ofEpochMilli(startDate).atZone(zoneId).toLocalDate()
    val eDate = Instant.ofEpochMilli(endDate).atZone(zoneId).toLocalDate()

    // Observe the unsafe behaviours and other data from the view model
    val unsafeBehaviours by unsafeBehavioursViewModel.unsafeBehaviours.collectAsState()
    val reportContent by chatGPTViewModel.response.observeAsState("")
    val reportPeriod by unsafeBehavioursViewModel.reportPeriod.observeAsState(Pair(sDate, eDate))
    val lastTripId by unsafeBehavioursViewModel.lastTripId.observeAsState()
    val isGeneratingSummary by chatGPTViewModel.isLoading.collectAsState()
    val isLoadingBehaviours by unsafeBehavioursViewModel.isLoading.collectAsState()
    val isGeneratingPrompt by locationAddressViewModel.isLoading.collectAsState()

    // Combined loading state
    val isLoading = isGeneratingSummary && isLoadingBehaviours && isGeneratingPrompt

    // Collect the summary data
    val summaryDataByDateHour by locationAddressViewModel.summaryDataByDateHour.collectAsState()

    // Prepare chart data
    val chartData = remember(summaryDataByDateHour) {
        locationAddressViewModel.prepareChartData(summaryDataByDateHour)
    }

    // Fetch the unsafe behaviours when the screen loads
    LaunchedEffect(periodType, startDate, endDate) {
        when (periodType) {
            PeriodType.TODAY, PeriodType.THIS_WEEK, PeriodType.LAST_WEEK, PeriodType.CUSTOM_PERIOD -> {
//                Log.d("ReportScreen", "Fetching unsafe behaviours between $sDate and $eDate")
                unsafeBehavioursViewModel.getUnsafeBehavioursBetweenDates(sDate, eDate)
            }

            PeriodType.LAST_TRIP -> {
                Log.d("ReportScreen", "Fetching unsafe behaviours for the last trip")
                unsafeBehavioursViewModel.getUnsafeBehaviourByTripId()
            }

            else -> {
                Log.i("ReportScreen", "Invalid dataSource: ${periodType.name}")
            }
        }
    }

    // React to changes in unsafeBehaviours
    LaunchedEffect(unsafeBehaviours) {
        if (unsafeBehaviours.isNotEmpty()) {
//            Log.d("ReportScreen", "Unsafe behaviours retrieved: ${unsafeBehaviours.size} records")

            // Call the ViewModel function to generate the prompt
            locationAddressViewModel.generatePromptForBehaviours(unsafeBehaviours, periodType)
        } else {
            Log.i("ReportScreen", "No unsafe behaviours found for the given criteria.")
        }
    }

    // Observe the generated prompt and send it to chatGPTViewModel
    val generatedPrompt by locationAddressViewModel.generatedPrompt.collectAsState()
    LaunchedEffect(generatedPrompt) {
        if (generatedPrompt.isNotEmpty()) {
//            Log.d("ReportScreen", "Generated Prompt: $generatedPrompt")
            chatGPTViewModel.getChatGPTPrompt(generatedPrompt)
        }
    }


    // Report screen UI setup
//    val databaseCheckedDataLoadedReportGenerated=!isGeneratingSummary && !isLoadingBehaviours && !isGeneratingPrompt && unsafeBehaviours.isNotEmpty() && generatedPrompt.isNotEmpty() && reportContent.isNotEmpty()
//    val databaseCheckedNoDataNoReport=!isLoadingBehaviours && unsafeBehaviours.isEmpty() && !isGeneratingPrompt && generatedPrompt.isEmpty() && reportContent.isEmpty()

    val isDataLoadedAndReportGenerated by remember(isGeneratingSummary, isLoadingBehaviours, isGeneratingPrompt, unsafeBehaviours, generatedPrompt, reportContent) {
        mutableStateOf(
            !isGeneratingSummary && !isLoadingBehaviours && !isGeneratingPrompt &&
                    unsafeBehaviours.isNotEmpty() && generatedPrompt.isNotEmpty() && reportContent.isNotEmpty()
        )
    }

    val isNoDataAndNoReport by remember(isLoadingBehaviours, unsafeBehaviours, isGeneratingPrompt, generatedPrompt, reportContent) {
        mutableStateOf(
            !isLoadingBehaviours && unsafeBehaviours.isEmpty() &&
                    !isGeneratingPrompt && generatedPrompt.isEmpty() && reportContent.isEmpty()
        )
    }
    if (isDataLoadedAndReportGenerated) {
        ReportScreen(
            navController = navController,
            reportContent = reportContent,
            reportPeriod = reportPeriod,
//            tripId = lastTripId?.toString(),
            periodType = periodType,
            chartData = chartData
        )
    }
    else if (periodType!=PeriodType.TODAY && periodType!=PeriodType.THIS_WEEK && periodType!=PeriodType.LAST_WEEK && periodType!=PeriodType.CUSTOM_PERIOD && periodType!=PeriodType.LAST_TRIP) {
        Log.d("ReportScreen", "${periodType.name} is not a valid period type")
        ReportScreen(
            navController = navController,
            reportContent = "Please click on a period, last trip or enter custom date period to generate a report",
            reportPeriod = reportPeriod,
            periodType = periodType,
            chartData = chartData
        )
    }
    else if (isNoDataAndNoReport) {
        Log.d("ReportScreen", "${periodType}.")
            ReportScreen(
                navController = navController,
                reportContent = "You have no trips recorded that matches your selection at the moment.",
                reportPeriod = reportPeriod,
//                tripId = lastTripId?.toString(),
                periodType = periodType,
                chartData = chartData
            )
        }
    else {
        // Show loading indicator
        Text(
            modifier = Modifier.padding(16.dp),

            text = "Generating your report, please wait...",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .size(10.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {

            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
            )

        }
    }
}



