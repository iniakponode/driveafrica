package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip_data"
)
data class TripEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val driverProfileId: Long?,
    val startTime: Long,
    var endTime: Long?,
    var synced: Boolean=false
    // Other trip data fields
)