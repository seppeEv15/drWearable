package com.example.drwearable.presentation.ui.screens.gate
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight.Companion.W800
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.example.drwearable.R
import com.example.drwearable.presentation.theme.BlackCustomGradient
import com.example.drwearable.presentation.theme.DrWearableTheme
import com.example.drwearable.presentation.theme.GreenGradient
import com.example.drwearable.presentation.theme.PurpleGradient
import com.example.drwearable.presentation.theme.RedGradient
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
    val borderState by viewModel.borderState.collectAsState()

    val borderBrush = when (borderState) {
        BorderState.Accepted -> GreenGradient
        BorderState.Neutral -> PurpleGradient
        BorderState.Denied -> RedGradient
    }

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
            timeText = {
                TimeTextWithSeconds(isConnected)
            }
        ) {
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
                            verticalArrangement = Arrangement.Center
                        ) {

                            if (player == null || fullName.isBlank()) {
                                Image(
                                    painter = painterResource(id = R.drawable.account_user_group_people_person_profile_svgrepo_com),
                                    contentDescription = "No player waiting",
                                    modifier = Modifier.size(60.dp)
                                )
                                BasicText(
                                    text = "No player waiting",
                                    modifier = Modifier.padding(top = 4.dp),
                                    style = TextStyle(
                                        color = Color(0xFF737373),
                                        fontSize = 16.sp,
                                        fontWeight = W800,
                                        textAlign = TextAlign.Center
                                    )
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
                                            .padding(bottom = 30.dp),
                                        style = TextStyle(
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Center,
                                            fontFamily = FontFamily.SansSerif
                                        )
                                    )
                                }
                            }

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
