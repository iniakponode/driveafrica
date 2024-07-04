package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID
// define the entity for the driver profile
@Entity(tableName = "driver_profile",
    indices = [Index(value = ["driverProfileId"])]
)
data class DriverProfile(
// define driver profile id
    @PrimaryKey(autoGenerate = false)
    val driverProfileId: UUID,
// define driver email
    val email: String,
)
