package com.example.drwearable.presentation.ui.components

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

@Composable
fun VerticalSwipeDetector(
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    content: @Composable () -> Unit
) {
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    var swipeDirection by remember { mutableStateOf<String?>(null) }

    val swipeThreshold = 30f  // Lower threshold for slower swipes
    val minSwipeDistance = 60f  // Minimum distance before swipe is triggered

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        // Trigger swipe if the distance exceeds the threshold
                        if (abs(swipeOffset) > minSwipeDistance) {
                            when (swipeDirection) {
                                "up" -> onSwipeUp()
                                "down" -> onSwipeDown()
                            }
                        }
                        // Reset swipe state
                        swipeOffset = 0f
                        swipeDirection = null
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        swipeOffset += dragAmount

                        // Adjust direction based on drag amount
                        swipeDirection = when {
                            swipeOffset < -swipeThreshold -> "up"
                            swipeOffset > swipeThreshold -> "down"
                            else -> null
                        }
                    }
                )
            }
    ) {
        content()
    }
}
