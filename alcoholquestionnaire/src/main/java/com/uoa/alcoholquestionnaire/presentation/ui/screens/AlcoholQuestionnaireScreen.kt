package com.uoa.alcoholquestionnaire.presentation.ui.screens

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uoa.core.apiServices.models.alcoholquestionnaireModels.AlcoholQuestionnaireCreate
import com.uoa.core.utils.Constants.Companion.DRIVER_PROFILE_ID
import com.uoa.core.utils.Constants.Companion.PREFS_NAME
import java.util.UUID

@Composable
fun AlcoholQuestionnaireScreen(
    onSubmit: (AlcoholQuestionnaireCreate) -> Unit,
    onCancel: () -> Unit
) {
    var drankAlcohol by remember { mutableStateOf(false) }
    var selectedAlcoholTypes by remember { mutableStateOf(listOf<String>()) }
    var beerQuantity by remember { mutableStateOf(TextFieldValue()) }
    var wineQuantity by remember { mutableStateOf(TextFieldValue()) }
    var spiritsQuantity by remember { mutableStateOf(TextFieldValue()) }
    var firstDrinkTime by remember { mutableStateOf(TextFieldValue()) }
    var lastDrinkTime by remember { mutableStateOf(TextFieldValue()) }
    var emptyStomach by remember { mutableStateOf(false) }
    var caffeinatedDrink by remember { mutableStateOf(false) }
    var impairmentLevel by remember { mutableStateOf(0f) }
    var plansToDrive by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val savedProfileId = prefs.getString(DRIVER_PROFILE_ID, null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Alcohol Consumption Questionnaire", fontSize = 18.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Button(onClick = onCancel) {
                Text("Cancel")
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Did you drink any alcohol in the past 24 hours?")
            Spacer(modifier = Modifier.width(14.dp))
            Switch(checked = drankAlcohol, onCheckedChange = { drankAlcohol = it })
        }

        if (drankAlcohol) {
            Text("Types of alcohol consumed (Select all that apply)")
            listOf("Beer", "Wine", "Spirits").forEach { type ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = selectedAlcoholTypes.contains(type),
                        onCheckedChange = {
                            selectedAlcoholTypes = if (selectedAlcoholTypes.contains(type))
                                selectedAlcoholTypes - type else selectedAlcoholTypes + type
                        }
                    )
                    Text(type)
                }
            }
            if (selectedAlcoholTypes.contains("Beer")) {
                Text("Beer: Number of bottles/cans (12 oz or 355 ml)")
                BasicTextField(
                    value = beerQuantity,
                    onValueChange = { beerQuantity = it },
                    modifier = Modifier.fillMaxWidth()
                        .border(2.dp, Color.Blue, RoundedCornerShape(8.dp)) // Add border with color and shape
                        .padding(8.dp) // Padding inside the border for better spacing
                )
            }

            if (selectedAlcoholTypes.contains("Wine")) {
                Text("Wine: Number of glasses (5 oz or 148 ml)")
                BasicTextField(
                    value = wineQuantity,
                    onValueChange = { wineQuantity = it },
                    modifier = Modifier.fillMaxWidth()
                        .border(2.dp, Color.Blue, RoundedCornerShape(8.dp)) // Add border with color and shape
                        .padding(8.dp) // Padding inside the border for better spacing
                )
            }
            if (selectedAlcoholTypes.contains("Spirits")) {
                Text("Spirits: Number of shots (1.5 oz or 44 ml)")
                BasicTextField(
                    value = spiritsQuantity,
                    onValueChange = { spiritsQuantity = it },
                    modifier = Modifier.fillMaxWidth()
                        .border(2.dp, Color.Blue, RoundedCornerShape(8.dp)) // Add border with color and shape
                        .padding(8.dp) // Padding inside the border for better spacing
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Time of first drink (e.g., 8:00 PM)")
            BasicTextField(
                value = firstDrinkTime,
                onValueChange = { firstDrinkTime = it },
                modifier = Modifier.fillMaxWidth()
                    .border(2.dp, Color.Blue, RoundedCornerShape(8.dp)) // Add border with color and shape
                    .padding(8.dp) // Padding inside the border for better spacing
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Time of last drink (e.g., 10:00 PM)")
            BasicTextField(
                value = lastDrinkTime,
                onValueChange = { lastDrinkTime = it },
                modifier = Modifier.fillMaxWidth()
                    .border(2.dp, Color.Blue, RoundedCornerShape(8.dp)) // Add border with color and shape
                    .padding(8.dp) // Padding inside the border for better spacing
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Was the alcohol consumed on an empty stomach?")
                Spacer(modifier = Modifier.width(14.dp))
                Switch(checked = emptyStomach, onCheckedChange = { emptyStomach = it })
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Did you consume any caffeinated drinks after drinking alcohol?")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(checked = caffeinatedDrink, onCheckedChange = { caffeinatedDrink = it })
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Self-Assessment of Impairment")
        Slider(
            value = impairmentLevel,
            onValueChange = { impairmentLevel = it },
            valueRange = 0f..10f,
            steps = 10,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Impairment Level: ${impairmentLevel.toInt()}")


        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Do you plan to drive within the next hour?")
            Spacer(modifier = Modifier.width(8.dp))
            Switch(checked = plansToDrive, onCheckedChange = { plansToDrive = it })
        }

        Button(
            onClick = {
                val response = AlcoholQuestionnaireCreate(
                    id = UUID.randomUUID(), // Generates a unique ID for each response
                    driverProfileId = UUID.fromString(savedProfileId), // Ensure `savedProfileId` is not null
                    drankAlcohol = drankAlcohol,
                    selectedAlcoholTypes = selectedAlcoholTypes.joinToString(","), // Convert list to comma-separated string
                    beerQuantity = beerQuantity.text,
                    wineQuantity = wineQuantity.text,
                    spiritsQuantity = spiritsQuantity.text,
                    firstDrinkTime = firstDrinkTime.text,
                    lastDrinkTime = lastDrinkTime.text,
                    emptyStomach = emptyStomach,
                    caffeinatedDrink = caffeinatedDrink,
                    impairmentLevel = impairmentLevel.toInt(),
                    plansToDrive = plansToDrive
                )
                onSubmit(response) // Pass the AlcoholQuestionnaireCreate object
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Submit")
        }
    }
}