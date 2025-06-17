//package com.uoa.alcoholquestionnaire.presentation.ui.screens
//
//// user profile composable with user details and user profile picture at the top and user profile management options at the bottom
//import android.content.res.Configuration
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.material3.*
//import androidx.compose.ui.unit.dp
//
//@androidx.compose.runtime.Composable
//fun UserProfile() {
//    androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.Companion.fillMaxSize()) {
//        androidx.compose.foundation.layout.Box(
//            modifier = androidx.compose.ui.Modifier.Companion
//                .fillMaxWidth()
//                .height(200.dp)
//                .padding(16.dp)
//        ) {
//            androidx.compose.material3.Surface(
//                shape = androidx.compose.foundation.shape.CircleShape,
//                color = androidx.compose.ui.graphics.Color.Companion.Gray,
//                modifier = androidx.compose.ui.Modifier.Companion
//                    .size(100.dp)
//                    .padding(8.dp)
//            ) {
//                // user profile picture
//            }
//            androidx.compose.foundation.layout.Column(
//                modifier = androidx.compose.ui.Modifier.Companion
//                    .fillMaxSize()
//                    .padding(start = 120.dp)
//            ) {
//                androidx.compose.material3.Text(
//                    "User Name",
//                    style = androidx.compose.material3.Typography.bodySmall
//                )
//                androidx.compose.material3.Text(
//                    "User Email",
//                    style = androidx.compose.material3.Typography.bodySmall
//                )
//            }
//        }
//        androidx.compose.foundation.layout.Spacer(
//            modifier = androidx.compose.ui.Modifier.Companion.height(
//                16.dp
//            )
//        )
//        androidx.compose.material3.Button(onClick = {
//            // user profile management options
//        }, modifier = androidx.compose.ui.Modifier.Companion.padding(8.dp)) {
//            androidx.compose.material3.Text("Edit Profile")
//        }
//    }
//}
////preview
//@androidx.compose.ui.tooling.preview.Preview(uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL)
//@androidx.compose.runtime.Composable
//fun UserProfilePreview() {
//    UserProfile()
//}