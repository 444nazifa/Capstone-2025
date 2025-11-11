package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.api.AuthApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(
    private val authApiService: AuthApiService = AuthApiService()
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun sendResetEmail(email: String) {
        if (email.isBlank()) {
            _errorMessage.value = "Email is required"
            return
        }

        _isLoading.value = true
        _successMessage.value = null
        _errorMessage.value = null

        viewModelScope.launch {
            val result = authApiService.forgotPassword(email)
            _isLoading.value = false

            result.fold(
                onSuccess = { message ->
                    _successMessage.value = message
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Failed to send reset email"
                }
            )
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        // Don't clear success message when user types
    }

    override fun onCleared() {
        super.onCleared()
        authApiService.close()
    }
}
