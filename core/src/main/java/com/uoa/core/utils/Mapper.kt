package com.uoa.core.utils

import com.uoa.core.database.entities.AIModelInputsEntity
import com.uoa.core.database.entities.CauseEntity
import com.uoa.core.database.entities.DriverProfileEntity
import com.uoa.core.database.entities.DrivingTipEntity
import com.uoa.core.database.entities.LocationEntity
import com.uoa.core.database.entities.NLGReportEntity
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.database.entities.TripEntity
import com.uoa.core.database.entities.UnsafeBehaviourEntity
import com.uoa.core.model.AIModelInputs
import com.uoa.core.model.Cause
import com.uoa.core.model.DriverProfile
import com.uoa.core.model.DrivingTip
import com.uoa.core.model.LocationData
import com.uoa.core.model.NLGReport
import com.uoa.core.model.RawSensorData
import com.uoa.core.model.Trip
import com.uoa.core.model.UnsafeBehaviourModel
import com.uoa.core.network.model.CauseData
import java.util.Date
import java.util.UUID

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
        sync = this.sync
    )
}

fun DrivingTipEntity.toDomainModel(): DrivingTip {
    return DrivingTip(
        tipId = this.tipId,
        title = this.title,
        meaning = this.meaning,
        penalty = this.penalty,
        fine = this.fine,
        law = this.law,
        hostility = this.hostility!!,
        summaryTip = this.summaryTip,
        profileId = this.profileId,
        date = this.date,
        sync = this.sync,
        llm = this.llm
    )
}

fun DrivingTip.toEntity(): DrivingTipEntity {
    return DrivingTipEntity(
        tipId = this.tipId,
        title = this.title,
        meaning = this.meaning,
        penalty = this.penalty,
        fine = this.fine,
        law = this.law,
        hostility = this.hostility,
        summaryTip = this.summaryTip,
        profileId = this.profileId,
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
        synced = this.synced
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
        synced = this.synced
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
        dateRange = this.dateRange,
        synced = this.synced
    )
}

fun NLGReportEntity.toDomainModel(): NLGReport {
    return NLGReport(
        id = this.id,
        userId = this.userId,
        reportText = this.reportText,
        dateRange = this.dateRange,
        synced = this.synced
    )
}


// LocationEntity to Location
fun LocationEntity.toDomainModel(): LocationData {
    return LocationData(
        id = this.id,
        latitude = this.latitude.toLong(),
        longitude = this.longitude.toLong(),
        timestamp = this.timestamp,
        date = this.date,
        speed = this.speed.toDouble(),
        distance = this.distance.toDouble(),
        sync = this.sync
    )
}

// Location to LocationEntity
fun LocationData.toEntity(): LocationEntity {
    return LocationEntity(
        id = this.id,
        latitude = this.latitude.toDouble(),
        longitude = this.longitude.toDouble(),
        altitude = this.altitude!!.toDouble(),
        timestamp = this.timestamp,
        date = this.date!!,
        speed = this.speed!!.toFloat(),
        distance = this.distance!!.toFloat(),
        sync = this.sync
    )

}

fun UnsafeBehaviourEntity.toDomainModel(): UnsafeBehaviourModel {
    val safeDate = this.date ?: Date() // Provide a default value if the date is null
    val safeUpdatedAt = this.updatedAt ?: Date() // Provide a default value if updatedAt is null

    return UnsafeBehaviourModel(
        id = this.id,
        tripId = this.tripId,
        locationId = this.locationId,
        behaviorType = this.behaviorType,
        severity = this.severity,
        timestamp = this.timestamp,
        date = safeDate,
        updatedAt = safeUpdatedAt,
        updated=this.updated,
        synced = this.synced,
        alcoholInfluence = this.alcoholInfluence
    )
}

fun UnsafeBehaviourModel.toEntity(): UnsafeBehaviourEntity {
    val safeDate = this.date ?: Date() // Provide a default value if the date is null
    val safeUpdatedAt = this.updatedAt ?: Date() // Provide a default value if updatedAt is null

    return UnsafeBehaviourEntity(
        id = this.id,
        tripId = this.tripId,
        locationId = this.locationId,
        behaviorType = this.behaviorType,
        severity = this.severity,
        timestamp = this.timestamp,
        date = safeDate,
        updatedAt = safeUpdatedAt,
        updated=this.updated,
        synced = this.synced,
        alcoholInfluence = this.alcoholInfluence
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
        timestamp = this.timestamp,
        date = this.date,
        hourOfDayMean = this.hourOfDayMean,
        dayOfWeekMean = this.dayOfWeekMean,
        speedStd = this.speedStd,
        courseStd = this.courseStd,
        accelerationYOriginalMean = this.accelerationYOriginalMean
    )
}
fun AIModelInputs.toEntity(): AIModelInputsEntity {
    return AIModelInputsEntity(
        id = this.id,
        tripId = this.tripId,
        timestamp = this.timestamp,
        date = this.date,
        hourOfDayMean = this.hourOfDayMean,
        dayOfWeekMean = this.dayOfWeekMean,
        speedStd = this.speedStd,
        courseStd = this.courseStd,
        accelerationYOriginalMean = this.accelerationYOriginalMean
    )
}
