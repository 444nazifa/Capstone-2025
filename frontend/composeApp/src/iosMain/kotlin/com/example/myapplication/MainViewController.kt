package com.example.myapplication

import androidx.compose.ui.window.ComposeUIViewController
import com.example.myapplication.data.UserSession
import com.example.myapplication.storage.createSecureStorage

fun MainViewController() = ComposeUIViewController {
    // Initialize secure storage for iOS
    UserSession.initialize(createSecureStorage())
    App()
}