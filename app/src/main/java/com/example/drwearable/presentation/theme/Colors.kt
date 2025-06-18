package com.example.drwearable.presentation.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// COLORS
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)
val Grey = Color(0xFF737373)
val RedGradientStart = Color(0xFFF9240C)
val RedGradientEnd = Color(0xFFB70024)
val GreenGradientStart = Color(0xFF45FA4D)
val GreenGradientEnd = Color(0xFF019B54)

// GRADIENTS
val RedGradient = Brush.linearGradient(
    colors = listOf(RedGradientStart, RedGradientEnd),
    start = Offset(0f, 0f),
    end = Offset(0f, Float.POSITIVE_INFINITY)
)

val GreenGradient = Brush.linearGradient(
    colors = listOf(GreenGradientStart, GreenGradientEnd),
    start = Offset(0f, 0f),
    end = Offset(0f, Float.POSITIVE_INFINITY)
)

val PurpleGradient = Brush.linearGradient(
    colorStops = arrayOf(
        0.0f to DrgtPurple,
        0.75f to Black
    ),
    start = Offset(0f, 0f),
    end = Offset(0f, Float.POSITIVE_INFINITY)
)

val GreyGradient = Brush.linearGradient(
    colorStops = arrayOf(
        0.0f to Grey,
        0.75f to Black
    ),
    start = Offset(0f, 0f),
    end = Offset(0f, Float.POSITIVE_INFINITY)
)

val BlackCustomGradient = Brush.linearGradient(
    0.0f to Color(0x00000000),       // Transparent black at 0%
    0.51f to Color(0x55000000),      // Black with ~33% alpha at 51%
    0.84f to Color(0xFF000000),      // Fully opaque black at 84%
    start = Offset(0f, 0f),
    end = Offset(0f, Float.POSITIVE_INFINITY)
)

val WhiteGradient = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.98f),
        Color.White.copy(alpha = 0.90f)
    ),
    start = Offset(0f, 0f),
    end = Offset(0f, Float.POSITIVE_INFINITY)
)