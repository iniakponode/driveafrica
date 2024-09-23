package com.uoa.nlgengine.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uoa.nlgengine.data.model.BehaviourSummary
import com.uoa.core.database.repository.LocationRepository
import com.uoa.core.model.LocationData
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.core.network.apiservices.OSMApiService
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

    private suspend fun generatePrompt(unsafeBehaviours: List<UnsafeBehaviourModel>, periodType: PeriodType,): String {
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
                val locations = locationRepository.getLocationsByIds(uniqueLocationIds)
                Log.d(
                    "LocationRoadViewModel",
                    "Retrieved ${locations.size} locations from repository"
                )

                // Map of locationId to LocationData
                val locationMap = locations.associateBy { it.id }
                Log.d("LocationRoadViewModel", "Created location map")

                // Step 3: Extract unique coordinates
                val uniqueCoordinates =
                    locations.map { Pair(it.latitude.toDouble(), it.longitude.toDouble()) }
                        .distinct()
                Log.d("LocationRoadViewModel", "Unique coordinates: $uniqueCoordinates")

                // Step 4: Perform reverse geocoding on unique coordinates
                val roadNameCache = mutableMapOf<Pair<Double, Double>, String?>()
                val semaphore = Semaphore(1) // Limit to 1 to comply with OSM rate limits

                val reverseGeocodingJobs = uniqueCoordinates.map { coordinate ->
                    async {
                        semaphore.withPermit {
                            Log.d(
                                "LocationRoadViewModel",
                                "Reverse geocoding for coordinate: $coordinate"
                            )
                            val roadName = getRoadNameFromOSM(coordinate.first, coordinate.second)
                            roadNameCache[coordinate] = roadName
                            Log.d(
                                "LocationRoadViewModel",
                                "Retrieved road name: $roadName for coordinate: $coordinate"
                            )
                        }
                    }
                }
                reverseGeocodingJobs.awaitAll()
                Log.d("LocationRoadViewModel", "Completed reverse geocoding")

                // Step 5: Map locationId to road name
                val locationIdToRoadName = mutableMapOf<UUID, String?>()
                locations.forEach { location ->
                    val coordinate =
                        Pair(location.latitude.toDouble(), location.longitude.toDouble())
                    val roadName = roadNameCache[coordinate]
                    locationIdToRoadName[location.id] = roadName
                    Log.d(
                        "LocationRoadViewModel",
                        "Mapped locationId ${location.id} to road name: $roadName"
                    )
                }
                Log.d("LocationRoadViewModel", "Created locationId to road name map")


                // Step 6: Aggregate unsafe behaviors by location, date, and hour
                val aggregatedData = unsafeBehaviours.mapNotNull { behavior ->
                    val locationName = behavior.locationId?.let { locationIdToRoadName[it] } ?: "Road Name unknown"
                    val instant = Instant.ofEpochMilli(behavior.timestamp).atZone(ZoneId.systemDefault())
                    val date = instant.toLocalDate()
                    val hour = instant.hour
                    val key = LocationDateHourKey(locationName, date, hour)
                    Pair(key, behavior)
                }.groupBy { it.first }

                // Create BehaviourSummary instances
                val summaryData = aggregatedData.mapValues { entry ->
                    val behaviors = entry.value.map { it.second }
                    val behaviorCounts = behaviors.groupingBy { it.behaviorType }.eachCount()
                    val mostFrequentBehavior = behaviorCounts.maxByOrNull { it.value }?.key ?: "N/A"
                    val totalBehaviors = behaviors.size
                    val alcoholInfluenceCount = behaviors.count { it.alcoholInfluence }

                    BehaviourSummary(
                        date = entry.key.date,
                        location = entry.key.locationName,
                        hour = entry.key.hour,
                        totalBehaviors = totalBehaviors,
                        behaviorCounts = behaviorCounts,
                        mostFrequentBehavior = mostFrequentBehavior,
                        alcoholInfluenceCount = alcoholInfluenceCount
                    )
                }

                // Step 6b: Aggregate unsafe behaviors by date and hour
                val aggregatedDataByDateHour = unsafeBehaviours.map { behavior ->
                    val instant = Instant.ofEpochMilli(behavior.timestamp).atZone(ZoneId.systemDefault())
                    val date = instant.toLocalDate()
                    val hour = instant.hour
                    val key = DateHourKey(date, hour)
                    Pair(key, behavior)
                }.groupBy { it.first }

                // Create HourlySummary instances
                val summaryDataByDateHour = aggregatedDataByDateHour.mapValues { entry ->
                    val behaviors = entry.value.map { it.second }
                    val behaviorCounts = behaviors.groupingBy { it.behaviorType }.eachCount()
                    val mostFrequentBehavior = behaviorCounts.maxByOrNull { it.value }?.key ?: "N/A"
                    val totalBehaviors = behaviors.size
                    val alcoholInfluenceCount = behaviors.count { it.alcoholInfluence }

                    HourlySummary(
                        date = entry.key.date,
                        hour = entry.key.hour,
                        totalBehaviors = totalBehaviors,
                        behaviorCounts = behaviorCounts,
                        mostFrequentBehavior = mostFrequentBehavior,
                        alcoholInfluenceCount = alcoholInfluenceCount
                    )
                }

                _summaryDataByDateHour.value = summaryDataByDateHour

                // Step 7: Generate the prompt
                val promptBuilder = StringBuilder()
                // Include the period the report covers
                val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val startDate = unsafeBehaviours.minOfOrNull { it.timestamp }?.let { timestamp ->
                    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        .format(dateFormatter)
                }

                val endDate = unsafeBehaviours.maxOfOrNull { it.timestamp }?.let { timestamp ->
                    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
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

                promptBuilder.append("$periodText\n\n")
                promptBuilder.append("You are a driving safety specialist focusing on promoting positive driving behaviors in Nigeria. " +
                        "Based on the following data, generate a friendly and encouraging driving behavior report for the driver on his selected report filter type $periodType." +
                        " The report should:\n" +
                        "Acknowledge the driver's efforts and any positive aspects.\n" +
                        "Gently highlight areas where the driver can improve, without direct criticism.\n" +
                        "Offer practical and actionable tips to enhance driving safety.\n" +
                        "Use an uplifting and motivational tone to inspire positive change.\n" +
                        "Always reference the specific dates and incident(s) data (numbers) as well as the locations provided." +
                        "The report should be specific to the data provided." +
                        "Every new sentence should begin on a new line for easy reading and understanding. " +
                        "Report should not be more than 100 words.\n\n")

                // Include per-date-hour summaries
                promptBuilder.append("Summary of Unsafe Behaviours Per Date and Hour:\n")
                summaryDataByDateHour.values.forEach { summary ->
                    val formattedDate = summary.date.format(dateFormatter)
                    val formattedHour = formatHour(summary.hour)
                    promptBuilder.append("Date: $formattedDate, Hour: $formattedHour\n")
                    promptBuilder.append("Total Behaviours: ${summary.totalBehaviors}\n")
                    promptBuilder.append("Alcohol Influence Count: ${summary.alcoholInfluenceCount}\n")
                    promptBuilder.append("Behavior Counts: ${summary.behaviorCounts.entries.joinToString { "${it.key}: ${it.value}" }}\n")
                    promptBuilder.append("Most Frequent Behaviour: ${summary.mostFrequentBehavior}\n\n")
                }

                // Include per-location-date-hour summaries
                promptBuilder.append("Detailed Behaviors by Location, Date, and Hour:\n")
                summaryData.values.forEach { summary ->
                    val formattedDate = summary.date.format(dateFormatter)
                    val formattedHour = formatHour(summary.hour)
                    promptBuilder.append("Location: ${summary.location}, Date: $formattedDate, Hour: $formattedHour\n")
                    promptBuilder.append("Total Behaviours: ${summary.totalBehaviors}\n")
                    promptBuilder.append("Alcohol Influence Count: ${summary.alcoholInfluenceCount}\n")
                    promptBuilder.append("Behaviour Counts: ${summary.behaviorCounts.entries.joinToString { "${it.key}: ${it.value}" }}\n")
                    promptBuilder.append("Most Frequent Behaviour: ${summary.mostFrequentBehavior}\n\n")
                }

                val prompt = promptBuilder.toString()
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


    fun generatePromptForBehaviours(unsafeBehaviours: List<UnsafeBehaviourModel>, periodType: PeriodType) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val prompt = generatePrompt(unsafeBehaviours, periodType)
                _generatedPrompt.value = prompt

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



    private suspend fun getRoadNameFromOSM(latitude: Double, longitude: Double): String? {
        val response = osmApiService.getReverseGeocoding(
            format = "json",
            lat = latitude.toLong(),
            lon = longitude.toLong(),
            zoom = 18,
            addressdetails = 1
        )
        return response.address.getAddressLine(0)
    }
}
