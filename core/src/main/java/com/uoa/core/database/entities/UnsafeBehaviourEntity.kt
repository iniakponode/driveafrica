package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "unsafe_behaviour",
    foreignKeys = [
        ForeignKey(entity = TripEntity::class, parentColumns = ["id"], childColumns = ["tripId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = LocationEntity::class, parentColumns = ["id"], childColumns = ["locationId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [
        Index(value = ["tripId"]),
        Index(value = ["locationId"]),
        Index(value = ["date"]),
        Index(value = ["synced"])
    ],

)
data class UnsafeBehaviourEntity(

    @PrimaryKey(autoGenerate = false) val id: UUID,
    val tripId: UUID,
    val locationId: UUID?,
    val behaviorType: String,
    val severity: Float,
    val timestamp: Long,
    val date: Date?,
    val updatedAt:Date?,
    val updated:Boolean=false,
    val synced: Boolean=false,
    val alcoholInfluence: Boolean=false,
    )
