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

        // Replace with your actual backend URL
        val url = "https://your-api.com/api/scan-medication"

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
            // Parse JSON response - adjust based on your API
            // For now, mock response:
            "12345678901"
        } else {
            null
        }
    } catch (e: Exception) {
        println("Image upload failed: ${e.message}")
        null
    }
}

actual suspend fun sendBarcodeToBackend(barcode: String): String? {
    return try {
        val client = HttpClient()

        // Replace with your actual backend URL
        val url = "https://your-api.com/api/verify-barcode"

        val response: HttpResponse = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody("""{"barcode": "$barcode"}""")
        }

        if (response.status.isSuccess()) {
            val responseText = response.bodyAsText()
            // Parse response and return result
            barcode
        } else {
            null
        }
    } catch (e: Exception) {
        println("Barcode verification failed: ${e.message}")
        null
    }
}