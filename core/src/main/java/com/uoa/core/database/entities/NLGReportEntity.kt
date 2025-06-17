package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "nlg_report",
    foreignKeys = [ForeignKey(entity = DriverProfileEntity::class,
        parentColumns = ["driverProfileId"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE)],
    indices = [
        Index(value = ["id"], unique = true),
        Index(value = ["userId"])
    ]
)
data class NLGReportEntity(
    @PrimaryKey(autoGenerate = false)
    val id: UUID,
    val userId: UUID,
    val tripId: UUID?=null,
    val reportText: String,
    val startDate: java.time.LocalDateTime?=null,
    val endDate: java.time.LocalDateTime?=null,
    val createdDate: java.time.LocalDateTime,
    val sync: Boolean=false,
)