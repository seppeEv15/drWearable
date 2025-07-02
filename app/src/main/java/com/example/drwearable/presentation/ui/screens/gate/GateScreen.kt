package com.example.drwearable.presentation.ui.screens.gate

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.font.FontWeight.Companion.W800
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.example.drwearable.R
import com.example.drwearable.presentation.theme.DrWearableTheme
import com.example.drwearable.presentation.theme.GreenGradient
import com.example.drwearable.presentation.theme.Grey
import com.example.drwearable.presentation.theme.PurpleGradient
import com.example.drwearable.presentation.theme.RedGradient
import com.example.drwearable.presentation.ui.AppViewModelProvider
import com.example.drwearable.presentation.ui.components.TimeTextWithSeconds
import com.example.drwearable.presentation.ui.components.VerticalSwipeDetector
import com.example.drwearable.presentation.ui.components.gate.PlayerImageOverlay
import com.example.drwearable.presentation.ui.components.gate.PlayerInfoContent
import kotlinx.coroutines.delay

@Composable
fun GateScreen(viewModel: GateViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val queue by viewModel.queue.collectAsState()

//    val swipeText by viewModel.swipeText.collectAsState()
    val isConnected by viewModel.isSseConnected.collectAsState()
    var showDisconnectedScreen by remember { mutableStateOf(false) }
    val borderState by viewModel.borderState.collectAsState()
//
//    var wasBlacklisted by remember { mutableStateOf(false) }
//    var lastPlayerBlacklistedImage by remember { mutableStateOf<Int?>(null) }
//
    val borderBrush = when (borderState) {
        BorderState.Green -> GreenGradient
        BorderState.Neutral -> PurpleGradient
        BorderState.Red -> RedGradient
    }

    LaunchedEffect(isConnected) {
        if  (!isConnected) {
            delay(5000) // wait 5 seconds
            if (!isConnected) {
                showDisconnectedScreen = true
            }
        } else {
            showDisconnectedScreen = false
        }
    }

    DrWearableTheme {
        Scaffold(
            timeText = {
                TimeTextWithSeconds(isConnected)
            }
        ) {
            if (showDisconnectedScreen) {
                DisconnectedScreen()
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(
                            width = 3.dp,
                            brush = borderBrush,
                            shape = CircleShape
                        )
                ) {
                    VerticalSwipeDetector(
                        onSwipeUp = {
                            if (queue.isNotEmpty()) {
//                                    wasBlacklisted = true
//                                    lastPlayerBlacklistedImage = R.drawable.new_user_block
                                if (queue[0].player.isBlacklisted) {
                                    viewModel.setStatusDenied()
                                } else {
                                    viewModel.setStatusAccepted()
                                }
//                                    wasBlacklisted = false
//                                    lastPlayerBlacklistedImage = null
                            }
                        },
                        onSwipeDown = {
                            if (queue.isNotEmpty()) {
                                viewModel.setStatusDenied()
                            }
                        }
                    ) {
                        if (queue.isNotEmpty()) {
                            val currentPlayer = queue[0].player
                            val fullName = currentPlayer.firstName + " " + currentPlayer.lastName
                            PlayerImageOverlay(imageBitmap = currentPlayer.image.asImageBitmap(), isBlacklisted = currentPlayer.isBlacklisted)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x00000000))
                                    .padding(2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    BasicText(
                                        text = fullName,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 40.dp),
                                        style = TextStyle(
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Center,
                                            fontFamily = FontFamily.SansSerif
                                        )
                                    )
                                    if (currentPlayer.isBlacklisted) {
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 20.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.warning_svgrepo_com),
                                                contentDescription = "Blacklist Icon",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.size(4.dp))
                                            Text(
                                                text = "Blacklisted",
                                                style = TextStyle(
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = Bold,
                                                    textAlign = TextAlign.Center,
                                                    fontFamily = FontFamily.SansSerif
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x00000000))
                                    .padding(2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Spacer(modifier = Modifier.height(25.dp))
                                Image(
                                    painter = painterResource(
                                        id = R.drawable.account_user_default
                                    ),
                                    contentDescription = "Player silhouette",
                                    modifier = Modifier.size(60.dp)
                                )
                                BasicText(
                                    text = "No player waiting",
                                    modifier = Modifier.padding(top = 4.dp),
                                    style = TextStyle(
                                        color = Grey,
                                        fontSize = 16.sp,
                                        fontWeight = W800,
                                        textAlign = TextAlign.Center
                                    )
                                )
                                Spacer(modifier = Modifier.height(25.dp))
                                Image(
                                    painter = painterResource(id = R.drawable.drgt),
                                    contentDescription = "drgt logo",
                                    modifier = Modifier.size(width = 50.dp, height = 20.dp)
                                )
                            }
                        }
                    }
                }
            }
//                        Box {
//                            PlayerImageOverlay(imageBitmap, player?.isBlacklisted == true)
//                            PlayerInfoContent(player, fullName, swipeText, wasBlacklisted, lastPlayerBlacklistedImage)
//                        }
        }
    }
}
