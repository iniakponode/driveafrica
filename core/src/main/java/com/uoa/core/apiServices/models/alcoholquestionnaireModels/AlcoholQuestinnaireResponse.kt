package com.uoa.core.apiServices.models.alcoholquestionnaireModels

import java.util.UUID

// Data Model
data class AlcoholQuestionnaireResponse(
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
) {
    override fun toString(): String {
        return "AlcoholQuestionnaireCreate(id=$id, userId=$driverProfileId, drankAlcohol=$drankAlcohol, " +
                "selectedAlcoholTypes=$selectedAlcoholTypes, beerQuantity=$beerQuantity, " +
                "wineQuantity=$wineQuantity, spiritsQuantity=$spiritsQuantity, " +
                "firstDrinkTime=$firstDrinkTime, lastDrinkTime=$lastDrinkTime, " +
                "emptyStomach=$emptyStomach, caffeinatedDrink=$caffeinatedDrink, " +
                "impairmentLevel=$impairmentLevel, plansToDrive=$plansToDrive)"
    }
}