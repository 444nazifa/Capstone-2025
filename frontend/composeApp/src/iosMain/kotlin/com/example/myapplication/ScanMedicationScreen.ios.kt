// ============================================
// iosMain/ScanMedicationScreen.ios.kt
// ============================================
package com.example.myapplication

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import platform.AVFoundation.*
import platform.CoreGraphics.CGRectMake
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue

private var globalCaptureSession: AVCaptureSession? = null
private var globalPhotoOutput: AVCapturePhotoOutput? = null

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CameraView(modifier: Modifier) {
    val previewLayer = remember { mutableStateOf<AVCaptureVideoPreviewLayer?>(null) }
    val captureSession = remember { mutableStateOf<AVCaptureSession?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            captureSession.value?.stopRunning()
        }
    }

    UIKitView(
        factory = {
            val view = UIView()
            view.backgroundColor = UIColor.blackColor
            view.clipsToBounds = true

            // Setup camera on background thread
            dispatch_async(
                dispatch_get_global_queue(
                    DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(),
                    0u
                )
            ) {
                val session = AVCaptureSession()

                session.beginConfiguration()
                session.sessionPreset = AVCaptureSessionPresetHigh // Changed from Photo to High

                val videoDevice = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
                if (videoDevice != null) {
                    val videoInput = try {
                        AVCaptureDeviceInput.deviceInputWithDevice(
                            videoDevice,
                            null
                        ) as AVCaptureDeviceInput
                    } catch (e: Exception) {
                        println("CameraView: Failed to create video input: ${e.message}")
                        null
                    }

                    if (videoInput != null && session.canAddInput(videoInput)) {
                        session.addInput(videoInput)
                        println("CameraView: Video input added successfully")
                    } else {
                        println("CameraView: Cannot add video input to session")
                    }

                    val photoOutput = AVCapturePhotoOutput()
                    if (session.canAddOutput(photoOutput)) {
                        session.addOutput(photoOutput)
                        globalPhotoOutput = photoOutput
                        println("CameraView: Photo output added successfully")
                    } else {
                        println("CameraView: Cannot add photo output to session")
                    }

                    session.commitConfiguration()

                    // Setup preview layer on main thread
                    dispatch_async(dispatch_get_main_queue()) {
                        val preview = AVCaptureVideoPreviewLayer(session = session)
                        preview.videoGravity = AVLayerVideoGravityResizeAspectFill
                        preview.frame = view.layer.bounds
                        view.layer.insertSublayer(
                            preview,
                            atIndex = 0u
                        ) // Ensure it's at the bottom
                        previewLayer.value = preview
                        captureSession.value = session
                        globalCaptureSession = session

                        println("CameraView: Preview layer added with frame: ${view.layer.bounds}")

                        // Start session on background thread
                        dispatch_async(
                            dispatch_get_global_queue(
                                DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(),
                                0u
                            )
                        ) {
                            session.startRunning()
                            dispatch_async(dispatch_get_main_queue()) {
                                println("CameraView: Session is running: ${session.running}")
                            }
                        }
                    }
                } else {
                    println("CameraView: No video device available")
                }
            }
            view
        },
        modifier = modifier,
        update = { view ->
            // Always update preview layer frame to match view bounds on main thread
            dispatch_async(dispatch_get_main_queue()) {
                previewLayer.value?.frame = view.layer.bounds
            }
        }
    )
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun requestCameraPermission(): Boolean {
    return withContext(Dispatchers.Main) {
        val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        println("Camera permission status: $status")

        when (status) {
            AVAuthorizationStatusAuthorized -> {
                println("Camera permission: Authorized")
                true
            }

            AVAuthorizationStatusNotDetermined -> {
                println("Camera permission: Not determined, requesting...")
                suspendCancellableCoroutine<Boolean> { continuation ->
                    AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                        println("Camera permission result: $granted")
                        continuation.resume(granted)
                    }
                }
            }

            else -> {
                println("Camera permission: Denied or Restricted")
                false
            }
        }
    }
}

actual fun captureImage(onImageCaptured: (ByteArray) -> Unit) {
    val photoOutput = globalPhotoOutput ?: return

    val settings = AVCapturePhotoSettings.photoSettings()

    val delegate = object : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
        override fun captureOutput(
            output: AVCapturePhotoOutput,
            didFinishProcessingPhoto: AVCapturePhoto,
            error: NSError?
        ) {
            if (error != null) {
                println("Photo capture error: ${error.localizedDescription}")
                return
            }

            didFinishProcessingPhoto.fileDataRepresentation()?.let { data ->
                val bytes = ByteArray(data.length.toInt())
                @OptIn(ExperimentalForeignApi::class)
                data.getBytes(bytes.refTo(0).getPointer(MemScope()), data.length)
                onImageCaptured(bytes)
            }
        }
    }

    photoOutput.capturePhotoWithSettings(settings, delegate)
}

actual suspend fun sendImageToBackend(imageData: ByteArray): String? {
    return try {
        val client = HttpClient()
        val url = "${com.example.myapplication.api.ApiConfig.PYTHON_BACKEND_URL}/api/scan-qr"

        val response: HttpResponse = client.submitFormWithBinaryData(
            url = url,
            formData = formData {
                append("image", imageData, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(HttpHeaders.ContentDisposition, "filename=medication.jpg")
                })
            }
        )

        if (response.status.isSuccess()) {
            val responseText = response.bodyAsText()
            println("Scan QR Response: $responseText")

            try {
                if (responseText.contains("\"qr_detected\":true") || responseText.contains("\"qr_detected\": true")) {
                    val ndcRegex = Regex("\"ndc\"\\s*:\\s*\"([^\"]+)\"")
                    val match = ndcRegex.find(responseText)
                    match?.groupValues?.get(1) ?: "QR code detected"
                } else {
                    null
                }
            } catch (e: Exception) {
                println("JSON parsing error: ${e.message}")
                null
            }
        } else {
            println("Image upload failed: ${response.status}")
            null
        }
    } catch (e: Exception) {
        println("Image upload failed: ${e.message}")
        null
    }
}

actual suspend fun searchMedicationsByNDC(ndc: String): com.example.myapplication.data.MedicationSearchResponse? {
    return try {
        val client = HttpClient {
            install(ContentNegotiation) {
                json(kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
        val url = "${com.example.myapplication.api.ApiConfig.MEDICATION_BACKEND_URL}/api/medication/search/?query=$ndc"

        val response: HttpResponse = client.get(url)

        if (response.status.isSuccess()) {
            val responseText = response.bodyAsText()
            println("NDC Search Response: $responseText")

            try {
                val json = kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
                json.decodeFromString<com.example.myapplication.data.MedicationSearchResponse>(responseText)
            } catch (e: Exception) {
                println("JSON parsing error: ${e.message}")
                com.example.myapplication.data.MedicationSearchResponse(
                    success = false,
                    error = "Failed to parse response"
                )
            }
        } else {
            println("NDC search failed: ${response.status}")
            com.example.myapplication.data.MedicationSearchResponse(
                success = false,
                error = "Search failed: ${response.status}"
            )
        }
    } catch (e: Exception) {
        println("NDC search failed: ${e.message}")
        com.example.myapplication.data.MedicationSearchResponse(
            success = false,
            error = e.message ?: "Network error"
        )
    }
}

actual suspend fun addMedicationToUserList(request: com.example.myapplication.data.CreateMedicationRequest): Boolean {
    return try {
        val client = HttpClient {
            install(ContentNegotiation) {
                json(kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    explicitNulls = false
                })
            }
        }

        // Get the auth token from UserSession
        val token = com.example.myapplication.data.UserSession.authToken.value ?: run {
            println("No auth token available")
            return false
        }

        // Ensure start_date defaults to today if missing
        val today = try {
            // Use NSDateFormatter to produce yyyy-MM-dd
            val formatter = NSDateFormatter()
            formatter.dateFormat = "yyyy-MM-dd"
            formatter.stringFromDate(NSDate())
        } catch (e: Exception) {
            // fallback
            "1970-01-01"
        }

        val requestToSend = if (request.startDate.isNullOrBlank()) {
            request.copy(startDate = today)
        } else request

        val json = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }
        val requestJson = json.encodeToString(
            com.example.myapplication.data.CreateMedicationRequest.serializer(),
            requestToSend
        )
        println("Sending medication request: $requestJson")

        val url = "${com.example.myapplication.api.ApiConfig.MEDICATION_BACKEND_URL}/api/medications/user"
        val response: HttpResponse = client.post(url) {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(requestToSend)
        }

        if (response.status.isSuccess()) {
            val responseText = response.bodyAsText()
            println("Medication added successfully: $responseText")
            true
        } else {
            val errorText = response.bodyAsText()
            println("Failed to add medication: ${'$'}{response.status} - ${'$'}errorText")
            println("Request body was: $requestJson")
            false
        }
    } catch (e: Exception) {
        println("Failed to add medication: ${e.message}")
        false
    }
}