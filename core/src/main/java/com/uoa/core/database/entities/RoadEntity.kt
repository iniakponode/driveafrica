package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "roads",
    foreignKeys =[ForeignKey(
        entity = DriverProfileEntity::class,
        parentColumns = ["driverProfileId"],
        childColumns = ["driverProfileId"]
    )],
    indices = [
        Index(value=["latitude"]),
        Index(value=["longitude"]),
        Index(value=["speedLimit"]),
        Index(value = ["driverProfileId"]),
        Index(value = ["id"], unique = true)
    ]
    )
data class RoadEntity (
    @PrimaryKey(autoGenerate = false)
    val id: UUID,
    val driverProfileId: UUID,
    val name: String,
    val roadType: String,
   val speedLimit: Int,
    val latitude: Double,
   val longitude: Double
)
