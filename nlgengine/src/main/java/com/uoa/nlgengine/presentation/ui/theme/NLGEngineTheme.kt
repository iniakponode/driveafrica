package com.uoa.nlgengine.presentation.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val NLGEngineColorScheme = lightColorScheme(
    primary = Color(0xFF003366), // Deep blue
    onPrimary = Color.White, // White text on primary color
    secondary = Color(0xFF6699CC), // Soft blue
    onSecondary = Color(0xFF333333), // Dark grey text on secondary color
    background = Color(0xFFF2F2F2), // Light grey background
    onBackground = Color.Black, // Black text on background
    error = Color(0xFFB00020), // Red for errors
    onError = Color.White // White text on error
)

// Define typography for headings, subheadings, and body text
private val NLGEngineTypography = Typography(
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 0.15.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun NLGEngineTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NLGEngineColorScheme,
        typography = NLGEngineTypography,
        content = content
    )
}
