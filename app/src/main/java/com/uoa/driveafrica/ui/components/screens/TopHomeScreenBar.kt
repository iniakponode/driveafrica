package com.uoa.driveafrica.ui.components.screens

// search screen composable with search by date button at the right and search icon at the left with a user profile icon at the right top corner
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarWithDateButton() {
    var searchText by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Search Safety Tips") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search Icon") },
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .border(1.dp, Color.Gray, MaterialTheme.shapes.small),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.Magenta,
                unfocusedTextColor = Color.hsv(0f,1f,0.8f),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,

            )
        )

        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = {
            // Call your API here with the search text
        }) {
            Icon(Icons.Filled.DateRange, contentDescription = "Search by date")
        }
    }
}