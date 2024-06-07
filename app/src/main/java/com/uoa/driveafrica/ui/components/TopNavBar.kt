package com.uoa.driveafrica.ui.components

// top navigation bar with app name and user profile picture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.materialIcon
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter

@Composable
fun TopNavBar(
    navigateBack: () -> Unit
) {
    Column {
        Box(
            modifier = Modifier
                .background(Color.DarkGray)
                .fillMaxWidth()
                .height(50.dp)
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start =5.dp)
            ) {
                IconButton(onClick = navigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black
                    )
                }

                Text(
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = 5.dp),
                    color = Color.White,
                    text="Safe Drive Africa",
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp)
                )

                Spacer(modifier = Modifier.weight(1f))

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

