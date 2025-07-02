package com.example.drwearable.presentation.ui.screens.gate

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight.Companion.W800
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.CircularProgressIndicator
import com.example.drwearable.R
import com.example.drwearable.presentation.theme.Black
import com.example.drwearable.presentation.theme.Grey
import com.example.drwearable.presentation.theme.GreyGradient

@Composable
fun DisconnectedScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .border(
                width = 3.dp,
                brush = GreyGradient,
                shape = CircleShape
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x00000000))
                .padding(2.dp),

            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.disconnected_svgrepo_com),
                contentDescription = "No player waiting",
                modifier = Modifier.size(40.dp)
            )
            BasicText(
                text = "Disconnected",
                modifier = Modifier.padding(top = 4.dp),
                style = TextStyle(
                    color = Color(0xFF737373),
                    fontSize = 16.sp,
                    fontWeight = W800,
                    textAlign = TextAlign.Center
                )
            )
            CircularProgressIndicator(
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 6.dp),
                strokeWidth = 2.dp,
                trackColor = Black,
                indicatorColor = Grey

            )
        }
    }
}