package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "driving_behavior_analysis")
data class DbdaResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val userId: String,
    val tripDataId: Long,
    val harshAcceleration: Boolean,
    val harshDeceleration: Boolean,
    val tailgaiting: Boolean,
    val speeding: Boolean,
    val causes: String,
    val causeUpdated: Boolean,
    val synced: Boolean,
    val timestamp: Long,
    val startDate: String,
    val endDate: String,
    val distance: String
    //... other behavior fields

)
