package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.uoa.core.model.SyncState
import java.util.Date
import java.util.UUID

@Entity(tableName = "trip_data",
    foreignKeys = [
        ForeignKey(entity = DriverProfileEntity::class,
        parentColumns = ["driverProfileId"],
        childColumns = ["driverPId"],
            onDelete = ForeignKey.CASCADE)],

    indices = [
        Index(value = ["driverPId"]),
        Index(value = ["id"], unique = true)
    ],

)
data class TripEntity(
    @PrimaryKey(autoGenerate = false)
    val id: UUID,
    val driverPId: UUID?,
    val startDate: Date?,
    val endDate: Date?,
    val startTime: Long,
    var endTime: Long?,
    var influence: String?,
    var sync: Boolean=false,
    val alcoholProbability: Float? = null,
    val userAlcoholResponse: String? = null,
    val syncState: SyncState? = null
    // Other trip data fields
)
