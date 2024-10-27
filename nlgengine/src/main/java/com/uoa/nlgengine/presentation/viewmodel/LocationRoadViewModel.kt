package com.uoa.nlgengine.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.nlgengine.data.model.BehaviourSummary
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.model.LocationData
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.core.network.apiservices.OSMApiService
import com.uoa.core.nlg.compressAndEncodeJson
import com.uoa.core.utils.toDomainModel
import com.uoa.nlgengine.data.model.DateHourKey
import com.uoa.nlgengine.data.model.HourlySummary
import com.uoa.nlgengine.data.model.LocationDateHourKey
import com.uoa.nlgengine.data.model.UnsafeBehaviorChartEntry
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
import retrofit2.HttpException
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject


@HiltViewModel
class LocationRoadViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val osmApiService: OSMApiService
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _generatedPrompt = MutableStateFlow<String>("")
    val generatedPrompt: StateFlow<String> get() = _generatedPrompt

    private val _summaryDataByDateHour = MutableStateFlow<Map<DateHourKey, HourlySummary>>(emptyMap())
    val summaryDataByDateHour: StateFlow<Map<DateHourKey, HourlySummary>> get() = _summaryDataByDateHour

    private suspend fun generatePrompt(context: Context,unsafeBehaviours: List<UnsafeBehaviourModel>, periodType: PeriodType,): String {
        _isLoading.value = true
        return withContext(Dispatchers.IO) {
            try {
                Log.d(
                    "LocationRoadViewModel",
                    "Starting generatePrompt with ${unsafeBehaviours.size} unsafe behaviours"
                )

                // Step 1: Extract unique location IDs
                val uniqueLocationIds = unsafeBehaviours.mapNotNull { it.locationId }.distinct()
                Log.d("LocationRoadViewModel", "Unique location IDs: $uniqueLocationIds")

                // Step 2: Get locations from repository
                val locationMap = locationRepository.getLocationsByIds(uniqueLocationIds)
                    .associateBy { it.id }
                Log.d("LocationRoadViewModel", "Created location map")

                // Step 3: Extract unique coordinates and reverse geocode with concurrency
                val roadNameCache = mutableMapOf<Pair<Double, Double>, String?>()
                val semaphore = Semaphore(1) // To comply with OSM rate limits
                val uniqueCoordinates =
                    locationMap.values.map { Pair(it.latitude.toDouble(), it.longitude.toDouble()) }

                uniqueCoordinates.map { coordinate ->
                    async {
                        semaphore.withPermit {
                            val roadName = getRoadNameFromOSM(coordinate.first, coordinate.second)
                            roadNameCache[coordinate] = roadName
                            Log.d(
                                "LocationRoadViewModel",
                                "Reverse geocoded: $coordinate -> $roadName"
                            )
                        }
                    }
                }.awaitAll()

                // Step 4: Map locationId to road name
                val locationIdToRoadName = locationMap.mapValues { (_, location) ->
                    val coordinate =
                        Pair(location.latitude.toDouble(), location.longitude.toDouble())
                    roadNameCache[coordinate]
                }
                Log.d("LocationRoadViewModel", "Mapped locationId to road names")

                // Step 5: Aggregate unsafe behaviors by location, date, and hour
                val aggregatedData = unsafeBehaviours.groupBy { behavior ->
                    val locationName =
                        behavior.locationId?.let { locationIdToRoadName[it] } ?: "Road Name unknown"
                    val instant =
                        Instant.ofEpochMilli(behavior.timestamp).atZone(ZoneId.systemDefault())
                    val date = instant.toLocalDate()
                    val hour = instant.hour
                    LocationDateHourKey(locationName, date, hour)
                }

                // Create BehaviorSummary instances
                val summaryData = aggregatedData.mapValues { entry ->
                    val behaviors = entry.value
                    val behaviorCounts = behaviors.groupingBy { it.behaviorType }.eachCount()
                    val mostFrequentBehavior = behaviorCounts.maxByOrNull { it.value }?.key ?: "N/A"
                    val alcoholInfluenceCount = behaviors.count { it.alcoholInfluence }

                    BehaviourSummary(
                        date = entry.key.date,
                        location = entry.key.locationName,
                        hour = entry.key.hour,
                        totalBehaviors = behaviors.size,
                        behaviorCounts = behaviorCounts,
                        mostFrequentBehavior = mostFrequentBehavior,
                        alcoholInfluenceCount = alcoholInfluenceCount
                    )
                }

                // Step 6: Generate prompt with context-specific data
                val prompt = buildPrompt(context,unsafeBehaviours, summaryData, periodType)
                Log.d("LocationRoadViewModel", "Generated prompt: $prompt")

                prompt
            } catch (e: Exception) {
                Log.e("LocationRoadViewModel", "Error generating prompt", e)
                ""
            } finally {
                _isLoading.value = false
                Log.d("LocationRoadViewModel", "generatePrompt finished")
            }
        }
    }

//        private suspend fun buildPrompt(
//            context: Context,
//            unsafeBehaviours: List<UnsafeBehaviourModel>,
//            summaryData: Map<LocationDateHourKey, BehaviourSummary>,
//            periodType: PeriodType
//        ): String {
//            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
//
//            // Include the period the report covers
//            val startDate = unsafeBehaviours.minOfOrNull { it.timestamp }?.let {
//                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
//                    .format(dateFormatter)
//            }
//            val endDate = unsafeBehaviours.maxOfOrNull { it.timestamp }?.let {
//                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
//                    .format(dateFormatter)
//            }
//
//            val periodText = when (periodType) {
//                PeriodType.TODAY -> "Report for Today"
//                PeriodType.THIS_WEEK -> "Report for This Week"
//                PeriodType.LAST_WEEK -> "Report for Last Week"
//                PeriodType.CUSTOM_PERIOD -> {
//                    if (startDate != null && endDate != null && startDate == endDate) {
//                        "Report for $startDate"
//                    } else if (startDate != null && endDate != null) {
//                        "Report Period: $startDate to $endDate"
//                    } else {
//                        "Report for Selected Period"
//                    }
//                }
//
//                PeriodType.LAST_TRIP -> "Report for the Last Trip"
//                else -> "Report"
//            }
//
//            val promptBuilder = StringBuilder()
//                .append("$periodText\n\n")
//                .append("You are a driving safety specialist in Nigeria. Based on the following data, generate a friendly and encouraging driving behavior report for the driver on their selected report filter ($periodType). The report should:\n")
//                .append("- Must have only between 150-300 words.\n\n")
//                .append("- Acknowledge the driver's efforts and positive aspects.\n")
//                .append("- Gently highlight areas for improvement without direct criticism.\n")
//                .append("- Offer practical, actionable tips to enhance safety.\n")
//                .append("- Use an uplifting and motivational tone.\n")
//                .append("- The senders name in the a complimentary closing section to be 'Your Driving Safety Specialist Agent'.\n\n")
//                .append("- Must include the numbers given in this prompt in the response without hallucination.\n\n")
//                .append("- You will need to decode the Base64 encoded JSON data to get the driving offences, penalties, fines, and laws.\n")
//                .append("- Reference the specific dates, incidents, and locations provided.\n")
//                .append("- Ensure to use only the given data for fines, laws, and sections without hallucination.\n")
//                .append("- Summary of Unsafe Behaviors Per Date and Hour:\n")
//
//            summaryData.forEach { (key, summary) ->
//                val formattedDate = key.date.format(dateFormatter)
//                val formattedHour = formatHour(key.hour)
//                promptBuilder
//                    .append("Location: ${summary.location}, Date: $formattedDate, Hour: $formattedHour\n")
//                    .append("Total Behaviors: ${summary.totalBehaviors}\n")
//                    .append("Alcohol Influence Count: ${summary.alcoholInfluenceCount}\n")
//                    .append("Behavior Counts: ${summary.behaviorCounts.entries.joinToString { "${it.key}: ${it.value}" }}\n")
//                    .append("Most Frequent Behavior: ${summary.mostFrequentBehavior}\n\n")
//                    .append("Base64Encoded Json Driving Offences, Penalties, Fines, and Laws: ${compressAndEncodeJson(context)}\n")
//            }
//
//            return promptBuilder.toString()
//        }


    private suspend fun buildPrompt(
        context: Context,
        unsafeBehaviours: List<UnsafeBehaviourModel>,
        summaryData: Map<LocationDateHourKey, BehaviourSummary>,
        periodType: PeriodType
    ): String {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

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
            .append("Summary of Unsafe Behaviors Per Date and Hour:\n")

        summaryData.forEach { (key, summary) ->
            val formattedDate = key.date.format(dateFormatter)
            val formattedHour = formatHour(key.hour)
            promptBuilder
                .append("Location: ${summary.location}, Date: $formattedDate, Hour: $formattedHour\n")
                .append("Total Behaviors: ${summary.totalBehaviors}\n")
                .append("Alcohol Influence Count: ${summary.alcoholInfluenceCount}\n")
                .append("Behavior Counts: ${summary.behaviorCounts.entries.joinToString { "${it.key}: ${it.value}" }}\n")
                .append("Most Frequent Behavior: ${summary.mostFrequentBehavior}\n\n")
//                .append("Base64Encoded Json Driving Offences, Penalties, Fines, and Laws: ${compressAndEncodeJson(context)}\n")
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


    fun generatePromptForBehaviours(context: Context, unsafeBehaviours: List<UnsafeBehaviourModel>, periodType: PeriodType) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val prompt = generatePrompt(context, unsafeBehaviours, periodType)
                _generatedPrompt.value = prompt
                prompt.chunked(100).forEach { chunk ->
                    Log.d("LocationRoadViewModel", "Generated Prompt in ViewModel: $chunk")
                }
//                Log.d("LocationRoadViewModel", "Generated Prompt in ViewModel: $prompt")

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



    private suspend fun getRoadNameFromOSM(latitude: Double, longitude: Double): String? {
        return try {
            val response = osmApiService.getReverseGeocoding(
                format = "json",
                lat = latitude.toLong(),
                lon = longitude.toLong(),
                zoom = 18,
                addressdetails = 1
            )
            response.address.getAddressLine(0)
        } catch (e: HttpException) {
            Log.e("LocationRoadViewModel", "HTTP error: ${e.code()} - ${e.message()}")
            null
        } catch (e: Exception) {
            Log.e("LocationRoadViewModel", "Error fetching road name from OSM", e)
            null
        }
    }
}
