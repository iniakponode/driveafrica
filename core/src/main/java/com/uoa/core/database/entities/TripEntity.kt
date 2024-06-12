package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "trip_data"
)
data class TripEntity(
    @PrimaryKey(autoGenerate = false)
    val id: UUID,
    val driverProfileId: Long?,
    val startTime: Long,
    var endTime: Long?,
    var synced: Boolean=false
    // Other trip data fields
)