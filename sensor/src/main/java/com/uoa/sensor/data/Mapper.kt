package com.uoa.sensor.data

import com.uoa.core.database.entities.DbdaResultEntity
import com.uoa.core.database.entities.LocationEntity
import com.uoa.core.database.entities.NLGReportEntity
import com.uoa.core.database.entities.RawSensorDataEntity
import com.uoa.core.database.entities.TripEntity
import com.uoa.sensor.data.model.DbdaResult
import com.uoa.sensor.data.model.LocationData
import com.uoa.sensor.data.model.NLGReport
import com.uoa.sensor.data.model.RawSensorData
import com.uoa.sensor.data.model.Trip

// RawSensorData to RawSensorDataEntity
fun RawSensorData.toEntity(): RawSensorDataEntity {
    return RawSensorDataEntity(
        id = this.id,
        sensorType = this.sensorType.toString(),  // Assuming you need to convert Int to String
        sensorTypeName = this.sensorTypeName,
        values = this.values,
        timestamp = this.timestamp,
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
        accuracy = this.accuracy,
        locationId = this.locationId,
        tripId = this.tripId,
        sync = this.sync
    )
}

// Trip to TripEntity
fun Trip.toEntity(): TripEntity {
    return TripEntity(
        id = this.id,
        driverProfileId = this.driverProfileId,
        startTime = this.startTime,
        endTime = this.endTime,
        synced = this.synced
    )
}

fun TripEntity.toDomainModel(): Trip {
    return Trip(
        id = this.id,
        driverProfileId = this.driverProfileId,
        startTime = this.startTime,
        endTime = this.endTime,
        synced = this.synced
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
        speed = this.speed?.toDouble(),
        distance = this.distance?.toDouble(),
        sync = this.sync
    )
}

// Location to LocationEntity
fun LocationData.toEntity(): LocationEntity {
    return LocationEntity(
        id = this.id,
        latitude = this.latitude.toDouble(),
        longitude = this.longitude.toDouble(),
        timestamp = this.timestamp,
        speed = this.speed?.toFloat(),
        distance = this.distance?.toFloat(),
        sync = this.sync
    )
}
