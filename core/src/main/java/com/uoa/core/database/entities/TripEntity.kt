package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "trip_data",
    foreignKeys = [
        ForeignKey(entity = DriverProfileEntity::class,
        parentColumns = ["driverProfileId"],
        childColumns = ["driverPId"])],
    indices = [Index(value = ["driverPId"])]
)
data class TripEntity(
    @PrimaryKey(autoGenerate = false)
    val id: UUID,
    val driverPId: UUID?,
    val startDate: Date?,
    val endDate: Date?,
    val startTime: Long,
    var endTime: Long?,
    var synced: Boolean=false
    // Other trip data fields
)