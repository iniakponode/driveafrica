package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nlg_report")
data class NLGReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val userId: String,
    val reportText: String,
    val dateRange: String,
    val synced: Boolean,
)