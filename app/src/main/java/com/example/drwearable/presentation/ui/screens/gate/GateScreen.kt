package com.example.drwearable.presentation.ui.screens.gate
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight.Companion.W800
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.example.drwearable.R
import com.example.drwearable.presentation.theme.Black
import com.example.drwearable.presentation.theme.BlackCustomGradient
import com.example.drwearable.presentation.theme.DrWearableTheme
import com.example.drwearable.presentation.theme.GreenGradient
import com.example.drwearable.presentation.theme.Grey
import com.example.drwearable.presentation.theme.GreyGradient
import com.example.drwearable.presentation.theme.PurpleGradient
import com.example.drwearable.presentation.theme.RedGradient
import com.example.drwearable.presentation.theme.WhiteGradient
import com.example.drwearable.presentation.ui.AppViewModelProvider
import com.example.drwearable.presentation.ui.components.VerticalSwipeDetector
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun GateScreen(viewModel: GateViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val swipeText by viewModel.swipeText.collectAsState()
    val currentPlayer by viewModel.currentPlayer.collectAsState()
    val isConnected by viewModel.isSseConnected.collectAsState()
    var showDisconnectedScreen by remember { mutableStateOf(false) }
    val entryState by viewModel.borderState.collectAsState()

    val borderBrush = when (entryState) {
        BorderState.Accepted -> GreenGradient
        BorderState.Neutral -> PurpleGradient
        BorderState.Denied -> RedGradient
    }

    val swipeTextColor = when {
        swipeText.contains("accepted", ignoreCase = true) -> GreenGradient
        swipeText.contains("denied", ignoreCase = true) -> RedGradient
        else -> WhiteGradient
    }

    val player = currentPlayer?.player

    val fullName = listOfNotNull(
        player?.firstName,
        player?.secondName,
        player?.lastName,
        player?.lastName2
    ).joinToString(" ")

    val playerImage = player?.image

    LaunchedEffect(isConnected) {
        if  (!isConnected) {
            delay(3000) // wait 3 seconds
            if (!isConnected) {
                showDisconnectedScreen = true
            }
        } else {
            showDisconnectedScreen = false
        }
    }

    DrWearableTheme {
        LaunchedEffect(isConnected) {
            Log.d("GateScreen", "SSE Connection status: $isConnected")
        }

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
                            brush = borderBrush,
                            shape = CircleShape
                        )
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
                                        .fillMaxSize()
                                        .align(Alignment.Center)
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .matchParentSize()
                                        .background(BlackCustomGradient)
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x00000000))
                                    .padding(2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                if (player == null || fullName.isBlank()) {
                                    val useRedImage = swipeText.lowercase().contains("denied")
                                    val useGreenImage = swipeText.lowercase().contains("accepted")
                                    Spacer(modifier = Modifier.height(25.dp))
                                    if (swipeText.isNotBlank()) {
                                        Image(
                                            painter = painterResource(
                                                id = if (useGreenImage) {
                                                    R.drawable.account_user_green
                                                } else if (useRedImage) {
                                                    R.drawable.account_user_red
                                                } else
                                                R.drawable.account_user_default
                                            ),
                                            contentDescription = "Player silhouette",
                                            modifier = Modifier
                                                .size(60.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            BasicText(
                                                text = swipeText,
                                                style = TextStyle(
                                                    brush = swipeTextColor,
                                                    fontSize = 16.sp,
                                                    fontWeight = W800,
                                                    textAlign = TextAlign.Center
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    } else {
                                        Image(
                                            painter = painterResource(id = R.drawable.account_user_default),
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
                                    }
                                    Spacer(modifier = Modifier.height(25.dp))
                                    Image(
                                        painter = painterResource(id = R.drawable.drgt),
                                        contentDescription = "drgt logo",
                                        modifier = Modifier.size(width = 50.dp, height = 20.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                    ) {
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
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
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
        }
    }
}

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