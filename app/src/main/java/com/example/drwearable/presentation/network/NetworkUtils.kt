package com.example.drwearable.presentation.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import androidx.compose.ui.graphics.Color

fun checkApiConnection(
    pingColor: (Color) -> Unit,
    connectionsStatus: (String) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        while (true) {
            try {
                pingColor(Color.Green) // Ping active
                val response = Jsoup.connect(drMember4ApiLink).get()
                val title = response.title()

                connectionsStatus(if (title.isNotEmpty()) {
                    "Connected: $title"
                } else {
                    "Failed: No Title Found"
                })
            } catch (e: Exception) {
                connectionsStatus("Failed to connect: ${e.message}")
            }

            delay(500)
            pingColor(Color.Gray)
            delay(4500)
        }
    }
}

const val  drMember4ApiLink = "https://vp1.aalst.drgt.org.dr.gt/"