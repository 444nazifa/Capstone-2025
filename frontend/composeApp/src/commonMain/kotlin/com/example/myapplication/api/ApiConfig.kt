package com.example.myapplication.api

object ApiConfig {
    // Python backend for QR scanning and NDC lookup
    // Note: Use 10.0.2.2 for Android emulator to reach host machine's localhost
    const val PYTHON_BACKEND_URL = "http://10.0.2.2:3002"

    // TypeScript backend for medication management
    const val MEDICATION_BACKEND_URL = "https://backend-ts-theta.vercel.app"
}
