package com.example.myapplication.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.Alignment

// 🌿 CareCapsule Brand Colors
private val CareCapsuleGreen = Color(0xFF4CAF50)
private val CareCapsuleDarkGreen = Color(0xFF388E3C)
private val CareCapsuleLightGray = Color(0xFFF3F3F3)

// ---------- Global layout constants ----------
private val ScreenHorizontalPadding = 20.dp
private val ScreenTopPadding = 15.dp

// ✅ Light Theme Colors
private val LightColors = lightColorScheme(
    primary = CareCapsuleGreen,               // buttons, highlights
    onPrimary = Color.White,
    secondary = CareCapsuleDarkGreen,         // secondary accent
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = CareCapsuleLightGray,    // card & nav surfaces
    onSurfaceVariant = Color.Black,
    primaryContainer = CareCapsuleGreen,      // selected states (fixes purple)
    onPrimaryContainer = Color.White,
    secondaryContainer = CareCapsuleDarkGreen,
    onSecondaryContainer = Color.White,
    tertiary = CareCapsuleGreen               // extra accents
)

// ✅ Dark Theme Colors
private val DarkColors = darkColorScheme(
    primary = CareCapsuleGreen,
    onPrimary = Color.White,
    secondary = CareCapsuleDarkGreen,
    onSecondary = Color.White,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    surfaceVariant = CareCapsuleGreen.copy(alpha = 0.2f),
    onSurfaceVariant = Color.White,
    primaryContainer = CareCapsuleGreen,
    onPrimaryContainer = Color.White,
    secondaryContainer = CareCapsuleDarkGreen,
    onSecondaryContainer = Color.White,
    tertiary = CareCapsuleGreen
)

@Composable
fun CareCapsuleTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}

/**
 * Standard screen container used by all top-level screens:
 * - fills whole screen
 * - background = theme background
 * - left/right padding = 20.dp
 * - top padding = 55.dp
 */
@Composable
fun ScreenContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = ScreenHorizontalPadding,
                end = ScreenHorizontalPadding,
                top = ScreenTopPadding,
                bottom = 16.dp
            ),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
        content = content
    )
}