package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "ai_model_inputs",
    indices = [
        Index(value = ["tripId"]),
        Index(value = ["id"]),
    ],
    foreignKeys = [
        ForeignKey(entity = TripEntity::class,
            parentColumns = ["tripId"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE)
    ])
data class AIModelInputsEntity(
    @PrimaryKey(autoGenerate = false)
    val id: UUID,
    val tripId: UUID,
    val timestamp: Long,
    val date: Date,
    val hourOfDayMean: Double,
    val dayOfWeekMean: Float,
    val speedStd: Float,
    val courseStd: Float,
    val accelerationYOriginalMean: Float
    )
