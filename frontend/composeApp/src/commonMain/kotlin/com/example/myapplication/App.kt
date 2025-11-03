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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.UserSession
import com.example.myapplication.storage.createSecureStorage

@Composable
fun App(
    onEnableNotifications: suspend () -> Boolean = { false },
    onDisableNotifications: suspend () -> Boolean = { false },
    isNotificationsEnabled: () -> Boolean = { false }
) {
    CareCapsuleTheme {
        val currentUser by UserSession.currentUser.collectAsState()
        val initialScreen = if (currentUser != null) "main" else "login"

        var currentScreen by remember { mutableStateOf(initialScreen) }
        val secureStorage = remember { createSecureStorage() }
        val homeViewModel = remember { HomeViewModel(secureStorage = secureStorage) }
        val medicationViewModel = remember { MedicationViewModel(secureStorage = secureStorage) }
        val scanMedicationViewModel = remember { ScanMedicationViewModel() }

        // Watch for session changes and redirect to login when the user is signed out
        LaunchedEffect(currentUser) {
            if (currentUser == null) {
                currentScreen = "login"
            } else {
                currentScreen = "main"
            }
        }

        Surface(color = MaterialTheme.colorScheme.background) {
            when (currentScreen) {

                // Login screen first
                "login" -> LoginScreen(
                    onLoginSuccess = { },
                    onForgotPassword = { /* later feature */ },
                    onCreateAccount = { currentScreen = "createAccount" } // Go to Create Account
                )

                // Create Account Screen
                "createAccount" -> CreateAccountScreen(
                    onSignUpSuccess = { currentScreen = "main" },
                    onLoginClick = { currentScreen = "login" }
                )

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
            "home" -> HomeScreen(homeViewModel)
            "medications" -> MedicationScreen(medicationViewModel, modifier = Modifier.padding(innerPadding))
            "scan" ->    ScanMedicationScreen(
                viewModel = scanMedicationViewModel,
                showBackButton = false, // ← No back button
                onBarcodeScanned = { barcode ->
                    // print the scanned barcode
                    println("Scanned barcode: $barcode")
                },
                onMedicationAdded = {
                    // Refresh both home and medication screens
                    homeViewModel.loadMedications()
                    medicationViewModel.loadMedicationData()
                }
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