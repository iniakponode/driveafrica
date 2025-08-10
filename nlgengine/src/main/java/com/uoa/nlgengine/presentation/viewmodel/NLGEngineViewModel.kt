package com.uoa.nlgengine.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.database.repository.ReportStatisticsRepository
import com.uoa.core.database.repository.RoadRepository
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.database.repository.UnsafeBehaviourRepository
import com.uoa.core.model.LocationData
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.core.network.apiservices.OSMRoadApiService
import com.uoa.core.utils.toDomainModel
import com.uoa.nlgengine.data.model.DateHourKey
import com.uoa.nlgengine.data.model.HourlySummary
import com.uoa.core.model.ReportStatistics
import com.uoa.core.network.apiservices.OSMSpeedLimitApiService
import com.uoa.nlgengine.data.model.UnsafeBehaviorChartEntry
import com.uoa.core.utils.GetLastInsertedUnsafeBehaviourUseCase
import com.uoa.core.utils.PeriodType
import com.uoa.core.utils.PeriodUtils
import com.uoa.core.utils.toEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import com.uoa.core.utils.computeReportStatistics
import javax.inject.Inject

@HiltViewModel
class NLGEngineViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val tripRepository: TripDataRepository,
    private val osmRoadApiService: OSMRoadApiService,
    private val lastInsertedUnsafeBehaviourUseCase: GetLastInsertedUnsafeBehaviourUseCase,
    private val reportStatisticsRepository: ReportStatisticsRepository,
    private val unsafeBehaviourRepository: UnsafeBehaviourRepository,
    private val osmSpeedLimitApiService: OSMSpeedLimitApiService,
    private val roadRepository: RoadRepository,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _generatedPrompt = MutableStateFlow("")
    val generatedPrompt: StateFlow<String> get() = _generatedPrompt

    private val _summaryDataByDateHour = MutableStateFlow<Map<DateHourKey, HourlySummary>>(emptyMap())
    val summaryDataByDateHour: StateFlow<Map<DateHourKey, HourlySummary>> get() = _summaryDataByDateHour

    /**
     * Returns cached report statistics. For LAST_TRIP, it checks whether the last inserted trip exists;
     * if so, it fetches the corresponding report and converts it to the domain model only if non-null.
     */
    suspend fun getCachedReportStats(periodType: PeriodType): ReportStatistics? {
        return if (periodType == PeriodType.LAST_TRIP) {
            val tripId = tripRepository.getLastInsertedTrip()?.id
            if (tripId != null) {
                val cachedReportStat = reportStatisticsRepository.getReportByTripId(tripId)
                if (cachedReportStat != null) {
                    cachedReportStat.toDomainModel()
                } else {
                    Log.w("NLGEngineViewModel", "No cached report statistics found for tripId: $tripId")
                    null
                }
            } else {
                Log.w("NLGEngineViewModel", "No last inserted trip found")
                null
            }
        } else {
            val periodOfReport = PeriodUtils.getReportingPeriod(periodType)
            if (periodOfReport != null) {
                reportStatisticsRepository.getReportsBetweenDates(periodOfReport.first, periodOfReport.second)
            } else {
                Log.w("NLGEngineViewModel", "No reporting period found for periodType: $periodType")
                null
            }
        }
    }

    /**
     * Generates the prompt by either using cached report statistics or computing new ones.
     * This method runs on the IO dispatcher and updates the loading state accordingly.
     */
    private suspend fun generatePrompt(
        context: Context,
        unsafeBehaviours: List<UnsafeBehaviourModel>,
        periodType: PeriodType,
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate,
    ): String {
        _isLoading.value = true
        return withContext(Dispatchers.IO) {
            try {
//                val cachedReportStats = getCachedReportStats(periodType)
                val cachedReportStats = withContext(Dispatchers.IO) {
                    getCachedReportStats(periodType)
                }
                val prompt = if (cachedReportStats == null) {
                    val reportStatistics = computeReportStatistics(
                        context,
                        osmRoadApiService,
                        osmSpeedLimitApiService,
                        roadRepository,
                        startDate,
                        endDate,
                        periodType,
                        unsafeBehaviours,
                        tripRepository,
                        locationRepository,
                        lastInsertedUnsafeBehaviourUseCase
                    )
                    if (reportStatistics == null) {
                        Log.e("NLGEngineViewModel", "Report statistics computation returned null")
                        return@withContext "Unable to generate prompt due to missing report statistics."
                    }
                    // Cache the processed report statistics
                    val reportStat = reportStatistics.copy(processed = true)
                    reportStatisticsRepository.insertReportStatistics(reportStat)
                    buildPrompt(context, unsafeBehaviours, periodType, reportStatistics)
                } else {
                    buildPrompt(context, unsafeBehaviours, periodType, cachedReportStats)
                }
                prompt
            } catch (e: Exception) {
                Log.e("NLGEngineViewModel", "Error generating prompt", e)
                ""
            } finally {
                _isLoading.value = false
            }
        }
    }


//    /**
//     * Builds the prompt string using the provided unsafe behaviors and report statistics.
//     * Uses formatted dates/times and includes all required report details.
//     */
//    private suspend fun buildPrompt(
//        context: Context,
//        unsafeBehaviours: List<UnsafeBehaviourModel>,
//        periodType: PeriodType,
//        reportStatistics: ReportStatistics
//    ): String {
//        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
//        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
//
//        // Determine the start and end dates from the unsafe behaviors
//        val startDateStr = unsafeBehaviours.minOfOrNull { it.timestamp }?.let {
//            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
//        }
//        val endDateStr = unsafeBehaviours.maxOfOrNull { it.timestamp }?.let {
//            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
//        }
//
//        val periodText = when (periodType) {
//            PeriodType.TODAY -> "Report for Today"
//            PeriodType.THIS_WEEK -> "Report for This Week"
//            PeriodType.LAST_WEEK -> "Report for Last Week"
//            PeriodType.CUSTOM_PERIOD -> {
//                if (startDateStr != null && endDateStr != null) {
//                    if (startDateStr == endDateStr) "Report for $startDateStr" else "Report Period: $startDateStr to $endDateStr"
//                } else {
//                    "Report for Selected Period"
//                }
//            }
//            PeriodType.LAST_TRIP -> "Report for the Last Trip"
//            else -> "Report"
//        }
//
//        // Build the prompt using a StringBuilder
//        val promptBuilder = StringBuilder().apply {
//            append("$periodText\n\n")
//            append("You are a driving safety specialist in Nigeria. Based on the following data, generate a friendly and encouraging driving behavior report for the driver on their selected report filter ($periodType). The report should:\n")
//            append("- Must have only between 150-200 words.\n")
//            append("- Acknowledge the driver's efforts and positive aspects.\n")
//            append("- Gently highlight areas for improvement without direct criticism.\n")
//            append("- Offer practical, actionable tips to enhance safety.\n")
//            append("- Use an uplifting and motivational tone.\n")
//            append("- The sender's name in the complimentary closing section should be 'Your Driving Safety Specialist Agent'.\n")
//            append("- Must include the numbers given in this prompt in the response without hallucination.\n")
//            append("- You will need to decode the Base64 encoded JSON data to get the driving offences, penalties, fines, and laws.\n")
//            append("- Reference the specific dates, incidents, and locations provided.\n")
//            append("- Ensure to use only the given data for fines, laws, and sections without hallucination.\n\n")
//            append("After generating the report, please ensure the response includes the key components of the Theory of Planned Behavior (TPB), specifically:\n")
//            append("- **Attitudes**: Reflect on the driver's positive and negative evaluations of performing the behavior.\n")
//            append("- **Subjective Norms**: Reference the social pressure or norms influencing the driver's behavior.\n")
//            append("- **Perceived Behavioral Control**: Address the driver's perception of their ability to perform the behavior.\n")
//            append("Also, ensure that the report:\n")
//            append("- Maintains a supportive tone throughout.\n")
//            append("- Specifies the unsafe behaviors observed.\n")
//            append("- Include the corresponding location (Road name) as given from the data and time for the most frequent incident.\n")
//            append("- State the risks inherent in the identified unsafe behaviours.\n")
//            append("- State the benefits inherent especially in economic (fuel consumption, vehicle health, etc) and life related terms if the drivers could improve and change from the identified unsafe behaviours.\n")
//            append("- Has a statement about the alcohol influence based on the given data.\n")
//            append("- Strictly adheres to the data provided in this prompt without adding any information not directly traceable to the prompt.\n\n")
//            append("- If any of the above components are missing, please identify and include them before finalizing the response.\n\n")
//            append("Report Statistics are given below:\n")
//            // Append common statistics
//            append("Total Unsafe Behaviors: ${reportStatistics.totalIncidences}\n")
//            if (reportStatistics.mostFrequentUnsafeBehaviour != null) {
//                append("Most Frequent Unsafe Behaviour: ${reportStatistics.mostFrequentUnsafeBehaviour}\n")
//                append("Occurrences: ${reportStatistics.mostFrequentBehaviourCount}\n")
//                append("Occurrences Details:\n")
//                reportStatistics.mostFrequentBehaviourOccurrences.forEach { occurrence ->
//                    val dateStr = occurrence.date.format(dateFormatter)
//                    val timeStr = occurrence.time.format(timeFormatter)
//                    append("- Date: $dateStr, Time: $timeStr, Road: ${occurrence.roadName}\n")
//                }
//            }
//            // Append period-specific statistics
//            when (periodType) {
//                PeriodType.LAST_TRIP -> {
//                    append("Trip Duration: ${reportStatistics.lastTripDuration} minutes\n")
//                    append("Distance Covered: ${"%.2f".format(reportStatistics.lastTripDistance)} km\n")
//                    append("Average Speed: ${"%.2f".format(reportStatistics.lastTripAverageSpeed)} km/h\n")
//                    append("Start Location: ${reportStatistics.lastTripStartLocation}\n")
//                    append("End Location: ${reportStatistics.lastTripEndLocation}\n")
//                    append("Most Frequent Unsafe Behaviour: ${reportStatistics.mostFrequentUnsafeBehaviour}\n")
//                    append("Most Frequent Unsafe Behaviour Count: ${reportStatistics.mostFrequentBehaviourCount}\n")
//                    append("Most Frequent Unsafe Behaviour Occurrences: ${reportStatistics.mostFrequentBehaviourOccurrences}\n")
//                    append("Alcohol Influence: ${reportStatistics.lastTripInfluence}\n")
//                }
//                else -> {
//                    append("Number of Trips: ${reportStatistics.numberOfTrips}\n")
//                    append("Number of Trips with Incidences: ${reportStatistics.numberOfTripsWithIncidences}\n")
//                    append("Incidences Per Trip: ${reportStatistics.incidencesPerTrip}\n")
//                    append("Number of Trips with Alcohol Influence: ${reportStatistics.numberOfTripsWithAlcoholInfluence}\n")
//                    if (reportStatistics.mostFrequentUnsafeBehaviour != null) {
//                        append("Count of Most Frequent Unsafe Behaviour: ${reportStatistics.mostFrequentBehaviourCount}\n")
//                        append("Occurrence instances (dates, times and road names) of Most Frequent Unsafe Behaviour: ${reportStatistics.mostFrequentBehaviourOccurrences}\n")
//                    }
//                    if (reportStatistics.tripWithMostIncidences != null) {
//                        val tripStartTime = Instant.ofEpochMilli(reportStatistics.tripWithMostIncidences!!.startTime)
//                            .atZone(ZoneId.systemDefault()).toLocalDateTime()
//                        append("Trip with Most Incidences Start Time: ${tripStartTime.format(dateFormatter)}\n")
//                    }
//                    if (reportStatistics.aggregationUnitWithMostIncidences != null) {
//                        append("Period with Most Incidences: ${reportStatistics.aggregationUnitWithMostIncidences}\n")
//                    }
//                }
//            }
//        }
//        return promptBuilder.toString()
//    }

//    private suspend fun buildPrompt(
//        context: Context,
//        unsafeBehaviours: List<UnsafeBehaviourModel>,
//        periodType: PeriodType,
//        reportStatistics: ReportStatistics
//    ): String {
//        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
//
//        val startDateStr = unsafeBehaviours.minOfOrNull { it.timestamp }?.let {
//            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
//        }
//        val endDateStr = unsafeBehaviours.maxOfOrNull { it.timestamp }?.let {
//            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
//        }
//
//        val periodText = when (periodType) {
//            PeriodType.TODAY -> "Today's Report"
//            PeriodType.THIS_WEEK -> "Weekly Report"
//            PeriodType.LAST_WEEK -> "Last Week's Report"
//            PeriodType.CUSTOM_PERIOD -> startDateStr?.let { "Report from $startDateStr to $endDateStr" } ?: "Custom Period Report"
//            PeriodType.LAST_TRIP -> "Last Trip Report"
//            else -> "Driving Report"
//        }
//
//        return buildString {
//            append("$periodText\n\n")
//            append("Generate a concise, motivational driving behavior report based on the following statistics. Follow these rules:\n")
//            append("- Length: 150-200 words.\n")
//            append("- Acknowledge good driving habits.\n")
//            append("- Highlight areas for improvement positively.\n")
//            append("- Provide actionable, data-driven tips.\n")
//            append("- Mention specific dates, incidents, and locations.\n")
//            append("- Do not introduce information not present in this data.\n\n")
//            append("Please ensure the response includes the key components of the Theory of Planned Behavior (TPB), specifically:\n")
//            append("- **Attitudes**: Reflect on the driver's positive and negative evaluations of performing the behavior.\n")
//            append("- **Subjective Norms**: Reference the social pressure or norms influencing the driver's behavior.\n")
//            append("- **Perceived Behavioral Control**: Address the driver's perception of their ability to perform the behavior.\n")
//
//            append("Key Statistics:\n")
//            append("Total Incidences: ${reportStatistics.totalIncidences}\n")
//            append("Alcohol Influence: ${reportStatistics.lastTripInfluence}\n")
//            reportStatistics.mostFrequentUnsafeBehaviour?.let {
//                append("Most Frequent Issue: $it (${reportStatistics.mostFrequentBehaviourCount} times)\n")
//            }
//            reportStatistics.mostFrequentBehaviourOccurrences.take(3).forEach { occurrence ->
//                append("- ${occurrence.date.format(dateFormatter)}, ${occurrence.roadName}\n")
//            }
//            if (periodType == PeriodType.LAST_TRIP) {
//                append("Trip Duration: ${reportStatistics.lastTripDuration} min\n")
//                append("Distance: ${"%.2f".format(reportStatistics.lastTripDistance)} km\n")
//                append("Avg Speed: ${"%.2f".format(reportStatistics.lastTripAverageSpeed)} km/h\n")
//            }
//        }
//    }


    private suspend fun buildPrompt(
        context: Context,
        unsafeBehaviours: List<UnsafeBehaviourModel>,
        periodType: PeriodType,
        reportStatistics: ReportStatistics
    ): String {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        val startDateStr = unsafeBehaviours.minOfOrNull { it.timestamp }?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
        }
        val endDateStr = unsafeBehaviours.maxOfOrNull { it.timestamp }?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
        }

        val periodText = when (periodType) {
            PeriodType.TODAY -> "Report for Today"
            PeriodType.THIS_WEEK -> "Report for This Week"
            PeriodType.LAST_WEEK -> "Report for Last Week"
            PeriodType.CUSTOM_PERIOD -> {
                if (startDateStr != null && endDateStr != null) {
                    if (startDateStr == endDateStr) "Report for $startDateStr" else "Report: $startDateStr to $endDateStr"
                } else "Report for Selected Period"
            }
            PeriodType.LAST_TRIP -> "Report for Last Trip"
            else -> "Driving Report"
        }

        return buildString {
            append("$periodText\n\n")
            append("You are a friendly driving safety coach speaking mainly to Nigerian drivers, while remaining understandable in Cameroon and Ghana. Using the statistics below, craft a complete 150-180 word report that never ends abruptly:\n")
            append("• Use culturally familiar terms and relatable examples from West Africa.\n")
            append("• Praise good habits and offer actionable, persuasive tips for improvement.\n")
            append("• Apply elements from the Theory of Planned Behavior—attitudes, subjective norms and perceived behavioural control—and Cialdini's principles like Social Proof and Loss Aversion.\n")
            append("• Reference the exact numbers, dates and road location of the most frequent unsafe behaviour.\n")
            append("• Keep the tone supportive and sign off as 'Your Driving Safety Specialist Agent'.\n")
            append("• Ensure the response forms a complete narrative, not a list of bullets or an unfinished sentence.\n\n")
            append("Report Statistics:\n")
            append("Total Unsafe Behaviors: ${reportStatistics.totalIncidences}\n")
            if (reportStatistics.mostFrequentUnsafeBehaviour != null &&
                reportStatistics.mostFrequentBehaviourOccurrences.isNotEmpty()
            ) {
                append("Most Frequent Unsafe Behaviour: ${reportStatistics.mostFrequentUnsafeBehaviour} (${reportStatistics.mostFrequentBehaviourCount} times)\n")
                val occurrence = reportStatistics.mostFrequentBehaviourOccurrences.first()
                val dateStr = occurrence.date.format(dateFormatter)
                val timeStr = occurrence.time.format(timeFormatter)
                append("Example Occurrence: $dateStr at $timeStr, Road: ${occurrence.roadName}\n")
            }
            when (periodType) {
                PeriodType.LAST_TRIP -> {
                    append("Trip Duration: ${reportStatistics.lastTripDuration} minutes, ")
                    append("Distance: ${"%.2f".format(reportStatistics.lastTripDistance)} km, ")
                    append("Avg Speed: ${"%.2f".format(reportStatistics.lastTripAverageSpeed)} km/h\n")
                    append("Start: ${reportStatistics.lastTripStartLocation}, End: ${reportStatistics.lastTripEndLocation}\n")
                    append("Alcohol Influence: ${reportStatistics.lastTripInfluence}\n")
                }
                else -> {
                    append("Trips: ${reportStatistics.numberOfTrips}, ")
                    append("Trips with Incidences: ${reportStatistics.numberOfTripsWithIncidences}, ")
                    append("Incidences/Trip: ${reportStatistics.incidencesPerTrip}, ")
                    append("Trips with Alcohol Influence: ${reportStatistics.numberOfTripsWithAlcoholInfluence}\n")
                    reportStatistics.tripWithMostIncidences?.let { trip ->
                        val tripStartTime = Instant.ofEpochMilli(trip.startTime)
                            .atZone(ZoneId.systemDefault()).toLocalDateTime()
                        append("Trip with Most Incidences Start: ${tripStartTime.format(dateFormatter)}\n")
                    }
                    reportStatistics.aggregationUnitWithMostIncidences?.let { agg ->
                        append("Period with Most Incidences: $agg\n")
                    }
                }
            }
        }
    }



    /**
     * Prepares chart data by aggregating unsafe behavior counts per hour.
     */
    fun prepareChartData(summaryDataByDateHour: Map<DateHourKey, HourlySummary>): List<UnsafeBehaviorChartEntry> {
        val behaviorCountsPerHour = mutableMapOf<Int, Int>()
        summaryDataByDateHour.values.forEach { summary ->
            behaviorCountsPerHour[summary.hour] = behaviorCountsPerHour.getOrDefault(summary.hour, 0) + summary.totalBehaviors
        }
        return behaviorCountsPerHour.map { (hour, count) ->
            UnsafeBehaviorChartEntry(hour, count)
        }.sortedBy { it.hour }
    }

    /**
     * Generates the prompt for unsafe behaviours and updates the database to mark them as processed.
     */
    fun generatePromptForBehaviours(
        context: Context,
        unsafeBehaviours: List<UnsafeBehaviourModel>,
        periodType: PeriodType,
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val prompt = generatePrompt(context, unsafeBehaviours, periodType, startDate, endDate)
                _generatedPrompt.value = prompt

                val unsafeBehavioursCopyList = unsafeBehaviours.map { it.copy(processed = true).toEntity() }
                // Update unsafe behaviours in the database
                unsafeBehaviourRepository.batchUpdateUnsafeBehaviours(unsafeBehavioursCopyList)

                // Log the generated prompt in chunks for readability
                prompt.chunked(100).forEach { chunk ->
                    Log.d("LocationRoadViewModel", "Generated Prompt in ViewModel: $chunk")
                }
                Log.d("LocationRoadViewModel", "Generated Prompt in ViewModel: $prompt")
            } catch (e: Exception) {
                Log.e("LocationRoadViewModel", "Error generating prompt", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Retrieves location data and converts it to the domain model if available.
     */
    private suspend fun getLocationData(locationId: UUID): LocationData? {
        return locationRepository.getLocationById(locationId)?.toDomainModel()
    }

    /**
     * Formats an hour value into a 12-hour clock string.
     */
    private fun formatHour(hour: Int): String {
        val localTime = LocalTime.of(hour % 24, 0)
        val formatter = DateTimeFormatter.ofPattern("h a")
        return localTime.format(formatter)
    }
}