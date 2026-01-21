package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.Index
import java.util.UUID

@Entity(
    tableName = "trip_summary_behaviour",
    primaryKeys = ["tripId", "behaviourType"],
    indices = [
        Index(value = ["tripId"]),
        Index(value = ["behaviourType"])
    ]
)
data class TripSummaryBehaviourEntity(
    val tripId: UUID,
    val behaviourType: String,
    val count: Int
)
