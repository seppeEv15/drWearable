package com.example.drwearable.presentation.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.example.drwearable.presentation.network.SessionIdResponse
import com.example.drwearable.presentation.network.WaggleDanceApi
import com.example.drwearable.presentation.network.checkApiConnection
import com.example.drwearable.presentation.theme.DrWearableTheme
import com.example.drwearable.presentation.ui.components.Greeting
import com.example.drwearable.presentation.ui.components.VerticalSwipeDetector
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import retrofit2.Response

@Composable
fun WearApp(greetingName: String) {
    var connectionsStatus by remember { mutableStateOf("Connecting...") }
    var pingColor by remember { mutableStateOf(Color.Gray) }
    var statusText by remember { mutableStateOf("") }
    var sessionId by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        checkApiConnection(
            pingColor = { newColor -> pingColor = newColor },
            connectionsStatus = { newStatus -> connectionsStatus = newStatus }
        )

        try {
            val response = testApiCall()
            if (response.isSuccessful) {
                sessionId = response.body()?.sessionId
            } else {
                errorMessage = "Error: ${response.code()} - ${response.message()}"
            }
        } catch (e: Exception) {
            errorMessage = "Network request failed: ${e.localizedMessage}"
        }
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

suspend fun testApiCall(): Response<SessionIdResponse> {
    val sessionBody = RequestBody.create(
        "application/json".toMediaTypeOrNull(),
        """{"cmd": "newSession"}"""
    )

    // Log the request data and response
    try {
        val response = WaggleDanceApi.service.getSessionId(sessionBody)
        Log.d("API_CALL", "Response: ${response.body()?.sessionId}")
        return response
    } catch (e: Exception) {
        Log.e("API_CALL", "Error getSessionId: ${e.localizedMessage}")
        throw e
    }
}