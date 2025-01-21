package com.uoa.core.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.uoa.core.apiServices.models.alcoholquestionnaireModels.AlcoholQuestionnaireResponse
import java.text.SimpleDateFormat
import java.time.LocalDate
//import kotlinx.datetime.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

import com.uoa.core.model.* // Replace with your actual package for domain models
import com.uoa.core.apiServices.models.tripModels.*
import com.uoa.core.apiServices.models.drivingTipModels.*
import com.uoa.core.apiServices.models.rawSensorModels.*
import com.uoa.core.apiServices.models.nlgReportModels.*
import com.uoa.core.apiServices.models.reportStatisticsModels.ReportStatisticsCreate
import com.uoa.core.apiServices.models.unsafeBehaviourModels.*
import com.uoa.core.database.entities.ReportStatisticsEntity
import okhttp3.internal.format
import java.time.Instant
import java.time.ZoneId


import java.util.Locale
import java.util.TimeZone
import kotlin.time.toKotlinDuration

// Common formatter for Date -> ISO 8601 (Z) format
private val dateFormatUTC = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

// ---------------- Trip Conversions ----------------
fun Trip.toTripCreate(): TripCreate {
    val isoStartDate = dateFormatUTC.format(startDate)
    val isoEndDate = endDate?.let { dateFormatUTC.format(it) }
    return TripCreate(
        id= id,
        driverProfileId = driverPId,
        start_date = isoStartDate,
        end_date = isoEndDate!!,
        start_time = startTime,
        end_time = endTime,
        synced = true
    )
}

fun TripCreate.toTrip(): Trip {
    val parsedStartDate = dateFormatUTC.parse(start_date)
    val parsedEndDate = end_date.let { dateFormatUTC.parse(it) }
    return Trip(
        driverPId = driverProfileId,
        startTime = start_time!!,
        endTime = end_time,
        startDate = parsedStartDate,
        endDate = parsedEndDate,
        id = UUID.randomUUID(), // Adjust as needed if you have a way to set the ID
        influence = ""
    )
}

fun ReportStatisticsEntity.toReportStatisticsCreate(): ReportStatisticsCreate {
    val gson = Gson()
    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val zoneId = ZoneId.systemDefault()

    fun Date.toLocalDate(): LocalDate {
        return Instant.ofEpochMilli(this.time).atZone(zoneId).toLocalDate()
    }

    // Parse JSON fields back to their domain types using Gson and converters
    // List<BehaviourOccurrence>
    val occurrencesType = object : TypeToken<List<BehaviourOccurrence>>() {}.type
    val parsedOccurrences: List<BehaviourOccurrence> = gson.fromJson(mostFrequentBehaviourOccurrences, occurrencesType) ?: emptyList()

    // Trip?
    val parsedTripWithMostIncidences: Trip? = tripWithMostIncidences?.let {
        gson.fromJson(it, Trip::class.java)
    }

    // Map<LocalDate, Int> for tripsPerAggregationUnit
    val mapLocalDateIntType = object : TypeToken<Map<String, Int>>() {}.type
    val parsedTripsPerAggregationUnit: Map<LocalDate, Int> = gson.fromJson<Map<String, Int>>(tripsPerAggregationUnit, mapLocalDateIntType)
        ?.mapKeys { LocalDate.parse(it.key, dateFormatter) }
        ?: emptyMap()

    // aggregationUnitWithMostIncidences is already LocalDate?, no parsing needed

    // Map<LocalDate, Int> for incidencesPerAggregationUnit
    val parsedIncidencesPerAggregationUnit: Map<LocalDate, Int> = gson.fromJson<Map<String, Int>>(incidencesPerAggregationUnit, mapLocalDateIntType)
        ?.mapKeys { LocalDate.parse(it.key, dateFormatter) }
        ?: emptyMap()

    // Map<UUID, Int> for incidencesPerTrip
    val mapUUIDIntType = object : TypeToken<Map<String, Int>>() {}.type
    val parsedIncidencesPerTrip: Map<UUID, Int> = gson.fromJson<Map<String, Int>>(incidencesPerTrip, mapUUIDIntType)
        ?.mapKeys { UUID.fromString(it.key) }
        ?: emptyMap()

    // Map<LocalDate, Int> for tripsWithAlcoholInfluencePerAggregationUnit
    val parsedTripsWithAlcoholInfluencePerAggregationUnit: Map<LocalDate, Int> =
        gson.fromJson<Map<String, Int>>(tripsWithAlcoholInfluencePerAggregationUnit, mapLocalDateIntType)
            ?.mapKeys { LocalDate.parse(it.key, dateFormatter) }
            ?: emptyMap()

    // lastTripDuration is a String? representing ISO-8601 duration
    val parsedLastTripDuration = lastTripDuration?.let { java.time.Duration.parse(it) }

    val localCreatedDate = createdDate
    val localStartDate = startDate ?: LocalDate.now()
    val localEndDate = endDate ?: LocalDate.now()

    return ReportStatisticsCreate(
        id = id,
        driverProfileId = driverProfileId,
        tripId = tripId,
        startDate = localStartDate,
        endDate = localEndDate,
        createdDate = localCreatedDate,
        totalIncidences = totalIncidences,
        mostFrequentUnsafeBehaviour = mostFrequentUnsafeBehaviour,
        mostFrequentBehaviourCount = mostFrequentBehaviourCount,
        mostFrequentBehaviourOccurrences = parsedOccurrences,
        tripWithMostIncidences = parsedTripWithMostIncidences,
        tripsPerAggregationUnit = parsedTripsPerAggregationUnit,
        aggregationUnitWithMostIncidences = aggregationUnitWithMostIncidences,
        incidencesPerAggregationUnit = parsedIncidencesPerAggregationUnit,
        incidencesPerTrip = parsedIncidencesPerTrip,
        aggregationLevel = aggregationLevel,
        aggregationUnitsWithAlcoholInfluence = aggregationUnitsWithAlcoholInfluence,
        tripsWithAlcoholInfluencePerAggregationUnit = parsedTripsWithAlcoholInfluencePerAggregationUnit,
        sync = sync,
        processed = processed,
        numberOfTrips = numberOfTrips,
        numberOfTripsWithIncidences = numberOfTripsWithIncidences,
        numberOfTripsWithAlcoholInfluence = numberOfTripsWithAlcoholInfluence,
        lastTripDuration = parsedLastTripDuration?.toKotlinDuration(),
        lastTripDistance = lastTripDistance,
        lastTripAverageSpeed = lastTripAverageSpeed,
        lastTripStartLocation = lastTripStartLocation,
        lastTripEndLocation = lastTripEndLocation,
        lastTripStartTime = lastTripStartTime,
        lastTripEndTime = lastTripEndTime,
        lastTripInfluence = lastTripInfluence
    )
}



// ---------------- RawSensorData Conversions ----------------
fun RawSensorData.toRawSensorDataCreate(): RawSensorDataCreate {
    val isoDate = dateFormatUTC.format(date!!)
    return RawSensorDataCreate(
        id=id,
        sensor_type = sensorType,
        sensor_type_name = sensorTypeName,
        values = values,
        timestamp = timestamp,
        date = isoDate,
        accuracy = accuracy,
        location_id = locationId,
        trip_id = tripId!!,
        driverProfileId = driverProfileId!!,
        sync = sync
    )
}

//fun RawSensorDataCreate.toRawSensorData(): RawSensorData {
//    val parsedDate = dateFormatUTC.parse(date!!)
//    return RawSensorData(
//        id = id,
//        driverProfileId=driverProfileId,
//        sensorType = sensor_type,
//        sensorTypeName = sensor_type_name,
//        values = values,
//        timestamp = timestamp,
//        date = parsedDate,
//        accuracy = accuracy,
//        locationId = location_id,
//        tripId = trip_id,
//        sync = sync
//    )
//}

// ---------------- UnsafeBehaviourModel Conversions ----------------
fun UnsafeBehaviourModel.toUnsafeBehaviourCreate(): UnsafeBehaviourCreate {
    val date = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val formattedDate = date.format(formatter)

    return UnsafeBehaviourCreate(
        id=id,
        trip_id = tripId,
        driverProfileId = driverProfileId,
        location_id = locationId!!,
        severity = severity.toDouble(),
        timestamp = timestamp,
        date = formattedDate,
        behaviour_type = behaviorType
    )
}

//fun UnsafeBehaviourCreate.toUnsafeBehaviourModel(): UnsafeBehaviourModel {
//    val parsedDate = dateFormatUTC.parse(date)
//    return UnsafeBehaviourModel(
//        id = UUID.fromString(id),
//        tripId = UUID.fromString(trip_id),
//        driverProfileId= UUID.fromString(driver_profile_id),
//        locationId = location_id?.let { UUID.fromString(it) },
//        behaviorType = behaviour_type,
//        severity = severity.toFloat(),
//        timestamp = timestamp,
//        date = parsedDate,
//        updated =
//    )
//}
//
//// ---------------- LocationData Conversions ----------------
//fun LocationData.toLocationCreate(): LocationCreate {
//    val isoDate = date?.let { dateFormatUTC.format(it) }
//    return LocationCreate(
//        id = id.toString(),
//        latitude = latitude,
//        longitude = longitude,
//        altitude = altitude,
//        speed = speed,
//        distance = distance,
//        timestamp = timestamp,
//        date = isoDate,
//        sync = sync
//    )
//}
//
//fun LocationCreate.toLocationData(): LocationData {
//    val parsedDate = date?.let { dateFormatUTC.parse(it) }
//    return LocationData(
//        id = UUID.fromString(id),
//        latitude = latitude,
//        longitude = longitude,
//        altitude = altitude,
//        speed = speed,
//        distance = distance,
//        timestamp = timestamp,
//        date = parsedDate,
//        sync = sync
//    )
//}
//
//// ---------------- DrivingTip Conversions ----------------
fun DrivingTip.toDrivingTipCreate(): DrivingTipCreate {
//    val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

    val formatter = DateTimeFormatter.ISO_DATE_TIME
    val isoDate = date.format(formatter)
    return DrivingTipCreate(
        tip_id= tipId,
        title = title,
        meaning = meaning!!,
        penalty = penalty!!,
        fine = fine!!,
        law = law!!,
        hostility = hostility,
        summary_tip = summaryTip!!,
        date = isoDate,
        sync = sync,
        profile_id = driverProfileId,
        llm = llm!!
    )
}

fun DrivingTipResponse.toDrivingTip(): DrivingTip {
    // If date is a String formatted as ISO_LOCAL_DATE
    val parsedDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
    return DrivingTip(
        tipId =tip_id,
        driverProfileId =profile_id,
        title = title, // Assuming the report_text can serve as the title
        meaning = meaning,      // No direct mapping, set to null
        penalty = penalty,      // No direct mapping, set to null
        fine = fine,         // No direct mapping, set to null
        law = law,          // No direct mapping, set to null
        hostility = "neutral", // Choose a default value or logic
        summaryTip = summary_tip,   // No direct mapping, set to null
        date = parsedDate,     // Using startDate as the driving tip's date
        sync = sync,
        llm = llm           // No direct mapping, set to null
    )
}

fun AlcoholQuestionnaireResponse.toQuestionnaire(): Questionnaire {
    return Questionnaire(
        id = this.id,
        driverProfileId = this.driverProfileId,
        drankAlcohol = this.drankAlcohol,
        selectedAlcoholTypes = this.selectedAlcoholTypes,
        beerQuantity = this.beerQuantity,
        wineQuantity = this.wineQuantity,
        spiritsQuantity = this.spiritsQuantity,
        firstDrinkTime = this.firstDrinkTime,
        lastDrinkTime = this.lastDrinkTime,
        emptyStomach = this.emptyStomach,
        caffeinatedDrink = this.caffeinatedDrink,
        impairmentLevel = this.impairmentLevel,
        date= DateConversionUtils.stringToDate(this.date)!!, // ISO 8601 format
        plansToDrive = this.plansToDrive
    )
}



//
//fun DrivingTipCreate.toDrivingTip(): DrivingTip {
//    val localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
//    return DrivingTip(
//        tipId = UUID.fromString(profile_id),
//        title = title,
//        meaning = meaning,
//        penalty = penalty,
//        fine = fine,
//        law = law,
//        hostility = hostility,
//        summaryTip = summary_tip,
//        date = localDate,
//        sync = sync,
//        profileId = UUID.fromString(profile_id),
//        llm = llm
//    )
//}
//
//// ---------------- AIModelInputs Conversions ----------------
//fun AIModelInputs.toAIModelInputCreate(): AIModelInputCreate {
//    val isoDate = date?.let { dateFormatUTC.format(it) }
//    return AIModelInputCreate(
//        id = id.toString(),
//        trip_id = tripId.toString(),
//        timestamp = timestamp,
//        start_timestamp = startTimestamp,
//        end_timestamp = endTimestamp,
//        date = isoDate,
//        hour_of_day_mean = hourOfDayMean,
//        day_of_week_mean = dayOfWeekMean,
//        speed_std = speedStd,
//        course_std = courseStd,
//        acceleration_y_original_mean = accelerationYOriginalMean,
//        sync = sync
//    )
//}
//
//fun AIModelInputCreate.toAIModelInputs(): AIModelInputs {
//    val parsedDate = date?.let { dateFormatUTC.parse(it) }
//    return AIModelInputs(
//        id = UUID.fromString(id),
//        tripId = UUID.fromString(trip_id),
//        timestamp = timestamp,
//        startTimestamp = start_timestamp,
//        endTimestamp = end_timestamp,
//        date = parsedDate,
//        hourOfDayMean = hour_of_day_mean,
//        dayOfWeekMean = day_of_week_mean,
//        speedStd = speed_std,
//        courseStd = course_std,
//        accelerationYOriginalMean = acceleration_y_original_mean,
//        sync = sync
//    )
//}
//
// Converters
private val isoDateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

fun NLGReport.toNLGReportCreate(): NLGReportCreate {
    return NLGReportCreate(
        id=id,
        driverProfileId = userId,
        startDate = startDate!!,
        endDate = endDate!!,
        report_text = reportText,
        generated_at = createdDate.format(isoDateTimeFormatter),
        synced = synced
    )
}

fun NLGReport.toNLGReportResponse(): NLGReportResponse {
    return NLGReportResponse(
        id = id,
        driverProfileId = userId,
        startDate = startDate!!,
        endDate = endDate!!,
        report_text = reportText,
        generated_at = createdDate.format(isoDateTimeFormatter),
        synced = synced
    )
}

//fun NLGReportCreate.toDomainModel(id: UUID? = null): NLGReport {
//    val generatedLocalDateTime = java.time.LocalDate.parse(generated_at, isoDateTimeFormatter)
//    return NLGReport(
//        userId = UUID.fromString(driver_profile_id),
//        startDate = startDate,
//        endDate = endDate,
//        reportText = report_text,
//        createdDate = generatedLocalDateTime,
//        synced = synced
//    )
//}

fun NLGReportResponse.toDomainModel(): NLGReport {
    val generatedLocalDateTime = LocalDate.parse(generated_at, isoDateTimeFormatter)
    return NLGReport(
        id = id,
        userId = driverProfileId,
        startDate = startDate,
        endDate = endDate,
        reportText = report_text,
        createdDate = generatedLocalDateTime,
        synced = synced
    )
}
