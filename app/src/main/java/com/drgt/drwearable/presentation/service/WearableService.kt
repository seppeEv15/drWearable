package com.drgt.drwearable.presentation.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

class WearableService : Service() {
    private val client = OkHttpClient()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        connectToSSeServer()
        return START_STICKY
    }

    private fun startForegroundService() {
        // Implement notification setup for foreground service
    }

    private fun connectToSSeServer() {
        val request = Request.Builder()
            .url("https://example.com/sse") // Replace with your SSE server URL
            .build()

        val eventSourceFactory = EventSources.createFactory(client)

        eventSourceFactory.newEventSource(request, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                Log.d("WearableService", "Received SSE message: $data")
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                Log.e("WearableService", "SSE connection failed", t)
            }

            override fun onClosed(eventSource: EventSource) {
                Log.d("WearableService", "SSE connection closed")
            }
        })
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        client.dispatcher.executorService.shutdown()
    }
}

