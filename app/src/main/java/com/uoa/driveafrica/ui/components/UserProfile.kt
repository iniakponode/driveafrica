package com.uoa.driveafrica.ui.components

// user profile composable with user details and user profile picture at the top and user profile management options at the bottom
import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun UserProfile() {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Gray,
                modifier = Modifier
                    .size(100.dp)
                    .padding(8.dp)
            ) {
                // user profile picture
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 120.dp)
            ) {
                Text("User Name", style = MaterialTheme.typography.bodySmall)
                Text("User Email", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            // user profile management options
        }, modifier = Modifier.padding(8.dp)) {
            Text("Edit Profile")
        }
    }
}
//preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
fun UserProfilePreview() {
    UserProfile()
}