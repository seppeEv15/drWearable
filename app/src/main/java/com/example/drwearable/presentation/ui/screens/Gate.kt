package com.example.drwearable.presentation.ui.screens

import android.R.attr.bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import com.example.drwearable.R
import com.example.drwearable.presentation.theme.DrWearableTheme
import com.example.drwearable.presentation.ui.components.VerticalSwipeDetector


@Composable
fun Gate () {

    val pingColor by remember { mutableStateOf(Color.Gray) }
    var statusText by remember { mutableStateOf("") }

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
                        .background(Color(0x00000000))
                        .padding(2.dp),
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

                    BasicText(
                        text = "GATE",
                        modifier = Modifier.padding(top = 4.dp),
                        style = TextStyle(
                            color = Color.White,
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
