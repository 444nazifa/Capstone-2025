package com.example.myapplication

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage

@Composable
actual fun AsyncImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(imageUrl) {
        try {
            val client = HttpClient()
            val response: HttpResponse = client.get(imageUrl)
            val bytes = response.readBytes()
            imageBitmap = SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
        } catch (e: Exception) {
            println("Failed to load image: ${e.message}")
        }
    }

    imageBitmap?.let { bitmap ->
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

