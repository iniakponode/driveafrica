package com.uoa.core.database.entities

import androidx.room.Embedded
import androidx.room.Relation

data class TripSummaryWithBehaviours(
    @Embedded val summary: TripSummaryEntity,
    @Relation(
        parentColumn = "tripId",
        entityColumn = "tripId"
    )
    val behaviours: List<TripSummaryBehaviourEntity>
)
