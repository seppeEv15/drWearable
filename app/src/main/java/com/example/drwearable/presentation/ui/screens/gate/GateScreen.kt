package com.example.drwearable.presentation.ui.screens.gate

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Scaffold
import com.example.drwearable.R
import com.example.drwearable.presentation.theme.DrWearableTheme
import com.example.drwearable.presentation.theme.GreenGradient
import com.example.drwearable.presentation.theme.PurpleGradient
import com.example.drwearable.presentation.theme.RedGradient
import com.example.drwearable.presentation.ui.AppViewModelProvider
import com.example.drwearable.presentation.ui.components.gate.PlayerImageOverlay
import com.example.drwearable.presentation.ui.components.TimeTextWithSeconds
import com.example.drwearable.presentation.ui.components.VerticalSwipeDetector
import com.example.drwearable.presentation.ui.components.gate.PlayerInfoContent
import kotlinx.coroutines.delay

@Composable
fun GateScreen(viewModel: GateViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val swipeText by viewModel.swipeText.collectAsState()
    val currentPlayer by viewModel.currentPlayer.collectAsState()
    val isConnected by viewModel.isSseConnected.collectAsState()
    var showDisconnectedScreen by remember { mutableStateOf(false) }
    val entryState by viewModel.borderState.collectAsState()

    var wasBlacklisted by remember { mutableStateOf(false) }
    var lastPlayerBlacklistedImage by remember { mutableStateOf<Int?>(null) }

    val borderBrush = when (entryState) {
        BorderState.Accepted -> GreenGradient
        BorderState.Neutral -> PurpleGradient
        BorderState.Denied -> RedGradient
    }

    val player = currentPlayer?.player

    val imageBitmap = remember(player?.image) {
        player?.image?.asImageBitmap()
    }

    val fullName = remember(player) {
        listOfNotNull(
            player?.firstName,
            player?.secondName,
            player?.lastName,
            player?.lastName2
        ).joinToString(" ")
    }

    LaunchedEffect(isConnected) {
        Log.d("GateScreen", "SSE Connection status: $isConnected")
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
            if (!showDisconnectedScreen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(
                            width = 3.dp,
                            brush = if (player?.isBlacklisted == true) RedGradient else borderBrush,
                            shape = CircleShape
                        )
                ) {
                    VerticalSwipeDetector (
                        onSwipeUp = {
                            currentPlayer?.let { cp ->
                                if (cp.player.isBlacklisted) {
                                    viewModel.setStatusDenied()
                                    wasBlacklisted = true
                                    lastPlayerBlacklistedImage = R.drawable.new_user_block
                                } else {
                                    viewModel.setStatusAccepted()
                                    wasBlacklisted = false
                                    lastPlayerBlacklistedImage = null
                                }
                            }
                        },
                        onSwipeDown = {
                            currentPlayer?.let { cp ->
                                viewModel.setStatusDenied()
                                wasBlacklisted = currentPlayer?.player?.isBlacklisted == true
                                if (wasBlacklisted) lastPlayerBlacklistedImage = R.drawable.new_user_block
                            }
                        }
                    ) {
                        Box {
                            PlayerImageOverlay(imageBitmap, player?.isBlacklisted == true)
                            PlayerInfoContent(player, fullName, swipeText, wasBlacklisted, lastPlayerBlacklistedImage)
                        }
                    }
                }
            } else {
                DisconnectedScreen()
            }
        }
    }
}
