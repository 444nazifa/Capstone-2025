package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.api.AuthApiService
import com.example.myapplication.data.AuthState
import com.example.myapplication.data.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authApiService: AuthApiService = AuthApiService()
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError.asStateFlow()

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> = _passwordError.asStateFlow()

    fun login(email: String, password: String) {
        // Validate inputs
        var hasError = false

        if (email.isBlank()) {
            _emailError.value = "Email is required"
            hasError = true
        } else if (!isValidEmail(email)) {
            _emailError.value = "Please enter a valid email"
            hasError = true
        } else {
            _emailError.value = null
        }

        if (password.isBlank()) {
            _passwordError.value = "Password is required"
            hasError = true
        } else if (password.length < 6) {
            _passwordError.value = "Password must be at least 8 characters"
            hasError = true
        } else {
            _passwordError.value = null
        }

        if (hasError) return

        // Make API call
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authApiService.login(email, password)
            _authState.value = result.fold(
                onSuccess = { response ->
                    // Store user data in session
                    UserSession.login(response.user!!, response.token!!)
                    AuthState.Success(response.user!!, response.token!!)
                },
                onFailure = { error ->
                    AuthState.Error(error.message ?: "Login failed")
                }
            )
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 8
    }

    fun clearError() {
        _authState.value = AuthState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        authApiService.close()
    }
}
