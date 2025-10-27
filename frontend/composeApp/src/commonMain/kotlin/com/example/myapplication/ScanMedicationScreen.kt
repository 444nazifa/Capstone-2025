// ============================================
// commonMain/ScanMedicationScreen.kt
// ============================================
package com.example.myapplication

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.myapplication.viewmodel.ScanMedicationViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ScanMedicationScreen(
    viewModel: ScanMedicationViewModel = viewModel(),
    showBackButton: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onBarcodeScanned: (String) -> Unit = {}
) {
    val barcode by viewModel.barcode.collectAsState()
    var cameraPermissionGranted by remember { mutableStateOf<Boolean?>(null) } // null = checking
    var showManualEntry by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Request permission on mount
    LaunchedEffect(Unit) {
        cameraPermissionGranted = requestCameraPermission()
    }

    // If barcode is scanned, call the callback and reset
    LaunchedEffect(barcode) {
        barcode?.let {
            onBarcodeScanned(it)
            viewModel.onBarcodeScanned("") // reset after callback
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            ScanHeader(
                showBackButton = showBackButton,
                onNavigateBack = onNavigateBack
            )

            when {
                cameraPermissionGranted == null -> {
                    // Still checking permission, show loading
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF2E7D32))
                    }
                }
                cameraPermissionGranted == false -> {
                    PermissionDeniedContent(
                        onRequestPermission = {
                            scope.launch {
                                cameraPermissionGranted = requestCameraPermission()
                            }
                        }
                    )
                }
                showManualEntry -> {
                    ManualEntryContent(
                        onBack = { showManualEntry = false },
                        onSubmit = { barcode ->
                            isProcessing = true
                            scope.launch {
                                val result = sendBarcodeToBackend(barcode)
                                isProcessing = false
                                if (result != null) {
                                    showSuccessDialog = true
                                }
                            }
                        },
                        isProcessing = isProcessing
                    )
                }
                else -> {
                    CameraContent(
                        onManualEntry = { showManualEntry = true },
                        onImageCaptured = { imageData ->
                            isProcessing = true
                            scope.launch {
                                val result = sendImageToBackend(imageData)
                                isProcessing = false
                                if (result != null) {
                                    showSuccessDialog = true
                                }
                            }
                        },
                        isProcessing = isProcessing
                    )
                }
            }
        }
    }

    if (showSuccessDialog) {
        SuccessDialog(
            result = barcode,
            onDismiss = {
                showSuccessDialog = false
            }
        )
    }
}

@Composable
fun ScanHeader(showBackButton: Boolean, onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2E7D32))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBackButton) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = "Scan Medication",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            color = Color.White
        )
    }
}

@Composable
fun CameraContent(
    onManualEntry: () -> Unit,
    onImageCaptured: (ByteArray) -> Unit,
    isProcessing: Boolean
) {
    var isScanningAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            isScanningAnimation = true
            delay(2000)
            isScanningAnimation = false
            delay(500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Instructions
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            elevation = CardDefaults.cardElevation(0.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Position the barcode or QR code inside the frame to scan",
                    fontSize = 14.sp,
                    color = Color(0xFF1B5E20),
                    lineHeight = 20.sp
                )
            }
        }

        // Camera Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            // Platform-specific camera view
            CameraView(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
            )

            // Scanning Frame Overlay
            Canvas(
                modifier = Modifier
                    .size(280.dp, 200.dp)
            ) {
                val cornerLength = 40.dp.toPx()
                val strokeWidth = 8.dp.toPx()
                val color = androidx.compose.ui.graphics.Color(0xFF2E7D32)

                // Top-left
                drawLine(color, androidx.compose.ui.geometry.Offset(0f, 0f),
                    androidx.compose.ui.geometry.Offset(cornerLength, 0f), strokeWidth)
                drawLine(color, androidx.compose.ui.geometry.Offset(0f, 0f),
                    androidx.compose.ui.geometry.Offset(0f, cornerLength), strokeWidth)

                // Top-right
                drawLine(color, androidx.compose.ui.geometry.Offset(size.width - cornerLength, 0f),
                    androidx.compose.ui.geometry.Offset(size.width, 0f), strokeWidth)
                drawLine(color, androidx.compose.ui.geometry.Offset(size.width, 0f),
                    androidx.compose.ui.geometry.Offset(size.width, cornerLength), strokeWidth)

                // Bottom-left
                drawLine(color, androidx.compose.ui.geometry.Offset(0f, size.height - cornerLength),
                    androidx.compose.ui.geometry.Offset(0f, size.height), strokeWidth)
                drawLine(color, androidx.compose.ui.geometry.Offset(0f, size.height),
                    androidx.compose.ui.geometry.Offset(cornerLength, size.height), strokeWidth)

                // Bottom-right
                drawLine(color, androidx.compose.ui.geometry.Offset(size.width, size.height - cornerLength),
                    androidx.compose.ui.geometry.Offset(size.width, size.height), strokeWidth)
                drawLine(color, androidx.compose.ui.geometry.Offset(size.width - cornerLength, size.height),
                    androidx.compose.ui.geometry.Offset(size.width, size.height), strokeWidth)
            }

            // Scanning line
            this@Column.AnimatedVisibility(
                visible = isScanningAnimation && !isProcessing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "scan")
                val animatedOffset by infiniteTransition.animateFloat(
                    initialValue = -100f,
                    targetValue = 100f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "scanLine"
                )

                Box(
                    modifier = Modifier
                        .size(280.dp, 2.dp)
                        .offset(y = animatedOffset.dp)
                        .background(Color(0xFF4CAF50))
                )
            }
        }

        // Bottom Actions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    if (!isProcessing) {
                        captureImage { imageData ->
                            onImageCaptured(imageData)
                        }
                    }
                },
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                enabled = !isProcessing,
                contentPadding = PaddingValues(0.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(30.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Capture",
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(
                onClick = onManualEntry,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32)),
                border = BorderStroke(2.dp, Color(0xFF2E7D32)),
                shape = RoundedCornerShape(12.dp),
                enabled = !isProcessing
            ) {
                Icon(imageVector = Icons.Default.EditNote, contentDescription = null,
                    modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("MANUAL ENTRY - 11 DIGITS", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                elevation = CardDefaults.cardElevation(0.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null,
                        tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("WARNING", fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            color = Color(0xFFE65100))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Always verify with your healthcare provider before taking any medication.",
                            fontSize = 11.sp,
                            color = Color(0xFF6D4C41),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ManualEntryContent(onBack: () -> Unit, onSubmit: (String) -> Unit, isProcessing: Boolean) {
    var barcodeText by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Icon(imageVector = Icons.Default.EditNote, contentDescription = null,
            modifier = Modifier.size(80.dp), tint = Color(0xFF2E7D32))

        Spacer(modifier = Modifier.height(24.dp))

        Text("Enter Barcode Number", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))

        Spacer(modifier = Modifier.height(8.dp))

        Text("Enter the 11-digit code manually", fontSize = 14.sp, color = Color.Gray,
            textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = barcodeText,
            onValueChange = {
                if (it.length <= 11 && it.all { char -> char.isDigit() }) {
                    barcodeText = it
                    showError = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Barcode Number") },
            placeholder = { Text("Enter 11 digits") },
            isError = showError,
            supportingText = {
                if (showError) {
                    Text("Please enter exactly 11 digits", color = MaterialTheme.colorScheme.error)
                } else {
                    Text("${barcodeText.length}/11 digits")
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (barcodeText.length == 11) {
                        onSubmit(barcodeText)
                    } else {
                        showError = true
                    }
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2E7D32),
                focusedLabelColor = Color(0xFF2E7D32)
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (barcodeText.length == 11) {
                    onSubmit(barcodeText)
                } else {
                    showError = true
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            shape = RoundedCornerShape(12.dp),
            enabled = !isProcessing
        ) {
            if (isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("SUBMIT", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onBack, enabled = !isProcessing) {
            Text("Back to Camera", color = Color(0xFF2E7D32))
        }
    }
}

@Composable
fun PermissionDeniedContent(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null,
            modifier = Modifier.size(80.dp), tint = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        Text("Camera Permission Required", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(12.dp))

        Text("To scan medication barcodes, we need access to your camera.",
            fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Grant Permission", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SuccessDialog(result: String?, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null,
                    modifier = Modifier.size(60.dp), tint = Color(0xFF4CAF50))

                Spacer(modifier = Modifier.height(16.dp))

                Text("Scan Successful!", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))

                Spacer(modifier = Modifier.height(8.dp))

                Text("Medication information retrieved", fontSize = 14.sp, color = Color.Gray,
                    textAlign = TextAlign.Center)

                if (result != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Text("Barcode: $result", modifier = Modifier.padding(12.dp),
                            fontSize = 12.sp, color = Color(0xFF1B5E20))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done")
                }
            }
        }
    }
}

// Platform-specific expect declarations
@Composable
expect fun CameraView(modifier: Modifier)

expect suspend fun requestCameraPermission(): Boolean

expect fun captureImage(onImageCaptured: (ByteArray) -> Unit)

expect suspend fun sendImageToBackend(imageData: ByteArray): String?

expect suspend fun sendBarcodeToBackend(barcode: String): String?