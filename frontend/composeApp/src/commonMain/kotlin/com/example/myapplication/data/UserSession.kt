package com.example.myapplication.data

import com.example.myapplication.storage.SecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Singleton to manage user session data across the app
 */
object UserSession {
    private const val KEY_USER = "user_data"
    private const val KEY_TOKEN = "auth_token"

    private var storage: SecureStorage? = null
    private val json = Json { ignoreUnknownKeys = true }

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _authToken = MutableStateFlow<String?>(null)
    val authToken: StateFlow<String?> = _authToken.asStateFlow()

    fun initialize(secureStorage: SecureStorage) {
        storage = secureStorage
        restoreSession()
    }

    private fun restoreSession() {
        val userJson = storage?.getString(KEY_USER)
        val token = storage?.getString(KEY_TOKEN)

        if (userJson != null && token != null) {
            try {
                val user = json.decodeFromString<User>(userJson)
                _currentUser.value = user
                _authToken.value = token
            } catch (e: Exception) {
                // If deserialization fails, clear corrupted data
                storage?.clear()
            }
        }
    }

    fun login(user: User, token: String) {
        _currentUser.value = user
        _authToken.value = token

        // Persist to storage
        storage?.saveString(KEY_USER, json.encodeToString(user))
        storage?.saveString(KEY_TOKEN, token)
    }

    fun updateUser(user: User) {
        _currentUser.value = user

        // Persist updated user to storage
        storage?.saveString(KEY_USER, json.encodeToString(user))
    }

    fun logout() {
        _currentUser.value = null
        _authToken.value = null

        // Clear persisted data
        storage?.clear()
    }

    fun isLoggedIn(): Boolean = _currentUser.value != null && _authToken.value != null
}
