// ============================================
// androidMain/ScanMedicationScreen.android.kt
// ============================================
package com.example.myapplication

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

private var globalImageCapture: ImageCapture? = null

@Composable
actual fun CameraView(modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                globalImageCapture = imageCapture

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    Log.e("CameraView", "Camera binding failed", e)
                }
            }, executor)
            previewView
        },
        modifier = modifier
    )
}

actual suspend fun requestCameraPermission(): Boolean {
    // This will be called from a Composable context, so we can't directly access LocalContext
    // We need to check permission using the application context
    // For now, return true and let the Composable handle permission request
    return true
}

actual fun captureImage(onImageCaptured: (ByteArray) -> Unit) {
    globalImageCapture?.let { capture ->
        val executor = Executors.newSingleThreadExecutor()
        capture.takePicture(
            executor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                    val byteArray = stream.toByteArray()
                    image.close()
                    onImageCaptured(byteArray)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("Camera", "Capture failed", exception)
                }
            }
        )
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

actual suspend fun sendImageToBackend(imageData: ByteArray): String? = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()
        val url = "${com.example.myapplication.api.ApiConfig.PYTHON_BACKEND_URL}/api/scan-qr"

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "image",
                "medication.jpg",
                imageData.toRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            val responseBody = response.body?.string()
            Log.d("API", "Scan QR Response: $responseBody")

            responseBody?.let {
                if (it.contains("\"qr_detected\":true") || it.contains("\"qr_detected\": true")) {
                    val ndcRegex = Regex("\"ndc\"\\s*:\\s*\"([^\"]+)\"")
                    val match = ndcRegex.find(it)
                    match?.groupValues?.get(1) ?: "QR code detected"
                } else {
                    null
                }
            }
        } else {
            Log.e("API", "Image upload failed: ${response.code}")
            null
        }
    } catch (e: Exception) {
        Log.e("API", "Image upload failed", e)
        null
    }
}

actual suspend fun searchMedicationsByNDC(ndc: String): com.example.myapplication.data.MedicationSearchResponse? = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()
        val url = "${com.example.myapplication.api.ApiConfig.MEDICATION_BACKEND_URL}/api/medication/search/ndc?ndc=$ndc"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        Log.d("API", "Sending NDC search request to URL: $url")
        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            val responseBody = response.body?.string()
            Log.d("API", "NDC Search Response: $responseBody")

            responseBody?.let { body ->
                try {
                    val json = kotlinx.serialization.json.Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    }
                    json.decodeFromString<com.example.myapplication.data.MedicationSearchResponse>(body)
                } catch (e: Exception) {
                    Log.e("API", "JSON parsing error: ${e.message}")
                    com.example.myapplication.data.MedicationSearchResponse(
                        success = false,
                        error = "Failed to parse response"
                    )
                }
            }
        } else {
            Log.e("API", "NDC search failed: ${response.code}")
            com.example.myapplication.data.MedicationSearchResponse(
                success = false,
                error = "Search failed: ${response.code}"
            )
        }
    } catch (e: Exception) {
        Log.e("API", "NDC search failed", e)
        com.example.myapplication.data.MedicationSearchResponse(
            success = false,
            error = e.message ?: "Network error"
        )
    }
}

actual suspend fun addMedicationToUserList(request: com.example.myapplication.data.CreateMedicationRequest): Boolean = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()

        // Get the auth token from UserSession
        val token = com.example.myapplication.data.UserSession.authToken.value ?: run {
            Log.e("API", "No auth token available")
            return@withContext false
        }

        val json = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false  // Don't serialize null values
        }

        // Ensure we always send a start_date (default to today) so the calendar doesn't show past dots
        val today = try {
            // Avoid java.time to keep minSdk compatibility. Use Calendar and Locale to format yyyy-MM-dd
            val cal = java.util.Calendar.getInstance()
            val y = cal.get(java.util.Calendar.YEAR)
            val m = cal.get(java.util.Calendar.MONTH) + 1
            val d = cal.get(java.util.Calendar.DAY_OF_MONTH)
            java.util.Locale.US.let { String.format(it, "%04d-%02d-%02d", y, m, d) }
        } catch (_: Exception) {
            // fallback to a safe static date if something goes wrong
            "1970-01-01"
        }

        val requestToSend = if (request.startDate.isNullOrBlank()) {
            request.copy(startDate = today)
        } else request

        val jsonBody = json.encodeToString(
            com.example.myapplication.data.CreateMedicationRequest.serializer(),
            requestToSend
        )

        Log.d("API", "Sending medication request: $jsonBody")

        // Add medication to user's list
        val url = "${com.example.myapplication.api.ApiConfig.MEDICATION_BACKEND_URL}/api/medications/user"
        val requestBody = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())
        val addRequest = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .post(requestBody)
            .build()

        val response = client.newCall(addRequest).execute()

        if (response.isSuccessful) {
            val responseBody = response.body?.string()
            Log.d("API", "Medication added successfully: $responseBody")
            true
        } else {
            val errorBody = response.body?.string()
            Log.e("API", "Failed to add medication: ${response.code} - $errorBody")
            Log.e("API", "Request body was: $jsonBody")
            false
        }
    } catch (e: Exception) {
        Log.e("API", "Failed to add medication", e)
        false
    }
}
