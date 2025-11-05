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
import com.example.myapplication.viewmodel.ScanMedicationViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.example.myapplication.data.UserSession
import com.example.myapplication.ForgotPasswordScreen

@Composable
fun App() {
    CareCapsuleTheme {
        val currentUser by UserSession.currentUser.collectAsState()

        // Start at login by default; the session watcher will move you to main if logged in.
        var currentScreen by remember { mutableStateOf("login") }

        val homeViewModel = remember { HomeViewModel() }
        val scanMedicationViewModel = remember { ScanMedicationViewModel() }

        // 🔐 Session-driven routing with guards in both directions
        LaunchedEffect(currentUser) {
            if (currentUser == null && currentScreen == "main") {
                // user signed out → force to login
                currentScreen = "login"
            } else if (currentUser != null && currentScreen == "login") {
                // user signed in → go to main (but don't hijack forgot/create screens)
                currentScreen = "main"
            }
        }

        Surface(color = MaterialTheme.colorScheme.background) {
            when (currentScreen) {
                "login" -> LoginScreen(
                    onLoginSuccess = { /* session watcher will flip to main */ },
                    onForgotPassword = { currentScreen = "forgotPassword" },
                    onCreateAccount = { currentScreen = "createAccount" }
                )

                "createAccount" -> CreateAccountScreen(
                    onSignUpSuccess = { /* session watcher will flip to main */ },
                    onLoginClick = { currentScreen = "login" }
                )

                "forgotPassword" -> ForgotPasswordScreen(
                    onBackToLogin = { currentScreen = "login" }
                )

                "main" -> MainApp(
                    homeViewModel = homeViewModel,
                    scanMedicationViewModel = scanMedicationViewModel,
                    onSignOut = {
                        // ❗ Don’t set currentScreen here—let the session watcher handle it.
                        UserSession.logout()
                    }
                )
            }
        }
    }
}

@Composable
fun MainApp(
    homeViewModel: HomeViewModel,
    scanMedicationViewModel: ScanMedicationViewModel,
    onSignOut: () -> Unit = {}
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
            "medications" -> MedicationsScreen(modifier = Modifier.padding(innerPadding))
            "scan" ->    ScanMedicationScreen(
                viewModel = scanMedicationViewModel,
                showBackButton = false, // ← No back button
                onBarcodeScanned = { barcode ->
                    // print the scanned barcode
                    println("Scanned barcode: $barcode")
                }
            )
            "profile" -> ProfileScreen(
                modifier = Modifier.padding(innerPadding),
                onSignOut = onSignOut
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