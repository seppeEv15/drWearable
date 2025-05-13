package com.example.drwearable.presentation.ui.screens.gate
import android.util.Log
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText

import com.example.drwearable.presentation.theme.DrWearableTheme
import com.example.drwearable.presentation.ui.AppViewModelProvider
import com.example.drwearable.presentation.ui.components.VerticalSwipeDetector

@Composable
fun GateScreen(viewModel: GateViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val swipeText by viewModel.swipeText.collectAsState()
    val gateResponse by viewModel.gateResponse.collectAsState()
    val currentPlayer by viewModel.currentPlayer.collectAsState()
    val isConnected by viewModel.isSseConnected.collectAsState()
    val gateText = "GATE" + if (isConnected) " ✅" else " ❌"

    val player = currentPlayer?.player

    val fullName = listOfNotNull(
        player?.firstName,
        player?.secondName,
        player?.lastName,
        player?.lastName2
    ).joinToString(" ")

    val playerImage = player?.image

    DrWearableTheme {

        LaunchedEffect(isConnected) {
            Log.d("GateScreen", "SSE Connection status: $isConnected")
        }

        Scaffold(
            timeText = { TimeText() }
        ) {

            VerticalSwipeDetector (
                onSwipeUp = { viewModel.setStatusAccepted() },
                onSwipeDown = { viewModel.setStatusDenied() }
            ) {
                Box {
                    playerImage?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Background Player Image",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x00000000))
                            .padding(2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {

                        BasicText(
                            text = gateText,
                            modifier = Modifier.padding(top = 4.dp),
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        )

                        BasicText(
                            text = "Player: $fullName",
                            modifier = Modifier.padding(top = 4.dp),
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        )

//                    BasicText(
//                        text = "GATE: position: ${gateResponse?.position ?: ""}, state: ${gateResponse?.state ?: ""}",
//                        modifier = Modifier.padding(top = 4.dp),
//                        style = TextStyle(
//                            color = Color.White,
//                            fontSize = 12.sp,
//                            textAlign = TextAlign.Center
//                        )
//                    )

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
}
