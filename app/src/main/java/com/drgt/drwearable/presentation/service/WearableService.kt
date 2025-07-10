package com.drgt.drwearable.presentation.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.drgt.drwearable.R
import com.drgt.drwearable.presentation.notifications.NotificationHelper
import okhttp3.OkHttpClient
import androidx.core.net.toUri
import com.drgt.drwearable.BuildConfig
import com.drgt.drwearable.presentation.data.WaggledanceRepository
import com.drgt.drwearable.presentation.network.SseClient
import com.drgt.drwearable.presentation.network.WaggleDanceApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WearableService : Service() {
    private val client = OkHttpClient()
    private val baseUrl = BuildConfig.BASE_URL
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var wifiLock: WifiManager.WifiLock
    private var sseClient: SseClient? = null
    private val repository: WaggledanceRepository by lazy {
        WaggledanceRepository(service = WaggleDanceApi.service)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        acquireWakeLock()
        acquireWifiLock()
        startForegroundService()
        initSessionAndConnect()
        requestIgnoreBatteryOptimization()
        startConnectionMonitor()
        return START_STICKY
    }

    private fun startConnectionMonitor() {
        Log.d("WearableService", "startConnectionMonitor invoked")
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                if (sseClient == null || !repository.isSseConnected.value) {
                    Log.w("WearableService", "SSE disconnected, attempting to reconnect...")
                    initSessionAndConnect()
                } else {
                    Log.d("WearableService", "SSE is connected")
                }
                delay(10_000) // Check every 10 seconds
            }
        }
    }

    private fun startForegroundService() {
        NotificationHelper.createNotificationChannel(this)

        val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setContentText("DrWearable Service Running")
            .setContentText("Monitoring player gate access...")
            .setSmallIcon(R.drawable.watch_icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    private fun initSessionAndConnect() {
        CoroutineScope(Dispatchers.IO).launch {
            val repository = WaggledanceRepository(service = WaggleDanceApi.service)
            val result = repository.getSessionId()

            if (result.isSuccess) {
                val sessionId = result.getOrNull()
                if (sessionId != null) {
                    connectToSseServerWithSession(sessionId)
                } else {
                    Log.e("WearableService", "Session ID is null")
                }
            } else {
                Log.e("WearableService", "Failed to get session ID", result.exceptionOrNull())
            }
        }
    }

    private fun connectToSseServerWithSession(sessionId: String) {
        sseClient = SseClient(
            sessionId = sessionId,
            apiUrl = baseUrl,
            onMessage = { message ->
                Log.d("WearableService", "Received message: $message")
                handleGateMessage(message)
            },
            onOpen = {
                Log.d("WearableService", "SSE connection opened")
            },
            onError = { error ->
                Log.e("WearableService", "SSE error", error)
            }
        )
        sseClient?.start()
    }

    private fun handleGateMessage(message: String) {
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val repository = WaggledanceRepository(service = WaggleDanceApi.service)
                val payload = repository.getPayload(message)
                val gateState = payload?.get("state")?.asString.orEmpty()
                val gatePosition = payload?.get("position")?.asString.orEmpty()

                if (gateState == "ACCESS_GRANTED" || gateState == "ACCESS_DENIED") {
                    val notificationText = "Gate at position $gatePosition: $gateState"
                    sendGateNotification(notificationText)
                }
            } catch (e: Exception) {
                Log.e("WearableService", "Failed to handle gate message", e)
            }
        } else {
            Log.w("WearableService", "Permission POST_NOTIFICATIONS not granted")
        }
    }

    //  TODO: implement logic from GateViewModel.kt
    private fun sendGateNotification(notificationText: String) {
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            val builder = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.watch_icon)
                .setContentTitle("Gate Update")
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), builder.build())
        } else {
            Log.w("WearableService", "Permission POST_NOTIFICATIONS not granted")
        }
    }

    private fun acquireWifiLock() {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "WearableService::WifiLock")
        if (!wifiLock.isHeld) {
            wifiLock.acquire()
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WearableService::WakeLock")
        if (!wakeLock.isHeld) {
            wakeLock.acquire(8*60*60*1000L /*8 hours*/)
        }
    }

    @SuppressLint("BatteryLife", "WearRecents")
    private fun requestIgnoreBatteryOptimization() {
        val packageName = packageName
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData("package:$packageName".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        if(::wifiLock.isInitialized && wifiLock.isHeld) {
            wifiLock.release()
        }

        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }
        client.dispatcher.executorService.shutdown()
    }
}

