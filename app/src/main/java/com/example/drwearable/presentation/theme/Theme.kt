package com.example.drwearable.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

@Composable
fun DrWearableTheme(
    colors: Colors = DrWearableColorPalette,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = colors,
        content = content
    )
}