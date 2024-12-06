package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "roads",
    indices = [
        Index(value=["latitude"]),
        Index(value=["longitude"]),
        Index(value=["speedLimit"])
    ]
    )
data class RoadEntity (
    @PrimaryKey(autoGenerate = false)
    val id: UUID,
    val name: String,
    val roadType: String,
   val speedLimit: Int,
    val latitude: Double,
   val longitude: Double
)
