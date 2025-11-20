package com.example.myapplication.api

import com.example.myapplication.BASE_URL

object ApiConfig {
    // Python backend for QR scanning and NDC lookup
    const val PYTHON_BACKEND_URL = "http://localhost:3002"

    // TypeScript backend for medication management
    val MEDICATION_BACKEND_URL: String = BASE_URL
}
