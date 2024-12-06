package com.uoa.nlgengine.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.nlgengine.data.model.BehaviourSummary
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.database.repository.TripDataRepository
import com.uoa.core.model.LocationData
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.core.network.apiservices.OSMApiService
import com.uoa.core.utils.toDomainModel
import com.uoa.nlgengine.data.model.DateHourKey
import com.uoa.nlgengine.data.model.HourlySummary
import com.uoa.nlgengine.data.model.LocationDateHourKey
import com.uoa.nlgengine.data.model.ReportStatistics
import com.uoa.nlgengine.data.model.UnsafeBehaviorChartEntry
import com.uoa.nlgengine.domain.usecases.local.GetLastInsertedUnsafeBehaviourUseCase
import com.uoa.nlgengine.util.PeriodType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import retrofit2.HttpException
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import kotlin.math.log
import com.uoa.nlgengine.util.computeReportStatistics
import kotlin.time.toJavaDuration


@HiltViewModel
class NLGEngineViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val tripRepository: TripDataRepository,
    private val osmApiService: OSMApiService,
    private val lastInsertedUnsafeBehaviourUseCase: GetLastInsertedUnsafeBehaviourUseCase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _generatedPrompt = MutableStateFlow<String>("")
    val generatedPrompt: StateFlow<String> get() = _generatedPrompt

    private val _summaryDataByDateHour = MutableStateFlow<Map<DateHourKey, HourlySummary>>(emptyMap())
    val summaryDataByDateHour: StateFlow<Map<DateHourKey, HourlySummary>> get() = _summaryDataByDateHour

    private suspend fun generatePrompt(context: Context,unsafeBehaviours: List<UnsafeBehaviourModel>, periodType: PeriodType): String {
        _isLoading.value = true

        return withContext(Dispatchers.IO) {
            try {
                val reportStatistics = computeReportStatistics(
                    context,
                    osmApiService,
                    periodType,
                    unsafeBehaviours,
                    tripRepository,
                    locationRepository,
                    lastInsertedUnsafeBehaviourUseCase
                )

                // Generate prompt with statistics
                val prompt = buildPrompt(context, unsafeBehaviours,periodType,reportStatistics!!)
                prompt

            } catch (e: Exception) {
                Log.e("NLGEngineViewModel", "Error generating prompt", e)
                ""
            }
            finally {
                _isLoading.value=false
            }
        }
    }


    private suspend fun buildPrompt(
        context: Context,
        unsafeBehaviours: List<UnsafeBehaviourModel>,
        periodType: PeriodType,
        reportStatistics: ReportStatistics
    ): String {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        // Include the period the report covers
        val startDate = unsafeBehaviours.minOfOrNull { it.timestamp }?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                .format(dateFormatter)
        }
        val endDate = unsafeBehaviours.maxOfOrNull { it.timestamp }?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                .format(dateFormatter)
        }

        val periodText = when (periodType) {
            PeriodType.TODAY -> "Report for Today"
            PeriodType.THIS_WEEK -> "Report for This Week"
            PeriodType.LAST_WEEK -> "Report for Last Week"
            PeriodType.CUSTOM_PERIOD -> {
                if (startDate != null && endDate != null && startDate == endDate) {
                    "Report for $startDate"
                } else if (startDate != null && endDate != null) {
                    "Report Period: $startDate to $endDate"
                } else {
                    "Report for Selected Period"
                }
            }
            PeriodType.LAST_TRIP -> "Report for the Last Trip"
            else -> "Report"
        }

        val promptBuilder = StringBuilder()
            .append("$periodText\n\n")
            .append("You are a driving safety specialist in Nigeria. Based on the following data, generate a friendly and encouraging driving behavior report for the driver on their selected report filter ($periodType). The report should:\n")
            .append("- Must have only between 150-200 words.\n")
            .append("- Acknowledge the driver's efforts and positive aspects.\n")
            .append("- Gently highlight areas for improvement without direct criticism.\n")
            .append("- Offer practical, actionable tips to enhance safety.\n")
            .append("- Use an uplifting and motivational tone.\n")
            .append("- The sender's name in the complimentary closing section should be 'Your Driving Safety Specialist Agent'.\n")
            .append("- Must include the numbers given in this prompt in the response without hallucination.\n")
            .append("- You will need to decode the Base64 encoded JSON data to get the driving offences, penalties, fines, and laws.\n")
            .append("- Reference the specific dates, incidents, and locations provided.\n")
            .append("- Ensure to use only the given data for fines, laws, and sections without hallucination.\n\n")
            .append("After generating the report, please ensure the response includes the key components of the Theory of Planned Behavior (TPB), specifically:\n")
            .append("- **Attitudes**: Reflect on the driver's positive and negative evaluations of performing the behavior.\n")
            .append("- **Subjective Norms**: Reference the social pressure or norms influencing the driver's behavior.\n")
            .append("- **Perceived Behavioral Control**: Address the driver's perception of their ability to perform the behavior.\n")
            .append("Also, ensure that the report:\n")
            .append("- Maintains a supportive tone throughout.\n")
            .append("- Specifies the unsafe behaviors observed.\n")
            .append("- Include the corresponding location (Road name) as given from the data and time for the most frequent incident.\n")
            .append("- State the risks inherent in the identified unsafe behaviours.\n")
            .append("- State the benefits inherent especially in economic (fuel consumption, vehicle health, etc) and life related terms if the drivers could improve and change from the identified unsafe behaviours.\n")
            .append("- Has a statement about the alcohol influence based on the given data.\n")
            .append("- Strictly adheres to the data provided in this prompt without adding any information not directly traceable to the prompt.\n\n")
            .append("- If any of the above components are missing, please identify and include them before finalizing the response.\n\n")
            .append("Report Statistics are given below:\n")

        if (true) {
            // Common statistics
            promptBuilder.append("Total Unsafe Behaviors: ${reportStatistics.totalIncidences}\n")
            if (reportStatistics.mostFrequentUnsafeBehaviour != null) {
                promptBuilder.append("Most Frequent Unsafe Behaviour: ${reportStatistics.mostFrequentUnsafeBehaviour}\n")
                promptBuilder.append("Occurrences: ${reportStatistics.mostFrequentBehaviourCount}\n")

                // Include occurrences
                promptBuilder.append("Occurrences Details:\n")
                reportStatistics.mostFrequentBehaviourOccurrences.forEach { occurrence ->
                    val dateStr = occurrence.date.format(dateFormatter)
                    val timeStr = occurrence.time.format(timeFormatter)
                    promptBuilder.append("- Date: $dateStr, Time: $timeStr, Road: ${occurrence.roadName}\n")
                }
            }


            // Period-specific statistics
            when (periodType) {
                PeriodType.LAST_TRIP -> {
                    // Include last trip specific statistics
                    promptBuilder.append("Trip Duration: ${reportStatistics.lastTripDuration?.toJavaDuration()} minutes\n")
                    promptBuilder.append("Distance Covered: ${"%.2f".format(reportStatistics.lastTripDistance)} km\n")
                    promptBuilder.append("Average Speed: ${"%.2f".format(reportStatistics.lastTripAverageSpeed)} km/h\n")
                    promptBuilder.append("Start Location: ${reportStatistics.lastTripStartLocation}\n")
                    promptBuilder.append("End Location: ${reportStatistics.lastTripEndLocation}\n")
                    promptBuilder.append("Most Frequent Unsafe Behaviour: ${reportStatistics.mostFrequentUnsafeBehaviour}\n")
                    promptBuilder.append("Most Frequent Unsafe Behaviour Count: ${reportStatistics.mostFrequentBehaviourCount}\n")
                    promptBuilder.append("Most Frequent Unsafe Behaviour Occurrences: ${reportStatistics.mostFrequentBehaviourOccurrences}\n")
                    promptBuilder.append("Alcohol Influence: ${reportStatistics.lastTripInfluence}\n")
                }

                else -> {
                    // Include statistics for other periods
                    promptBuilder.append("Number of Trips: ${reportStatistics.numberOfTrips}\n")
                    promptBuilder.append("Number of Trips with Incidences: ${reportStatistics.numberOfTripsWithIncidences}\n")
                    promptBuilder.append("Incidences Per trip: ${reportStatistics.incidencesPerTrip}\n")
                    promptBuilder.append("Number of Trips with Alcohol Influence: ${reportStatistics.numberOfTripsWithAlcoholInfluence}\n")

                    // Include most frequent Unsafe Behaviour
                    if (reportStatistics.mostFrequentUnsafeBehaviour != null) {
                        promptBuilder.append("Most Frequent Unsafe Behviour: ${reportStatistics.mostFrequentUnsafeBehaviour}\n")
                    }

                    // Include most frequent Unsafe Behaviour count
                    if (true) {
                        promptBuilder.append("Count of Most Frequent Unsafe Behviour: ${reportStatistics.mostFrequentBehaviourCount}\n")
                    }

                    // Include most frequent Unsafe Behaviour occurrence instance
                    if (true) {
                        promptBuilder.append("Occurrence instances (dates, times and road names) of Most Frequent Unsafe Behaviour; get all occurrences out of the variable using a loop: ${reportStatistics.mostFrequentBehaviourOccurrences}\n")
                    }

                    // Include trip with most incidences
                    if (reportStatistics.tripWithMostIncidences != null) {
                        val tripStartTime =
                            Instant.ofEpochMilli(reportStatistics.tripWithMostIncidences.startTime)
                                .atZone(ZoneId.systemDefault()).toLocalDateTime()
                        promptBuilder.append(
                            "Trip with Most Incidences Start Time: ${
                                tripStartTime.format(
                                    dateFormatter
                                )
                            }\n"
                        )
                    }

                    // Include aggregation unit with most incidences
                    if (reportStatistics.aggregationUnitWithMostIncidences != null) {
                        promptBuilder.append("Period with Most Incidences: ${reportStatistics.aggregationUnitWithMostIncidences}\n")
                    }
                }
            }
        }else {
            promptBuilder.append("No unsafe behaviors recorded during this period.\n")
        }

        return promptBuilder.toString()
    }

    fun prepareChartData(summaryDataByDateHour: Map<DateHourKey, HourlySummary>): List<UnsafeBehaviorChartEntry> {
        // Aggregate behavior counts per hour
        val behaviorCountsPerHour = mutableMapOf<Int, Int>()

        summaryDataByDateHour.values.forEach { summary ->
            behaviorCountsPerHour[summary.hour] = behaviorCountsPerHour.getOrDefault(summary.hour, 0) + summary.totalBehaviors
        }

        // Convert to a list of entries
        return behaviorCountsPerHour.map { (hour, count) ->
            UnsafeBehaviorChartEntry(hour, count)
        }.sortedBy { it.hour }
    }


    fun generatePromptForBehaviours(context: Context,
                                    unsafeBehaviours: List<UnsafeBehaviourModel>,
                                    periodType: PeriodType) {

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val prompt = generatePrompt(context, unsafeBehaviours, periodType)
                _generatedPrompt.value = prompt
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



    private suspend fun getLocationData(locationId: UUID): LocationData? {
        // Implement logic to retrieve location data from the repository
        return locationRepository.getLocationById(locationId)!!.toDomainModel()
    }

    private fun formatHour(hour: Int): String {
        val localTime = LocalTime.of(hour % 24, 0)
        val formatter = DateTimeFormatter.ofPattern("h a") // 'h' for 12-hour clock, 'a' for AM/PM
        return localTime.format(formatter)
    }


}
