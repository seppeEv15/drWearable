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
import com.example.drwearable.presentation.network.SseClient
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

    var sseClient by remember { mutableStateOf<SseClient?>(null) }

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

    LaunchedEffect(sessionId) {
        sessionId?.let { id ->
            sseClient?.stop() // Stop previous connection
            sseClient = SseClient(
                sessionId = id,
                apiUrl = "http://10.129.100.80:5050", // your BASE_URL
                onMessage = { message ->
                    Log.d("SSE", "Received message: $message")
                    // Example: only update statusText if it's not a spammy type
                    if (!listOf("currentTime", "sync", "AreYouThere", "test").any { message.contains(it) }) {
                        statusText = message
                    }
                },
                onOpen = {
                    Log.d("SSE", "Connection opened")
                    connectionsStatus = "Connected"
                    pingColor = Color.Green
                },
                onError = { error ->
                    Log.e("SSE", "Connection error", error)
                    connectionsStatus = "Disconnected"
                    pingColor = Color.Red
                }
            )
            sseClient?.start()
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

                    BasicText(
                        text = if (connectionsStatus == "Connected") "✅ Connected to SSE" else "❌ SSE Not Connected",
                        modifier = Modifier.padding(top = 4.dp),
                        style = TextStyle(
                            color = if (connectionsStatus == "Connected") Color.Green else Color.Red,
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

    try {
        val response = WaggleDanceApi.service.getSessionId(sessionBody)
        Log.d("API_CALL", "Response: Session established. ID: ${response.body()?.sessionId}")
        return response
    } catch (e: Exception) {
        Log.e("API_CALL", "Error: Could not establish session: ${e.localizedMessage}")
        throw e
    }
}

//  Optional:  clean up the SSE client when the composable is disposed
//  DisposableEffect(Unit) {
//      onDispose {
//          sseClient?.stop()
//      }
//  }