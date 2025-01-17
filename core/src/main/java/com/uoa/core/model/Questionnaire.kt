package com.uoa.core.model

import java.util.UUID

data class Questionnaire(
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
