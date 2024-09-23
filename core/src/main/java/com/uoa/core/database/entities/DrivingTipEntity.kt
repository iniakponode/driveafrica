package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.util.UUID
@Entity(tableName = "driving_tips",
    foreignKeys = [
        ForeignKey(entity = DriverProfileEntity::class,
            parentColumns = arrayOf("driverProfileId"),
            childColumns = arrayOf("profileId"),
            onDelete = ForeignKey.CASCADE
        )],
    indices = [Index(value = ["profileId"]),
        Index(value = ["tipId"], unique = true),
        Index(value = ["date"])]
)
data class DrivingTipEntity(
    @PrimaryKey(autoGenerate = false)
    val tipId: UUID,
    val title: String,
    val meaning: String?=null,
    val penalty: String?=null,
    val law: String?=null,
    val hostility: String?=null,
    val summaryTip: String?=null,
    val sync: Boolean = false,
    val date: LocalDate,
    val profileId: UUID
)
