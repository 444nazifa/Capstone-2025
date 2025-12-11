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

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSent = MutableStateFlow(false)
    val isSent: StateFlow<Boolean> = _isSent.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun setEmail(value: String) {
        _email.value = value
    }

    fun requestReset() {
        val emailValue = _email.value
        if (emailValue.isBlank()) {
            _errorMessage.value = "Email is required"
            return
        }

        _isLoading.value = true
        _isSent.value = false
        _errorMessage.value = null

        viewModelScope.launch {
            val result = authApiService.forgotPassword(emailValue)
            _isLoading.value = false

            result.fold(
                onSuccess = { message ->
                    _isSent.value = true
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Failed to send reset email"
                }
            )
        }
    }

    fun clearBanners() {
        _errorMessage.value = null
        _isSent.value = false
    }

    override fun onCleared() {
        super.onCleared()
        authApiService.close()
    }
}
