package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import java.util.Date
import java.util.UUID

@Entity(tableName = "ai_model_inputs",
    indices = [
        Index(value = ["tripId"]),
        Index(value = ["id"]),
        Index(value = ["driverProfileId"])
    ],
    foreignKeys = [
        ForeignKey(entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = DriverProfileEntity::class,
            parentColumns = ["driverProfileId"],
            childColumns = ["driverProfileId"],
            onDelete = ForeignKey.CASCADE)
    ])
data class AIModelInputsEntity(
    @PrimaryKey(autoGenerate = false)
    val id: UUID,
    val tripId: UUID,
    val driverProfileId: UUID,
    val timestamp: Long,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val date: Date?,
    val hourOfDayMean: Double,
    val dayOfWeekMean: Float,
    val speedStd: Float,
    val courseStd: Float,
    val accelerationYOriginalMean: Float,
    val processed: Boolean=false,
    val sync: Boolean = false
    )
