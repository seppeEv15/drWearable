package com.example.drwearable.presentation.ui.screens.gate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText

import com.example.drwearable.presentation.theme.DrWearableTheme
import com.example.drwearable.presentation.ui.AppViewModelProvider
import com.example.drwearable.presentation.ui.components.VerticalSwipeDetector

@Composable
fun GateScreen(viewModel: GateViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val swipeText by viewModel.swipeText.collectAsState()
    val gateResponse by viewModel.gateResponse.collectAsState()
    val currentPlayer by viewModel.currentPlayer.collectAsState()
    //val errorMessage by viewModel.errorMessage.collectAsState()
    //val pingColor by viewModel.pingColor.collectAsState()

    DrWearableTheme {
        Scaffold(
            timeText = { TimeText() }
        ) {

            VerticalSwipeDetector (
                onSwipeUp = { viewModel.setStatusAccepted() },
                onSwipeDown = { viewModel.setStatusDenied() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x00000000))
                        .padding(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
//                    Box(
//                        modifier = Modifier
//                            .size(10.dp)
//                            .clip(CircleShape)
//                            .background(pingColor.value)
//                            .padding(start = 4.dp)
//                    )

                    BasicText(
                        text = "GATE",
                        modifier = Modifier.padding(top = 4.dp),
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    )

                    BasicText(
                        text = "Player: ${currentPlayer?.player?.firstName ?: ""} ${currentPlayer?.player?.secondName ?: ""} ${currentPlayer?.player?.lastName ?: ""} ${currentPlayer?.player?.lastName2 ?: ""}",
                        modifier = Modifier.padding(top = 4.dp),
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    )

                    BasicText(
                        text = "GATE: position: ${gateResponse?.position ?: ""}, state: ${gateResponse?.state ?: ""}",
                        modifier = Modifier.padding(top = 4.dp),
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    )

                    BasicText(
                        text = swipeText,
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
