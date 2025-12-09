package com.example.myapplication

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.client.request.setBody


@Serializable
data class ChatRequest(
    val message: String,
    val reminders: List<ReminderPayload>? = null
)

@Serializable
data class ReminderPayload(
    val name: String? = null,
    val time: String? = null,
    val notes: String? = null
)

@Serializable
data class ChatResponse(
    val reply: String,
    val source: String? = null
)

object ChatApi {

    // Shared Ktor client – engine is provided per platform
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = false
                }
            )
        }
    }

    suspend fun sendMessage(
        message: String,
        reminders: List<ReminderPayload>? = null
    ): ChatResponse {
        val url = backendBaseUrl() //
        val response = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(message, reminders))
        }
        return response.body()
    }
}
