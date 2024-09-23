package com.uoa.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DAAppTopNavBar(
    navigateBack: () -> Unit,
    navigateHome: () -> Unit
) {
    Column {
        Box(
            modifier = Modifier
                .background(Color.Blue)
                .fillMaxWidth()
                .height(50.dp)
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 5.dp)
            ) {
                IconButton(onClick = navigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,  // Use Default if AutoMirrored is not available
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Making the Text clickable to navigate to the home screen
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { navigateHome() }
                        .padding(start = 10.dp),
                    text = "Safe Drive Africa",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall  // Updated for consistency with Compose Material Typography
                )

                IconButton(onClick = { /*TODO: Handle user icon click*/ }) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "User",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
