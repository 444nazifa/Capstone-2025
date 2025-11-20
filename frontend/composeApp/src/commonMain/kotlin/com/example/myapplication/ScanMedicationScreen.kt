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
import com.example.myapplication.theme.CareCapsuleTheme
import com.example.myapplication.theme.ScreenContainer
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ScanMedicationScreen(
    viewModel: ScanMedicationViewModel,
    showBackButton: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onBarcodeScanned: (String) -> Unit = {},
    onMedicationAdded: () -> Unit = {},
    previewMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val barcode by viewModel.barcode.collectAsState()
    var cameraPermissionGranted by remember { mutableStateOf<Boolean?>(null) } // null = checking
    var showManualEntry by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<com.example.myapplication.data.MedicationSearchResult>?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showAddBottomSheet by remember { mutableStateOf(false) }
    var selectedMedication by remember { mutableStateOf<com.example.myapplication.data.MedicationSearchResult?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Request permission on mount
    LaunchedEffect(previewMode) {
        cameraPermissionGranted = if (previewMode) {
            true       // pretend permission is granted in preview
        } else {
            requestCameraPermission()
        }
    }

    // If barcode is scanned, call the callback and reset
    LaunchedEffect(barcode) {
        barcode?.let {
            onBarcodeScanned(it)
            viewModel.onBarcodeScanned("") // reset after callback
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 20.dp)
            ) {
                if (!showManualEntry && cameraPermissionGranted != false) {
                    ScanHeader(
                        showBackButton = showBackButton,
                        onNavigateBack = onNavigateBack
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when {
                        cameraPermissionGranted == null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                            ManualEntryWithResultsContent(
                                onBack = {
                                    showManualEntry = false
                                    searchResults = null
                                    searchQuery = ""
                                },
                                onSearch = { query ->
                                    isProcessing = true
                                    searchQuery = query
                                    scope.launch {
                                        val response = searchMedicationsByNDC(query)
                                        isProcessing = false
                                        if (response != null && response.success && !response.data.isNullOrEmpty()) {
                                            searchResults = response.data
                                        } else {
                                            searchResults = emptyList()
                                            errorMessage = response?.error ?: "No medications found"
                                            showErrorDialog = true
                                        }
                                    }
                                },
                                searchResults = searchResults,
                                isProcessing = isProcessing,
                                onMedicationSelected = { medication ->
                                    selectedMedication = medication
                                    showAddBottomSheet = true
                                }
                            )
                        }
                        else -> {
                            CameraContent(
                                onManualEntry = { showManualEntry = true },
                                onImageCaptured = { imageData ->
                                    isProcessing = true
                                    scope.launch {
                                        val ndc = sendImageToBackend(imageData)
                                        if (ndc != null) {
                                            searchQuery = ndc
                                            val response = searchMedicationsByNDC(ndc)
                                            isProcessing = false
                                            if (response != null && response.success && !response.data.isNullOrEmpty()) {
                                                searchResults = response.data
                                            } else {
                                                errorMessage = response?.error
                                                    ?: "No medications found for NDC: $ndc"
                                                showErrorDialog = true
                                            }
                                        } else {
                                            isProcessing = false
                                            errorMessage = "Could not read barcode from image"
                                            showErrorDialog = true
                                        }
                                    }
                                },
                                isProcessing = isProcessing,
                                previewMode = previewMode
                            )
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Search Failed",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(errorMessage)
            },
            confirmButton = {
                Button(
                    onClick = { showErrorDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("OK")
                }
            }
        )
    }

    // Add Medication Bottom Sheet
    if (showAddBottomSheet && selectedMedication != null) {
        AddMedicationBottomSheet(
            medication = selectedMedication!!,
            onDismiss = {
                showAddBottomSheet = false
                selectedMedication = null
            },
            onSave = { request ->
                scope.launch {
                    isProcessing = true
                    val success = addMedicationToUserList(request)
                    isProcessing = false
                    if (success) {
                        snackbarHostState.showSnackbar(
                            message = "✓ Medication added successfully!",
                            duration = SnackbarDuration.Short
                        )

                        showAddBottomSheet = false
                        selectedMedication = null
                        searchResults = null
                        searchQuery = ""
                        showManualEntry = false

                        onMedicationAdded()
                    } else {
                        errorMessage = "Failed to add medication. Please try again."
                        showErrorDialog = true
                    }
                }
            },
            isLoading = isProcessing
        )
    }
}

@Composable
fun ScanHeader(showBackButton: Boolean, onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(top = 12.dp, bottom = 16.dp, start = 20.dp, end = 20.dp),
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
                    tint = MaterialTheme.colorScheme.onPrimary                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = "Scan Medication",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun CameraContent(
    onManualEntry: () -> Unit,
    onImageCaptured: (ByteArray) -> Unit,
    isProcessing: Boolean,
    previewMode: Boolean = false
) {
    var isScanningAnimation by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary

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
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Instructions
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            ),
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Position the barcode or QR code inside the frame to scan",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
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
            if (previewMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.LightGray.copy(alpha = 0.4f))
                )
            } else {
                CameraView(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                )
            }

            // Scanning Frame Overlay
            Canvas(
                modifier = Modifier
                    .size(280.dp, 200.dp)
            ) {
                val cornerLength = 40.dp.toPx()
                val strokeWidth = 8.dp.toPx()
                val color = primaryColor
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
                        .background(MaterialTheme.colorScheme.primary)
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
                    if (!isProcessing && !previewMode) {
                        captureImage { imageData ->
                            onImageCaptured(imageData)
                        }
                    }
                },
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),                enabled = !isProcessing,
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
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                enabled = !isProcessing
            ) {
                Icon(imageVector = Icons.Default.EditNote, contentDescription = null,
                    modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("SEARCH BY NDC OR NAME", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
fun ManualEntryWithResultsContent(
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    searchResults: List<com.example.myapplication.data.MedicationSearchResult>?,
    isProcessing: Boolean,
    onMedicationSelected: (com.example.myapplication.data.MedicationSearchResult) -> Unit
) {
    var ndcText by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Input Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)                                      // inner padding only
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Search Medications",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = ndcText,
                    onValueChange = {
                        if (it.length <= 50) {
                            ndcText = it
                            showError = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("NDC Code or Medication Name") },
                    placeholder = { Text("e.g., 0378-6208-93 or Aspirin") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF2E7D32)) },
                    isError = showError,
                    supportingText = {
                        if (showError) {
                            Text(
                                "Please enter a valid NDC or medication name",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text("Accepts dashes: 12345-678-90")
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val trimmed = ndcText.trim()
                            if (trimmed.isNotEmpty()) {
                                onSearch(trimmed)
                            } else {
                                showError = true
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val trimmed = ndcText.trim()
                        if (trimmed.isNotEmpty()) {
                            onSearch(trimmed)
                        } else {
                            showError = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SEARCHING...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Search, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SEARCH", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Results Section
        when {
            searchResults == null -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Default.EditNote, null, Modifier.size(80.dp), Color(0xFF2E7D32).copy(alpha = 0.5f))
                        Spacer(Modifier.height(16.dp))
                        Text("Enter NDC or Medication Name", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text("Search for medications to add to your list", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            }
            searchResults.isEmpty() -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Default.SearchOff, null, Modifier.size(80.dp), Color.Gray)
                        Spacer(Modifier.height(16.dp))
                        Text("No Medications Found", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        Text("Try a different search term", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
            }
            else -> {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "${searchResults.size} Result${if (searchResults.size != 1) "s" else ""} Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(searchResults.size) { index ->
                        MedicationResultCard(
                            medication = searchResults[index],
                            isAdding = false,
                            onAdd = {
                                onMedicationSelected(searchResults[index])
                            }
                        )
                    }
                }
            }
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
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        Spacer(modifier = Modifier.height(8.dp))

        Text("To scan medications, we need access to your camera.",
            fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("GRANT PERMISSION", fontWeight = FontWeight.Bold)
        }
    }
}


// Platform-specific expect declarations
@Composable
expect fun CameraView(modifier: Modifier)

expect suspend fun requestCameraPermission(): Boolean

expect fun captureImage(onImageCaptured: (ByteArray) -> Unit)

expect suspend fun sendImageToBackend(imageData: ByteArray): String?

expect suspend fun searchMedicationsByNDC(ndc: String): com.example.myapplication.data.MedicationSearchResponse?

expect suspend fun addMedicationToUserList(request: com.example.myapplication.data.CreateMedicationRequest): Boolean

@Preview
@Composable
fun ScanMedicationScreenPreview() {
    CareCapsuleTheme {
        val previewViewModel = remember { ScanMedicationViewModel() }
        ScanMedicationScreen(
            viewModel = previewViewModel,
            showBackButton = true,
            previewMode = true
        )
    }
}

