//package com.uoa.driverprofile.presentation.ui
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.AlertDialogDefaults.containerColor
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.LocalTextStyle
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.material3.TextField
//import androidx.compose.material3.TextFieldDefaults
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.input.TextFieldValue
//import androidx.compose.ui.text.input.VisualTransformation
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun CustomTextField(
//    value: TextFieldValue,
//    onValueChange: (TextFieldValue) -> Unit,
//    label: String,
//    isError: Boolean,
//    errorMessage: String,
//    modifier: Modifier = Modifier,
//    visualTransformation: VisualTransformation = VisualTransformation.None
//) {
//    Column(modifier = modifier) {
//        TextField(
//            value = value,
//            onValueChange = onValueChange,
//            modifier = Modifier
//                .fillMaxWidth()
//                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)),
//            label = {
//                Text(text = label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
//            },
//            textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
//            isError = isError,
//            visualTransformation = visualTransformation,
//            singleLine = true,
//            colors=TextFieldDefaults.colors(
//                focusedIndicatorColor = Color.Blue,
//                unfocusedIndicatorColor = Color.Transparent,
//                errorIndicatorColor = Color.Red,
//                errorCursorColor = Color.Red,
//                focusedContainerColor = containerColor,
//            )
//        )
//        if (isError) {
//            Text(
//                text = errorMessage,
//                color = MaterialTheme.colorScheme.error,
//                style = LocalTextStyle.current.copy(fontSize = 14.sp),
//                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
//            )
//        }
//    }
//}