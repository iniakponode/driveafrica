package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "unsafe_behaviour",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(entity = TripEntity::class, parentColumns = ["id"], childColumns = ["tripId"]),
        ForeignKey(entity = LocationEntity::class, parentColumns = ["id"], childColumns = ["locationId"])
    ]

)
data class UnsafeBehaviour(

    @PrimaryKey(autoGenerate = false) val id: UUID,
    val tripId: UUID,
      val locationId: UUID,
    val behaviorType: String,
    val severity: Float,
    val timestamp: Long,
    val synced: Boolean=false,
    val cause: String,


)
