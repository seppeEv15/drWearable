package com.example.drwearable.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.MaterialTheme

val DrgtPurple = Color(0xFF570F82)

private val lightThemeColorPalette =  androidx.compose.material.lightColors(
    primary = DrgtPurple,
)

@Composable
fun DrWearableTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        content = content
    )
}