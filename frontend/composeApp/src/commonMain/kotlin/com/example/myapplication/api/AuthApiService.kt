package com.example.myapplication.api

import com.example.myapplication.data.AuthResponse
import com.example.myapplication.data.LoginRequest
import com.example.myapplication.data.RegisterRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class AuthApiService(
    private val baseUrl: String = "https://backend-ts-theta.vercel.app"
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = client.post("$baseUrl/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email, password))
            }

            val authResponse: AuthResponse = response.body()

            if (response.status.isSuccess() && authResponse.success && authResponse.user != null && authResponse.token != null) {
                Result.success(authResponse)
            } else {
                val errorMessage = authResponse.errors?.joinToString(", ")
                    ?: authResponse.message
                    ?: "Login failed"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        dateOfBirth: String
    ): Result<AuthResponse> {
        return try {
            val response = client.post("$baseUrl/api/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest(name, email, password, dateOfBirth))
            }

            val authResponse: AuthResponse = response.body()

            if (response.status.isSuccess() && authResponse.success && authResponse.user != null && authResponse.token != null) {
                Result.success(authResponse)
            } else {
                val errorMessage = authResponse.errors?.joinToString(", ")
                    ?: authResponse.message
                    ?: "Registration failed"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun getProfile(token: String): Result<AuthResponse> {
        return try {
            val response = client.get("$baseUrl/api/auth/profile") {
                header("Authorization", "Bearer $token")
            }

            val authResponse: AuthResponse = response.body()

            if (response.status.isSuccess() && authResponse.success && authResponse.user != null) {
                Result.success(authResponse)
            } else {
                val errorMessage = authResponse.errors?.joinToString(", ")
                    ?: authResponse.message
                    ?: "Failed to fetch profile"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun updateProfile(
        token: String,
        name: String,
        email: String,
        dateOfBirth: String,
        phone: String? = null
    ): Result<AuthResponse> {
        return try {
            val bodyMap = mutableMapOf(
                "name" to name,
                "email" to email,
                "date_of_birth" to dateOfBirth
            )
            if (phone != null) {
                bodyMap["phone"] = phone
            }

            val response = client.put("$baseUrl/api/auth/profile") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                setBody(bodyMap)
            }

            val authResponse: AuthResponse = response.body()

            if (response.status.isSuccess() && authResponse.success && authResponse.user != null) {
                Result.success(authResponse)
            } else {
                val errorMessage = authResponse.errors?.joinToString(", ")
                    ?: authResponse.message
                    ?: "Update failed"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun forgotPassword(email: String): Result<String> {
        return try {
            val response = client.post("$baseUrl/api/auth/forgot-password") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("email" to email))
            }

            val authResponse: AuthResponse = response.body()

            if (response.status.isSuccess() && authResponse.success) {
                Result.success(authResponse.message ?: "Password reset email sent")
            } else {
                val errorMessage = authResponse.errors?.joinToString(", ")
                    ?: authResponse.message
                    ?: "Failed to send password reset email"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun verifyResetToken(token: String): Result<String> {
        return try {
            val response = client.post("$baseUrl/api/auth/verify-reset-token") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("token" to token))
            }

            val authResponse: AuthResponse = response.body()

            if (response.status.isSuccess() && authResponse.success) {
                Result.success(authResponse.message ?: "Token is valid")
            } else {
                val errorMessage = authResponse.errors?.joinToString(", ")
                    ?: authResponse.message
                    ?: "Invalid or expired token"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun resetPassword(token: String, password: String): Result<String> {
        return try {
            val response = client.post("$baseUrl/api/auth/reset-password") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "token" to token,
                    "password" to password
                ))
            }

            val authResponse: AuthResponse = response.body()

            if (response.status.isSuccess() && authResponse.success) {
                Result.success(authResponse.message ?: "Password has been reset successfully")
            } else {
                val errorMessage = authResponse.errors?.joinToString(", ")
                    ?: authResponse.message
                    ?: "Failed to reset password"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    fun close() {
        client.close()
    }
}
