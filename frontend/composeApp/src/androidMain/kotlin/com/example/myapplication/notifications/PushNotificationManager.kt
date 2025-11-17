package com.example.myapplication.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.myapplication.api.MedicationApiService
import com.example.myapplication.storage.createSecureStorage
import com.example.myapplication.storage.getToken
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PushNotificationManager private constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "PushNotificationManager"
        private const val PREFS_NAME = "push_notifications"
        private const val KEY_TOKEN_REGISTERED = "token_registered"
        private const val KEY_LAST_TOKEN = "last_token"

        @Volatile
        private var instance: PushNotificationManager? = null

        fun getInstance(context: Context): PushNotificationManager {
            return instance ?: synchronized(this) {
                instance ?: PushNotificationManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val secureStorage = createSecureStorage()
    private var permissionCallback: ((Boolean) -> Unit)? = null
    private var permissionLauncher: ActivityResultLauncher<String>? = null

    fun initializePermissionLauncher(activity: ComponentActivity) {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            Log.d(TAG, "Notification permission result: $isGranted")
            permissionCallback?.invoke(isGranted)
            permissionCallback = null
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // No runtime permission needed before Android 13
        }
    }

    suspend fun requestNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        if (hasNotificationPermission()) {
            return true
        }

        return suspendCoroutine { continuation ->
            permissionCallback = { isGranted ->
                continuation.resume(isGranted)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
                    ?: run {
                        Log.e(TAG, "Permission launcher not initialized! Call initializePermissionLauncher() first")
                        continuation.resume(false)
                    }
            }
        }
    }

    suspend fun getToken(): String? {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "FCM token retrieved: ${token.take(20)}...")
            token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FCM token", e)
            null
        }
    }

    suspend fun registerTokenWithBackend(token: String) {
        try {
            val authToken = secureStorage.getToken()
            if (authToken.isNullOrEmpty()) {
                throw Exception("Not authenticated - no auth token found")
            }

            val apiService = MedicationApiService.shared
            apiService.registerDeviceToken(authToken, token, "android")

            prefs.edit().apply {
                putBoolean(KEY_TOKEN_REGISTERED, true)
                putString(KEY_LAST_TOKEN, token)
                apply()
            }

            Log.d(TAG, "Device token registered with backend successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register token with backend", e)
            throw e
        }
    }

    suspend fun unregisterTokenFromBackend() {
        try {
            val authToken = secureStorage.getToken()
            if (authToken.isNullOrEmpty()) {
                throw Exception("Not authenticated - no auth token found")
            }

            val token = getToken() ?: return

            val apiService = MedicationApiService.shared
            apiService.unregisterDeviceToken(authToken, token)

            prefs.edit().apply {
                putBoolean(KEY_TOKEN_REGISTERED, false)
                remove(KEY_LAST_TOKEN)
                apply()
            }

            Log.d(TAG, "Device token unregistered from backend")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister token from backend", e)
            throw e
        }
    }

    fun isTokenRegistered(): Boolean {
        return prefs.getBoolean(KEY_TOKEN_REGISTERED, false)
    }

    suspend fun requestPermissionAndRegister(): Boolean {
        Log.d(TAG, "Requesting permission and registering for push notifications")

        // Step 1: Request notification permission
        val permissionGranted = requestNotificationPermission()
        if (!permissionGranted) {
            Log.w(TAG, "Notification permission denied")
            return false
        }

        // Step 2: Get FCM token
        val token = getToken()
        if (token == null) {
            Log.e(TAG, "Failed to get FCM token")
            return false
        }

        // Step 3: Register with backend
        return try {
            registerTokenWithBackend(token)
            Log.d(TAG, "Push notifications enabled successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable push notifications", e)
            false
        }
    }

    suspend fun disablePushNotifications(): Boolean {
        return try {
            unregisterTokenFromBackend()
            Log.d(TAG, "Push notifications disabled")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disable push notifications", e)
            false
        }
    }
}
