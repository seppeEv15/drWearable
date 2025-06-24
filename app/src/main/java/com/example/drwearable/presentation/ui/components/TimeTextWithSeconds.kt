package com.example.drwearable.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.example.drwearable.presentation.theme.GreenGradient
import com.example.drwearable.presentation.theme.RedGradient
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun TimeTextWithSeconds(isConnected: Boolean) {
    val time = remember { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            time.value = LocalTime.now()
            delay(1000)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = time.value.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            style = TextStyle(fontSize = 12.sp)
        )
        Box(
            modifier = Modifier
                .padding(start = 6.dp)
                .size(6.dp)
                .background(
                    brush = if (isConnected) GreenGradient else RedGradient,
                    shape = CircleShape
                )
        )
    }
}