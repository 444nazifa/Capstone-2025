// ============================================
// androidMain/ScanMedicationScreen.android.kt
// ============================================
package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private var globalImageCapture: ImageCapture? = null

@Composable
actual fun CameraView(modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                cameraProviderFuture.get()?.unbindAll()
            } catch (e: Exception) {
                Log.e("CameraView", "Error unbinding camera", e)
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val executor = ContextCompat.getMainExecutor(ctx)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    Log.d("CameraView", "Setting up camera preview")

                    // Get available cameras
                    val availableCameras = cameraProvider.availableCameraInfos
                    Log.d("CameraView", "Available cameras: ${availableCameras.size}")
                    availableCameras.forEachIndexed { index, cameraInfo ->
                        Log.d("CameraView", "Camera $index: ${cameraInfo}")
                    }

                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                    val imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(previewView.display.rotation)
                        .build()

                    globalImageCapture = imageCapture

                    // Try to select a camera with fallback logic for emulator compatibility
                    val cameraSelector = if (availableCameras.isEmpty()) {
                        Log.e("CameraView", "No cameras available!")
                        throw IllegalStateException("No cameras available")
                    } else {
                        // For emulator: Just use first available camera without lens facing requirement
                        Log.d("CameraView", "Selecting first available camera (works with emulator webcam)")
                        // Build a selector without lens facing requirements for emulator compatibility
                        try {
                            // First try with default back camera
                            if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                                Log.d("CameraView", "Using DEFAULT_BACK_CAMERA")
                                CameraSelector.DEFAULT_BACK_CAMERA
                            } else {
                                // Emulator mode: accept any available camera
                                Log.d("CameraView", "Using first available camera without lens facing filter")
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }
                        } catch (e: IllegalArgumentException) {
                            // If no back camera, just use any camera
                            Log.d("CameraView", "Back camera not found, using any available camera")
                            CameraSelector.Builder().build()
                        }
                    }

                    // Unbind before rebinding
                    cameraProvider.unbindAll()

                    // Bind to lifecycle
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )

                    Log.d("CameraView", "Camera bound successfully to lifecycle")
                } catch (e: Exception) {
                    Log.e("CameraView", "Camera binding failed: ${e.message}", e)
                    e.printStackTrace()
                }
            }, executor)
            previewView
        },
        modifier = modifier,
        update = { previewView ->
            // Update rotation if needed
            globalImageCapture?.targetRotation = previewView.display.rotation
        }
    )
}

@Composable
actual fun RequestCameraPermissionHandler(onPermissionResult: (Boolean) -> Unit) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onPermissionResult(isGranted)
    }

    LaunchedEffect(Unit) {
        val permission = Manifest.permission.CAMERA
        when {
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                onPermissionResult(true)
            }
            else -> {
                permissionLauncher.launch(permission)
            }
        }
    }
}

actual suspend fun requestCameraPermission(): Boolean {
    // This is called from LaunchedEffect in commonMain
    // The actual permission request is handled by the Composable above
    // This function just checks if permission is already granted
    return true
}

actual fun captureImage(onImageCaptured: (ByteArray) -> Unit) {
    val capture = globalImageCapture
    if (capture == null) {
        Log.e("Camera", "ImageCapture not initialized")
        return
    }

    Log.d("Camera", "Starting image capture...")
    val executor = Executors.newSingleThreadExecutor()

    capture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    Log.d("Camera", "Image captured successfully: ${image.width}x${image.height}")
                    val bitmap = imageProxyToBitmap(image)
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                    val byteArray = stream.toByteArray()
                    Log.d("Camera", "Image converted to JPEG: ${byteArray.size} bytes")
                    image.close()
                    onImageCaptured(byteArray)
                } catch (e: Exception) {
                    Log.e("Camera", "Error processing captured image", e)
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("Camera", "Capture failed: ${exception.message}", exception)
            }
        }
    )
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    // Handle different image formats
    return when (image.format) {
        android.graphics.ImageFormat.YUV_420_888 -> {
            // Convert YUV to RGB
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)

            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = android.graphics.YuvImage(
                nv21,
                android.graphics.ImageFormat.NV21,
                image.width,
                image.height,
                null
            )
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(
                android.graphics.Rect(0, 0, image.width, image.height),
                90,
                out
            )
            val imageBytes = out.toByteArray()
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        }
        android.graphics.ImageFormat.JPEG -> {
            // Direct JPEG conversion
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
        else -> {
            // Fallback for other formats
            Log.w("Camera", "Unknown image format: ${image.format}, attempting direct conversion")
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }
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
                    // Try to extract NDC from prescription_data.ndc_number first
                    val ndcNumberRegex = Regex("\"ndc_number\"\\s*:\\s*\"([^\"]+)\"")
                    val ndcMatch = ndcNumberRegex.find(it)
                    if (ndcMatch != null) {
                        val ndc = ndcMatch.groupValues[1]
                        Log.d("API", "Extracted NDC from prescription_data: $ndc")
                        ndc
                    } else {
                        // Fallback: try to extract from raw ndc field
                        val ndcRegex = Regex("\"ndc\"\\s*:\\s*\"([^\"]+)\"")
                        val match = ndcRegex.find(it)
                        if (match != null) {
                            val ndc = match.groupValues[1]
                            Log.d("API", "Extracted NDC from raw data: $ndc")
                            ndc
                        } else {
                            Log.w("API", "QR detected but no NDC found in response")
                            null
                        }
                    }
                } else {
                    Log.d("API", "No QR code detected in image")
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
        // Use the dedicated NDC search endpoint
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
