package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.api.AuthApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ResetPasswordViewModel(
    private val authApiService: AuthApiService = AuthApiService()
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isTokenValid = MutableStateFlow<Boolean?>(null)
    val isTokenValid: StateFlow<Boolean?> = _isTokenValid.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun verifyToken(token: String) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = authApiService.verifyResetToken(token)
            _isLoading.value = false

            result.fold(
                onSuccess = {
                    _isTokenValid.value = true
                },
                onFailure = { error ->
                    _isTokenValid.value = false
                    _errorMessage.value = error.message ?: "Invalid or expired token"
                }
            )
        }
    }

    fun resetPassword(token: String, password: String) {
        if (password.isBlank()) {
            _errorMessage.value = "Password is required"
            return
        }

        if (password.length < 8) {
            _errorMessage.value = "Password must be at least 8 characters"
            return
        }

        _isLoading.value = true
        _successMessage.value = null
        _errorMessage.value = null

        viewModelScope.launch {
            val result = authApiService.resetPassword(token, password)
            _isLoading.value = false

            result.fold(
                onSuccess = { message ->
                    _successMessage.value = message
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Failed to reset password"
                }
            )
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        // Don't clear success message
    }

    override fun onCleared() {
        super.onCleared()
        authApiService.close()
    }
}
