package com.uoa.core.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.uoa.core.database.entities.AIModelInputsEntity
import com.uoa.core.database.entities.CauseEntity
import com.uoa.core.database.entities.DriverProfileEntity
import com.uoa.core.database.entities.DrivingTipEntity
import com.uoa.core.database.entities.LocationEntity
import com.uoa.core.database.entities.NLGReportEntity
import com.uoa.core.database.entities.QuestionnaireEntity
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.database.entities.ReportStatisticsEntity
import com.uoa.core.database.entities.RoadEntity
import com.uoa.core.database.entities.TripEntity
import com.uoa.core.database.entities.UnsafeBehaviourEntity
import com.uoa.core.model.AIModelInputs
import com.uoa.core.model.BehaviourOccurrence
import com.uoa.core.model.Cause
import com.uoa.core.model.DriverProfile
import com.uoa.core.model.DrivingTip
import com.uoa.core.model.LocationData
import com.uoa.core.model.NLGReport
import com.uoa.core.model.Questionnaire
import com.uoa.core.model.RawSensorData
import com.uoa.core.model.ReportStatistics
import com.uoa.core.model.Road
import com.uoa.core.model.Trip
import com.uoa.core.model.UnsafeBehaviourModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.UUID
import kotlin.String

// RawSensorData to RawSensorDataEntity
fun RawSensorData.toEntity(): RawSensorDataEntity {
    return RawSensorDataEntity(
        id = this.id,
        sensorType = this.sensorType,  // Assuming you need to convert Int to String
        sensorTypeName = this.sensorTypeName,
        values = this.values,
        timestamp = this.timestamp,
        date = this.date,
        accuracy = this.accuracy,
        locationId = this.locationId,
        tripId = this.tripId,
        driverProfileId=this.driverProfileId,
        processed=this.processed,
        sync = this.sync
    )
}

fun RawSensorDataEntity.toDomainModel(): RawSensorData {
    return RawSensorData(
        id = this.id,
        sensorType = this.sensorType.toInt(),  // Assuming you need to convert String to Int
        sensorTypeName = this.sensorTypeName,
        values = this.values,
        timestamp = this.timestamp,
        date = this.date,
        accuracy = this.accuracy,
        locationId = this.locationId,
        tripId = this.tripId,
        driverProfileId=this.driverProfileId,
        processed=this.processed,
        sync = this.sync
    )
}

fun Questionnaire.toEntity(): QuestionnaireEntity{
    return QuestionnaireEntity(
        id=this.id,
        driverProfileId=this.driverProfileId,
        drankAlcohol=this.drankAlcohol,
        selectedAlcoholTypes=this.selectedAlcoholTypes,
        beerQuantity=this.beerQuantity,
        wineQuantity=this.wineQuantity,
        spiritsQuantity=this.spiritsQuantity,
        firstDrinkTime=this.firstDrinkTime,
        lastDrinkTime=this.lastDrinkTime,
        emptyStomach=this.emptyStomach,
        caffeinatedDrink=this.caffeinatedDrink,
        impairmentLevel=this.impairmentLevel,
        date=this.date, // ISO 8601 format
        plansToDrive=this.plansToDrive
    )
}

fun QuestionnaireEntity.toModel(): Questionnaire{
    return Questionnaire(
        id=this.id,
        driverProfileId=this.driverProfileId,
        drankAlcohol=this.drankAlcohol,
        selectedAlcoholTypes=this.selectedAlcoholTypes,
        beerQuantity=this.beerQuantity,
        wineQuantity=this.wineQuantity,
        spiritsQuantity=this.spiritsQuantity,
        firstDrinkTime=this.firstDrinkTime,
        lastDrinkTime=this.lastDrinkTime,
        emptyStomach=this.emptyStomach,
        caffeinatedDrink=this.caffeinatedDrink,
        impairmentLevel=this.impairmentLevel,
        date=this.date, // ISO 8601 format
        plansToDrive=this.plansToDrive
    )
}

fun DrivingTipEntity.toDomainModel(): DrivingTip {
    return DrivingTip(
        tipId = this.tipId,
        driverProfileId=this.driverProfileId,
        title = this.title,
        meaning = this.meaning,
        penalty = this.penalty,
        fine = this.fine,
        law = this.law,
        hostility = this.hostility!!,
        summaryTip = this.summaryTip,
        date = this.date,
        sync = this.sync,
        llm = this.llm
    )
}

fun DrivingTip.toEntity(): DrivingTipEntity {
    return DrivingTipEntity(
        tipId = this.tipId,
        driverProfileId=this.driverProfileId,
        title = this.title,
        meaning = this.meaning,
        penalty = this.penalty,
        fine = this.fine,
        law = this.law,
        hostility = this.hostility,
        summaryTip = this.summaryTip,
        date = this.date,
        sync = this.sync,
        llm = this.llm
    )
}

// Trip to TripEntity
fun Trip.toEntity(): TripEntity {
    return TripEntity(
        id = this.id,
        driverPId = this.driverPId,
        startTime = this.startTime,
        endTime = this.endTime,
        startDate = this.startDate,
        endDate = this.endDate,
        influence = this.influence,
        sync = this.sync
    )
}

fun TripEntity.toDomainModel(): Trip {
    return Trip(
        id = this.id,
        driverPId = this.driverPId,
        startTime = this.startTime,
        endTime = this.endTime,
        startDate = this.startDate,
        endDate = this.endDate,
        influence=this.influence,
        sync = this.sync
    )
}

fun DriverProfileEntity.toDomainModel(): DriverProfile {
    return DriverProfile(
        driverProfileId = this.driverProfileId,
        email = this.email
    )
}

fun DriverProfile.toEntity(): DriverProfileEntity {
    return DriverProfileEntity(
        driverProfileId = this.driverProfileId,
        email = this.email
    )
}

// DbdaResultEntity to DbdaResultEntity
//fun DbdaResult.toEntity(): DbdaResultEntity {
//    return DbdaResultEntity(
//        id = this.id,
//        userId = this.userId,
//        tripDataId = this.tripDataId,
//        harshAcceleration = this.harshAcceleration,
//        harshDeceleration = this.harshDeceleration,
//        tailgaiting = this.tailgaiting,
//        speeding = this.speeding,
//        causes = this.causes,
//        causeUpdated = this.causeUpdated,
//        synced = this.synced,
//        timestamp = this.timestamp,
//        startDate = this.startDate,
//        endDate = this.endDate,
//        distance = this.distance
//        //... other behavior fields
//    )
//}
//
//fun DbdaResultEntity.toDomainModel(): DbdaResult {
//    return DbdaResult(
//        id = this.id,
//        userId = this.userId,
//        tripDataId = this.tripDataId,
//        harshAcceleration = this.harshAcceleration,
//        harshDeceleration = this.harshDeceleration,
//        cornering = this.cornering,
//        speeding = this.speeding,
//        causes = this.causes,
//        causeUpdated = this.causeUpdated,
//        synced = this.synced,
//        timestamp = this.timestamp,
//        startDate = this.startDate,
//        endDate = this.endDate,
//        distance = this.distance,
//        //... other behavior fields
//    )
//}

// NLGReportEntity to NLGReportEntity
fun NLGReport.toEntity(): NLGReportEntity {
    return NLGReportEntity(
        id = this.id,
        userId = this.userId,
        reportText = this.reportText,
        startDate=this.startDate,
        endDate=this.endDate,
        createdDate = this.createdDate,
        sync = this.sync
    )
}

fun NLGReportEntity.toDomainModel(): NLGReport {
    return NLGReport(
        id = this.id,
        userId = this.userId,
        reportText = this.reportText,
        startDate=this.startDate,
        endDate=this.endDate,
        createdDate = this.createdDate,
        sync = this.sync
    )
}


// LocationEntity to Location
fun LocationEntity.toDomainModel(): LocationData {
    return LocationData(
        id = this.id,
        latitude = this.latitude,
        longitude = this.longitude,
        timestamp = this.timestamp,
        date = this.date,
        speed = this.speed.toDouble(),
        distance = this.distance.toDouble(),
        speedLimit = this.speedLimit,
        processed = this.processed,
        sync = this.sync
    )
}

// Location to LocationEntity
fun LocationData.toEntity(): LocationEntity {
    return LocationEntity(
        id = this.id,
        latitude = this.latitude,
        longitude = this.longitude,
        altitude = this.altitude!!.toDouble(),
        timestamp = this.timestamp,
        date = this.date!!,
        speed = this.speed!!.toFloat(),
        distance = this.distance!!.toFloat(),
        speedLimit = this.speedLimit,
        processed = this.processed,
        sync = this.sync
    )

}
fun Road.toEntity(): RoadEntity{
    return RoadEntity(
    id=this.id,
    driverProfileId=this.driverProfileId,
    name=this.name,
    roadType=this.roadType,
    speedLimit=this.speedLimit,
    latitude=this.latitude,
    longitude=this.latitude,
    radius=this.radius)

}

fun RoadEntity.toDomainModel(): Road{
    return Road(
    id=this.id,
    driverProfileId=this.driverProfileId,
    name=this.name,
    roadType=this.roadType,
    speedLimit=this.speedLimit,
    latitude=this.latitude,
    longitude=this.latitude,
    radius=this.radius)

}

fun UnsafeBehaviourEntity.toDomainModel(): UnsafeBehaviourModel {
    val safeDate = this.date ?: Date() // Provide a default value if the date is null
    val safeUpdatedAt = this.updatedAt ?: Date() // Provide a default value if updatedAt is null

    return UnsafeBehaviourModel(
        id = this.id,
        driverProfileId=this.driverProfileId,
        tripId = this.tripId,
        locationId = this.locationId,
        behaviorType = this.behaviorType,
        severity = this.severity,
        timestamp = this.timestamp,
        date = safeDate,
        updatedAt = safeUpdatedAt,
        updated=this.updated,
        processed = this.processed,
        sync = this.sync,

    )
}

fun UnsafeBehaviourModel.toEntity(): UnsafeBehaviourEntity {
    val safeDate = this.date ?: Date() // Provide a default value if the date is null
    val safeUpdatedAt = this.updatedAt ?: Date() // Provide a default value if updatedAt is null

    return UnsafeBehaviourEntity(
        id = this.id,
        driverProfileId=this.driverProfileId,
        tripId = this.tripId,
        locationId = this.locationId,
        behaviorType = this.behaviorType,
        severity = this.severity,
        timestamp = this.timestamp,
        date = safeDate,
        updatedAt = safeUpdatedAt,
        updated=this.updated,
        processed = this.processed,
        sync = this.sync,
    )
}

fun CauseEntity.toDomainModel(): Cause {
    return Cause(
        id= this.id,
        name=this.name,
        unsafeBehaviourId=this.unsafeBehaviourId,
//        tripId = this.tripId,
        influence=this.influence,
        createdAt=this.createdAt,
        updatedAt=this.updatedAt

    )
}

fun Cause.toEntity(): CauseEntity {

    return CauseEntity(id= this.id,
    name=this.name,
    unsafeBehaviourId=this.unsafeBehaviourId,
//        tripId = this.tripId,
    influence=this.influence,
    createdAt=this.createdAt,
    updatedAt=this.updatedAt
    )
}

fun AIModelInputsEntity.toDomainModel(): AIModelInputs {
    return AIModelInputs(
        id = this.id,
        tripId = this.tripId,
        driverProfileId=this.driverProfileId,
        timestamp = this.timestamp,
        startTimestamp =this.startTimestamp,
        endTimestamp =this.endTimestamp,
        date = this.date,
        hourOfDayMean = this.hourOfDayMean,
        dayOfWeekMean = this.dayOfWeekMean,
        speedStd = this.speedStd,
        courseStd = this.courseStd,
        accelerationYOriginalMean = this.accelerationYOriginalMean,
        sync=this.sync,
        processed = this.processed
    )
}
fun AIModelInputs.toEntity(): AIModelInputsEntity {
    return AIModelInputsEntity(
        id = this.id,
        driverProfileId=this.driverProfileId,
        tripId = this.tripId,
        timestamp = this.timestamp,
        startTimestamp=this.startTimestamp,
        endTimestamp=this.endTimestamp,
        date = this.date,
        hourOfDayMean = this.hourOfDayMean,
        dayOfWeekMean = this.dayOfWeekMean,
        speedStd = this.speedStd,
        courseStd = this.courseStd,
        accelerationYOriginalMean = this.accelerationYOriginalMean,
        sync = this.sync,
        processed = this.processed
    )
}

// Conversion extensions
fun ReportStatisticsEntity.toDomainModel(): ReportStatistics {
    val gson = Gson()
    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    // Deserialize JSON fields back to their domain types
    // mostFrequentBehaviourOccurrences
    val occurrencesType = object : TypeToken<List<BehaviourOccurrence>>() {}.type
    val parsedOccurrences: List<BehaviourOccurrence> =
        gson.fromJson(mostFrequentBehaviourOccurrences, occurrencesType) ?: emptyList()

    // tripWithMostIncidences
    val parsedTripWithMostIncidences: Trip? = tripWithMostIncidences?.let {
        gson.fromJson(it, Trip::class.java)
    }

    // tripsPerAggregationUnit: Map<LocalDate, Int>
    val mapLocalDateIntType = object : TypeToken<Map<String, Int>>() {}.type
    val parsedTripsPerAggregationUnit: Map<LocalDate, Int> =
        gson.fromJson<Map<String, Int>>(tripsPerAggregationUnit, mapLocalDateIntType)
            ?.mapKeys { LocalDate.parse(it.key, dateFormatter) }
            ?: emptyMap()

    // incidencesPerAggregationUnit: Map<LocalDate, Int>
    val parsedIncidencesPerAggregationUnit: Map<LocalDate, Int> =
        gson.fromJson<Map<String, Int>>(incidencesPerAggregationUnit, mapLocalDateIntType)
            ?.mapKeys { LocalDate.parse(it.key, dateFormatter) }
            ?: emptyMap()

    // incidencesPerTrip: Map<UUID, Int>
    val mapUUIDIntType = object : TypeToken<Map<String, Int>>() {}.type
    val parsedIncidencesPerTrip: Map<UUID, Int> =
        gson.fromJson<Map<String, Int>>(incidencesPerTrip, mapUUIDIntType)
            ?.mapKeys { UUID.fromString(it.key) }
            ?: emptyMap()

    // tripsWithAlcoholInfluencePerAggregationUnit: Map<LocalDate, Int>
    val parsedTripsWithAlcoholInfluencePerAggregationUnit: Map<LocalDate, Int> =
        gson.fromJson<Map<String, Int>>(tripsWithAlcoholInfluencePerAggregationUnit, mapLocalDateIntType)
            ?.mapKeys { LocalDate.parse(it.key, dateFormatter) }
            ?: emptyMap()

    // lastTripDuration: String? to Duration?
    val parsedLastTripDuration = lastTripDuration?.let { java.time.Duration.parse(it) }

    // Convert createdDate (Date) to LocalDate (assuming domain model uses LocalDate)

    return ReportStatistics(
        id = this.id,
        driverProfileId = this.driverProfileId,
        tripId = this.tripId,
        createdDate = this.createdDate,
        startDate = this.startDate,
        endDate = this.endDate,
        totalIncidences = this.totalIncidences,
        mostFrequentUnsafeBehaviour = this.mostFrequentUnsafeBehaviour,
        mostFrequentBehaviourCount = this.mostFrequentBehaviourCount,
        mostFrequentBehaviourOccurrences = parsedOccurrences,
        tripWithMostIncidences = parsedTripWithMostIncidences,
        tripsPerAggregationUnit = parsedTripsPerAggregationUnit,
        aggregationUnitWithMostIncidences = this.aggregationUnitWithMostIncidences,
        incidencesPerAggregationUnit = parsedIncidencesPerAggregationUnit,
        incidencesPerTrip = parsedIncidencesPerTrip,
        aggregationLevel = this.aggregationLevel,
        aggregationUnitsWithAlcoholInfluence = this.aggregationUnitsWithAlcoholInfluence,
        tripsWithAlcoholInfluencePerAggregationUnit = parsedTripsWithAlcoholInfluencePerAggregationUnit,
        sync = this.sync,
        processed = this.processed,
        numberOfTrips = this.numberOfTrips,
        numberOfTripsWithIncidences = this.numberOfTripsWithIncidences,
        numberOfTripsWithAlcoholInfluence = this.numberOfTripsWithAlcoholInfluence,
        lastTripDuration = parsedLastTripDuration,
        lastTripDistance = this.lastTripDistance,
        lastTripAverageSpeed = this.lastTripAverageSpeed,
        lastTripStartLocation = this.lastTripStartLocation,
        lastTripEndLocation = this.lastTripEndLocation,
        lastTripStartTime = this.lastTripStartTime,
        lastTripEndTime = this.lastTripEndTime,
        lastTripInfluence = this.lastTripInfluence
    )
}


fun ReportStatistics.toEntity(): ReportStatisticsEntity {
    val gson = Gson()
    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    // Convert complex fields to JSON
    val occurrencesJson = gson.toJson(mostFrequentBehaviourOccurrences)

    // tripWithMostIncidences as JSON
    val tripJson = tripWithMostIncidences?.let { gson.toJson(it) }

    // Convert Map<LocalDate, Int> to Map<String, Int> then to JSON
    fun mapLocalDateIntToJson(map: Map<LocalDate, Int>): String {
        val stringKeyMap = map.mapKeys { it.key.format(dateFormatter) }
        return gson.toJson(stringKeyMap)
    }

    val tripsPerAggregationUnitJson = mapLocalDateIntToJson(tripsPerAggregationUnit)
    val incidencesPerAggregationUnitJson = mapLocalDateIntToJson(incidencesPerAggregationUnit)
    val tripsWithAlcoholInfluencePerAggregationUnitJson = mapLocalDateIntToJson(tripsWithAlcoholInfluencePerAggregationUnit)

    // Convert Map<UUID, Int> to Map<String, Int> then to JSON
    fun mapUUIDIntToJson(map: Map<UUID, Int>): String {
        val stringKeyMap = map.mapKeys { it.key.toString() }
        return gson.toJson(stringKeyMap)
    }

    val incidencesPerTripJson = mapUUIDIntToJson(incidencesPerTrip)

    // lastTripDuration: Duration? to String?
    val durationString = lastTripDuration?.toString()

    // Convert createdDate (LocalDate) to Date
    val zoneId = ZoneId.systemDefault()
//    val createdInstant = createdDate.atStartOfDay(zoneId).toInstant()
//    val createdDateAsDate = Date.from(createdInstant)

    return ReportStatisticsEntity(
        id = id,
        driverProfileId = driverProfileId,
        tripId = tripId,
        createdDate = this.createdDate,
        startDate = startDate,
        endDate = endDate,
        totalIncidences = totalIncidences,
        mostFrequentUnsafeBehaviour = mostFrequentUnsafeBehaviour,
        mostFrequentBehaviourCount = mostFrequentBehaviourCount,
        mostFrequentBehaviourOccurrences = occurrencesJson,
        tripWithMostIncidences = tripJson,
        tripsPerAggregationUnit = tripsPerAggregationUnitJson,
        aggregationUnitWithMostIncidences = aggregationUnitWithMostIncidences,
        incidencesPerAggregationUnit = incidencesPerAggregationUnitJson,
        incidencesPerTrip = incidencesPerTripJson,
        aggregationLevel = aggregationLevel,
        aggregationUnitsWithAlcoholInfluence = aggregationUnitsWithAlcoholInfluence,
        tripsWithAlcoholInfluencePerAggregationUnit = tripsWithAlcoholInfluencePerAggregationUnitJson,
        sync = sync,
        processed = processed,
        numberOfTrips = numberOfTrips,
        numberOfTripsWithIncidences = numberOfTripsWithIncidences,
        numberOfTripsWithAlcoholInfluence = numberOfTripsWithAlcoholInfluence,
        lastTripDuration = durationString,
        lastTripDistance = lastTripDistance,
        lastTripAverageSpeed = lastTripAverageSpeed,
        lastTripStartLocation = lastTripStartLocation,
        lastTripEndLocation = lastTripEndLocation,
        lastTripStartTime = lastTripStartTime,
        lastTripEndTime = lastTripEndTime,
        lastTripInfluence = lastTripInfluence
    )
}

