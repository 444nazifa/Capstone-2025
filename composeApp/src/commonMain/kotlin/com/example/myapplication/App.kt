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
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    CareCapsuleTheme {
        var currentScreen by remember { mutableStateOf("login") }  // 🔹 Track whether we're on login or main
        val viewModel = remember { HomeViewModel() }

        Surface(color = MaterialTheme.colorScheme.background) {
            when (currentScreen) {

                // 🔹 Login screen first
                "login" -> LoginScreen(
                    onLogin = { currentScreen = "main" } // switch to main after login
                )

                // 🔹 Main app (your bottom navigation)
                "main" -> MainApp(viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: HomeViewModel) {
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
            "home" -> HomeScreen(viewModel)
            "medications" -> MedicationScreen(modifier = Modifier.padding(innerPadding))
            "scan" -> ScanScreen(modifier = Modifier.padding(innerPadding))
            "profile" -> ProfileScreen(modifier = Modifier.padding(innerPadding))
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
