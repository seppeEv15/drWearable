package com.example.drwearable.presentation.ui.screens
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.example.drwearable.R
import com.example.drwearable.presentation.network.SseClient
import com.example.drwearable.presentation.theme.DrWearableTheme
import com.example.drwearable.presentation.ui.components.StartButton
import com.example.drwearable.presentation.ui.components.VerticalSwipeDetector
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Wearable app entry point.
 */
@Composable
fun WearApp(greetingName: String) {
    var playerData = JsonObject()
    var firstName by remember { mutableStateOf("") }
    var connectionsStatus by remember { mutableStateOf("Connecting...") }
    var pingColor by remember { mutableStateOf(Color.Gray) }
    var statusText by remember { mutableStateOf("") }
    var sessionId by remember { mutableStateOf<String?>(null) }
    val lastIsAliveTime = remember { mutableLongStateOf(System.currentTimeMillis()) }
    var sseClient by remember { mutableStateOf<SseClient?>(null) }

    val scope = rememberCoroutineScope()

    // TODO: move filters and logic to repository? or correct viewModel
    // Handle SSE connection
    LaunchedEffect(sessionId) {
        sessionId?.let { id ->
            sseClient?.stop() // Stop previous connection
            sseClient = SseClient(
                sessionId = id,
                apiUrl = "http://10.129.10.42:5050", // your BASE_URL

                onMessage = { message ->
                    if (message.contains("drMemberCPPlayerData")) {
                        try {
                            val jsonParser = JsonParser()
                            val jsonObject = jsonParser.parse(message).asJsonObject
                            println(jsonObject)
                            println(jsonObject::class.simpleName)

                            playerData = jsonObject
                            val payloadElement = playerData.get("payload")

                            if (payloadElement != null && payloadElement.isJsonObject) {
                                val payload = payloadElement.asJsonObject
                                val playerElement = payload.get("player")

                                if (playerElement != null && playerElement.isJsonObject) {
                                    val player = playerElement.asJsonObject
                                    firstName = player.get("firstName")?.toString() ?: "Unknown"
                                } else {
                                    Log.e("SSE", "Missing 'player' field in payload")
                                }
                            } else {
                                Log.e("SSE", "Missing 'payload' field in message")
                            }
                        } catch (e: Exception) {
                            Log.e("SSE", "Exception while parsing message: ${e.message}")
                        }
                    }

                    if (message.contains("test")) {
                        lastIsAliveTime.value = System.currentTimeMillis()
                    }

                    val ignoredCmds = listOf("currentTime", "sync", "AreYouThere", "test")
                    if (!ignoredCmds.any { message.contains(it) }) {
                        Log.d("SSE", "Received message: $message")
                    }
                },

                onOpen = {
                    Log.d("SSE", "Connection opened")
                    connectionsStatus = "Connected"
                },
                onError = { error ->
                    Log.e("SSE", "Connection error", error)
                    connectionsStatus = "Disconnected"
                    pingColor = Color.Red

                    scope.launch {
                        while (connectionsStatus != "Connected") {
                            try {
                                sseClient?.stop()
                                delay(5000)
                                sseClient?.start()
                                Log.d("SSE_RETRY", "Retrying SSE connection...")
                            } catch (e: Exception) {
                                Log.e("SSE_RETRY", "Retry failed: ${e.localizedMessage}")
                            }
                            delay(5000)
                        }
                    }
                }
            )
            sseClient?.start()
        }
    }

    //TODO: Move to repository or viewModel of gateScreen
    LaunchedEffect(Unit) {
        while (true) {
            val timeSinceLastPing = System.currentTimeMillis() - lastIsAliveTime.value
            if (timeSinceLastPing > 10000 && connectionsStatus != "Disconnected") {
                connectionsStatus = "Disconnected"
                pingColor = Color.Red
            } else if (timeSinceLastPing <= 10000 && connectionsStatus != "Connected") {
                connectionsStatus = "Connected"
                pingColor = Color.Green
            }
            delay(1000)
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
                        .background(Color(0x00000000))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
//                    Box(
//                        modifier = Modifier
//                            .size(10.dp)
//                            .clip(CircleShape)
//                            .background(pingColor)
//                            .padding(start = 4.dp)
//                    )

                    Image(
                        modifier = Modifier
                            .width(80.dp)
                            .height(80.dp)
                            .padding(10.dp),
                        painter = painterResource(id = R.drawable.bwinoostende),
                        contentDescription = "Casino logo"
                    )

                    StartButton()

                    // Display error message if available
                    BasicText(
                        text = statusText,
                        modifier = Modifier.padding(top = 4.dp),
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    )

/*                    BasicText(
                        text = if (connectionsStatus == "Connected") "✅ Connected to SSE" else "❌ SSE Not Connected",
                        modifier = Modifier.padding(top = 4.dp),
                        style = TextStyle(
                            color = if (connectionsStatus == "Connected") Color.Green else Color.Red,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    )*/
                }
            }
        }
    }
}
