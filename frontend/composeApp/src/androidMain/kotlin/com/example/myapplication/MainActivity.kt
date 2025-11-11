package com.example.myapplication

import android.content.Intent
import android.net.Uri
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

        // Handle deep links
        val deepLinkData = handleDeepLink(intent)

        setContent {
            AppWithPushNotifications(
                pushManager = pushNotificationManager,
                initialRoute = deepLinkData.first,
                resetToken = deepLinkData.second
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Handle deep link when app is already running
        val deepLinkData = handleDeepLink(intent)

        // Recreate the activity to navigate to the reset password screen
        if (deepLinkData.first == "resetPassword") {
            recreate()
        }
    }

    private fun handleDeepLink(intent: Intent): Pair<String?, String?> {
        val data: Uri? = intent.data

        // Check if it's a password reset deep link
        // Supabase sends: carecapsule://reset-password#access_token=xxx&...
        // or: carecapsule://reset-password?token=xxx (for testing)
        if (data?.scheme == "carecapsule" && data.host == "reset-password") {
            // First try to get from query parameter (for testing/manual links)
            var token = data.getQueryParameter("token")

            // If not found, try to extract from fragment (Supabase format)
            if (token == null) {
                val fragment = data.fragment
                if (fragment != null) {
                    // Parse fragment: access_token=xxx&type=recovery&...
                    val params = fragment.split("&")
                    for (param in params) {
                        val keyValue = param.split("=")
                        if (keyValue.size == 2 && keyValue[0] == "access_token") {
                            token = keyValue[1]
                            break
                        }
                    }
                }
            }

            if (token != null) {
                return Pair("resetPassword", token)
            }
        }

        return Pair(null, null)
    }
}

@Composable
fun AppWithPushNotifications(
    pushManager: PushNotificationManager,
    initialRoute: String? = null,
    resetToken: String? = null
) {
    App(
        onEnableNotifications = { pushManager.requestPermissionAndRegister() },
        onDisableNotifications = { pushManager.disablePushNotifications() },
        isNotificationsEnabled = { pushManager.isTokenRegistered() },
        initialRoute = initialRoute,
        resetToken = resetToken
    )
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}