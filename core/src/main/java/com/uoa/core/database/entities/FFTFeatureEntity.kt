package com.uoa.core.database.entities

// FFTFeatureEntity.kt
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fft_features")
data class FFTFeatureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val label: String,
    val energy: Double,
    val dominantFrequency: Double,
    val entropy: Double
)