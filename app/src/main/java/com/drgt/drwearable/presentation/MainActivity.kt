package com.drgt.drwearable.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.Manifest
import android.content.Intent
import com.drgt.drwearable.presentation.notifications.NotificationHelper
import com.drgt.drwearable.presentation.ui.DrWearableApp
import android.provider.Settings
import com.drgt.drwearable.presentation.service.WearableService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        disableBatteryOptimization()
        NotificationHelper.createNotificationChannel(this)

        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(this, permission) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 1001)
        } else {
            startWearableService()
        }

        setContent {
            DrWearableApp()
        }
    }

    private fun disableBatteryOptimization() {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        startActivity(intent)
    }

    private fun startWearableService() {
        val intent = Intent(this, WearableService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}

