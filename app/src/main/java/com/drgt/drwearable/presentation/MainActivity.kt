package com.drgt.drwearable.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.Manifest
import com.drgt.drwearable.presentation.notifications.NotificationHelper
import com.drgt.drwearable.presentation.ui.DrWearableApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        NotificationHelper.createNotificationChannel(this)

        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(this, permission) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 1001)
        }

        setContent {
            DrWearableApp()
        }
    }
}

