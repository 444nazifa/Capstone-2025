package com.example.myapplication.data

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val date_of_birth: String
)

@Serializable
data class User(
    val id: String,
    val name: String,
    val email: String,
    val date_of_birth: String
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val user: User? = null,
    val token: String? = null,
    val errors: List<String>? = null
)

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User, val token: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
