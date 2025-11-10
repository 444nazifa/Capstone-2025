package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.data.UserSession
import com.example.myapplication.notifications.PushNotificationManager
import com.example.myapplication.storage.createSecureStorage
import com.example.myapplication.storage.initSecureStorage

class MainActivity : ComponentActivity() {
    private lateinit var pushNotificationManager: PushNotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        initSecureStorage(applicationContext)
        UserSession.initialize(createSecureStorage())

        pushNotificationManager = PushNotificationManager.getInstance(this)
        pushNotificationManager.initializePermissionLauncher(this)

        setContent {
            AppWithPushNotifications(pushNotificationManager)
        }
    }
}

@Composable
fun AppWithPushNotifications(pushManager: PushNotificationManager) {
    App(
        onEnableNotifications = { pushManager.requestPermissionAndRegister() },
        onDisableNotifications = { pushManager.disablePushNotifications() },
        isNotificationsEnabled = { pushManager.isTokenRegistered() }
    )
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}