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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlin.coroutines.resume

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

// Only check permission in suspend function, no Compose APIs
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

        // Replace with your actual backend URL
        val url = "https://your-api.com/api/scan-medication"

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
            // Parse response - adjust based on your API response format
            val responseBody = response.body?.string()
            // Example: {"barcode": "12345678901", "medication_name": "Aspirin"}
            // Parse JSON and return barcode
            responseBody?.let {
                // Use kotlinx.serialization or Gson to parse
                // For now, mock response:
                "12345678901"
            }
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e("API", "Image upload failed", e)
        null
    }
}

actual suspend fun sendBarcodeToBackend(barcode: String): String? = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()

        // Replace with your actual backend URL
        val url = "https://your-api.com/api/verify-barcode"

        val json = """{"barcode": "$barcode"}"""
        val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            // Parse response
            val responseBody = response.body?.string()
            // Return medication info or barcode
            barcode
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e("API", "Barcode verification failed", e)
        null
    }
}