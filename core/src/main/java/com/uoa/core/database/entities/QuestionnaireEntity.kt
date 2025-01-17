package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// Data Model
@Entity(tableName = "questionnaire_responses",
        foreignKeys = [
            ForeignKey(entity = DriverProfileEntity::class,
                parentColumns = ["driverProfileId"],
                childColumns=["driverProfileId"],
                onDelete = ForeignKey.CASCADE)],
                indices = [
                    Index(value=["id"]),
                    Index(value = ["driverProfileId"])
                ]
    )
data class QuestionnaireEntity(
    @PrimaryKey(autoGenerate = false)
    val id: UUID,
    val driverProfileId: UUID,
    val drankAlcohol: Boolean,
    val selectedAlcoholTypes: String,
    val beerQuantity: String,
    val wineQuantity: String,
    val spiritsQuantity: String,
    val firstDrinkTime: String,
    val lastDrinkTime: String,
    val emptyStomach: Boolean,
    val caffeinatedDrink: Boolean,
    val impairmentLevel: Int,
    val plansToDrive: Boolean
)