package com.example.myapplication

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.theme.CareCapsuleTheme
import com.example.myapplication.viewmodel.HomeViewModel
import com.example.myapplication.viewmodel.MedicationViewModel
import com.example.myapplication.viewmodel.ScanMedicationViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.example.myapplication.data.UserSession
import com.example.myapplication.storage.createSecureStorage

@Composable
fun App(
    onEnableNotifications: suspend () -> Boolean = { false },
    onDisableNotifications: suspend () -> Boolean = { false },
    isNotificationsEnabled: () -> Boolean = { false },
    initialRoute: String? = null,
    resetToken: String? = null
) {
    CareCapsuleTheme {
        val currentUser by UserSession.currentUser.collectAsState()
        val initialScreen = when {
            resetToken != null -> "resetPassword"
            initialRoute != null -> initialRoute
            currentUser != null -> "main"
            else -> "login"
        }

        var currentScreen by remember { mutableStateOf(initialScreen) }
        var passwordResetToken by remember { mutableStateOf(resetToken) }
        val secureStorage = remember { createSecureStorage() }
        val homeViewModel = remember { HomeViewModel(secureStorage = secureStorage) }
        val medicationViewModel = remember {
            MedicationViewModel(
                secureStorage = secureStorage,
                onMedicationDeleted = {
                    homeViewModel.loadMedications()
                },
                onMedicationUpdated = {
                    homeViewModel.loadMedications()
                }
            )
        }
        val scanMedicationViewModel = remember { ScanMedicationViewModel() }

        // 🔐 Session-driven routing with guards in both directions
        LaunchedEffect(currentUser) {
            if (currentUser == null && currentScreen != "resetPassword" && currentScreen != "forgotPassword") {
                currentScreen = "login"
            } else if (currentUser != null && currentScreen !in listOf("resetPassword", "forgotPassword")) {
                currentScreen = "main"
            }
        }

        Surface(color = MaterialTheme.colorScheme.background) {
            when (currentScreen) {
                "login" -> LoginScreen(
                    onLoginSuccess = { },
                    onForgotPassword = { currentScreen = "forgotPassword" },
                    onCreateAccount = { currentScreen = "createAccount" },
                    onReregisterNotifications = onEnableNotifications
                )

                "createAccount" -> CreateAccountScreen(
                    onSignUpSuccess = { /* session watcher will flip to main */ },
                    onLoginClick = { currentScreen = "login" }
                )

                // Forgot Password Screen
                "forgotPassword" -> ForgotPasswordScreen(
                    onBackToLogin = { currentScreen = "login" }
                )

                // Reset Password Screen
                "resetPassword" -> {
                    if (passwordResetToken != null) {
                        ResetPasswordScreen(
                            resetToken = passwordResetToken!!,
                            onPasswordResetSuccess = { currentScreen = "login" },
                            onBackToLogin = { currentScreen = "login" }
                        )
                    } else {
                        // If no token, redirect to login
                        LaunchedEffect(Unit) {
                            currentScreen = "login"
                        }
                    }
                }

                // Main app (your bottom navigation)
                "main" -> MainApp(
                    homeViewModel = homeViewModel,
                    medicationViewModel = medicationViewModel,
                    scanMedicationViewModel = scanMedicationViewModel,
                    onSignOut = {
                        UserSession.logout()
                    },
                    onEnableNotifications = onEnableNotifications,
                    onDisableNotifications = onDisableNotifications,
                    isNotificationsEnabled = isNotificationsEnabled
                )
            }
        }
    }
}

@Composable
fun MainApp(
    homeViewModel: HomeViewModel,
    medicationViewModel: MedicationViewModel,
    scanMedicationViewModel: ScanMedicationViewModel,
    onSignOut: () -> Unit = {},
    onEnableNotifications: suspend () -> Boolean = { false },
    onDisableNotifications: suspend () -> Boolean = { false },
    isNotificationsEnabled: () -> Boolean = { false }
) {
    var selectedTab by remember { mutableStateOf("home") }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == "home",
                    onClick = { selectedTab = "home" },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == "medications",
                    onClick = { selectedTab = "medications" },
                    icon = { Icon(Icons.Default.MedicalServices, contentDescription = "Medications") },
                    label = { Text("Medications") }
                )
                NavigationBarItem(
                    selected = selectedTab == "scan",
                    onClick = { selectedTab = "scan" },
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Scan") },
                    label = { Text("Scan") }
                )
                NavigationBarItem(
                    selected = selectedTab == "profile",
                    onClick = { selectedTab = "profile" },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            "home" -> HomeScreen(
                viewModel = homeViewModel,
                modifier = Modifier.padding(innerPadding)
            )
            "medications" -> MedicationScreen(
                viewModel = medicationViewModel,
                modifier = Modifier.padding(innerPadding)
            )

            "scan" -> ScanMedicationScreen(
                viewModel = scanMedicationViewModel,
                showBackButton = false,
                onBarcodeScanned = { barcode ->
                    println("Scanned barcode: $barcode")
                },
                onMedicationAdded = {
                    homeViewModel.loadMedications()
                    medicationViewModel.loadMedicationData()
                },
                modifier = Modifier.padding(innerPadding)   // ← important
            )

            "profile" -> ProfileScreen(
                modifier = Modifier.padding(innerPadding),
                onSignOut = onSignOut,
                onEnableNotifications = onEnableNotifications,
                onDisableNotifications = onDisableNotifications,
                isNotificationsEnabled = isNotificationsEnabled
            )
        }
    }
}

@Composable
fun MedicationsScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Text("Medications Page", modifier = Modifier.padding(32.dp))
    }
}

@Composable
fun ScanScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Text("Scan Page", modifier = Modifier.padding(32.dp))
    }
}

@Composable
fun ProfilesScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Text("Profile Page", modifier = Modifier.padding(32.dp))
    }
}


@Preview
@Composable
fun AppPreview() {
    App()
}