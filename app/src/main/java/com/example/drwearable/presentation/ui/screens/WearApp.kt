package com.example.drwearable.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.example.drwearable.presentation.network.checkApiConnection
import com.example.drwearable.presentation.theme.DrWearableTheme
import com.example.drwearable.presentation.ui.components.Greeting
import kotlin.math.abs

@Composable
fun WearApp(greetingName: String) {
    var connectionsStatus by remember { mutableStateOf("Connecting...") }
    var pingColor by remember { mutableStateOf(Color.Gray) }
    var statusText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        checkApiConnection(
            pingColor = { newColor -> pingColor = newColor },
            connectionsStatus = { newStatus -> connectionsStatus = newStatus }
        )
    }

    DrWearableTheme {
        Scaffold(
            timeText = { TimeText() }
        ) {
            VerticalSwipeDetector (
                onSwipeUp = { statusText = "Accepted" },
                onSwipeDown = { statusText = "Denied" }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF570F82))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(pingColor)
                            .padding(start = 4.dp)
                    )

                    Greeting(greetingName = greetingName)

                    BasicText(
                        text = connectionsStatus,
                        modifier = Modifier.padding(top = 4.dp),
                        style = TextStyle(
                            color = Color.Red,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    )

                    BasicText(
                        text = statusText,
                        modifier = Modifier.padding(top = 4.dp),
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}

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



