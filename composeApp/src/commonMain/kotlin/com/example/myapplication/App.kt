package com.example.myapplication

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // Create an instance of HomeViewModel
            val viewModel = remember { HomeViewModel() }
            Spacer(
                modifier = Modifier.fillMaxSize()

            )
            // Display the HomeScreen with the viewModel
            HomeScreen(viewModel = viewModel)
        }
    }
}