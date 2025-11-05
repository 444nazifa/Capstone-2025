package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ForgotPasswordViewModel : ViewModel() {

    // Stores the user's email input
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    // UI state tracking
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSent = MutableStateFlow(false)
    val isSent: StateFlow<Boolean> = _isSent

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    /** Called whenever the user types into the email box */
    fun setEmail(value: String) {
        _email.value = value
    }

    /** Clears previous banners or errors when user types again */
    fun clearBanners() {
        _errorMessage.value = null
        _isSent.value = false
    }

    /** Handles the forgot password action (placeholder for API later) */
    fun requestReset() {
        val emailValue = _email.value.trim()

        // basic validation
        if (emailValue.isEmpty()) {
            _errorMessage.value = "Please enter your email"
            return
        }

        // Simulate loading state
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            // ─── PLACEHOLDER: connect API here later ───
            // Example:
            // val result = authApiService.requestPasswordReset(emailValue)
            // result.fold(
            //     onSuccess = { _isSent.value = true },
            //     onFailure = { _errorMessage.value = it.message ?: "Something went wrong" }
            // )

            // Temporary mock behavior
            _isSent.value = true
            _isLoading.value = false
        }
    }
}
