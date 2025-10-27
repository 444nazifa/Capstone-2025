package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.api.AuthApiService
import com.example.myapplication.data.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileUpdateState {
    object Idle : ProfileUpdateState()
    object Loading : ProfileUpdateState()
    object Success : ProfileUpdateState()
    data class Error(val message: String) : ProfileUpdateState()
}

class ProfileViewModel(
    private val authApiService: AuthApiService = AuthApiService()
) : ViewModel() {

    private val _updateState = MutableStateFlow<ProfileUpdateState>(ProfileUpdateState.Idle)
    val updateState: StateFlow<ProfileUpdateState> = _updateState.asStateFlow()

    fun updateProfile(
        token: String,
        name: String,
        email: String,
        dateOfBirth: String,
        phone: String? = null
    ) {
        _updateState.value = ProfileUpdateState.Loading
        viewModelScope.launch {
            val result = authApiService.updateProfile(token, name, email, dateOfBirth, phone)
            _updateState.value = result.fold(
                onSuccess = { response ->
                    // Update the session with the new user data
                    response.user?.let { updatedUser ->
                        UserSession.updateUser(updatedUser)
                    }
                    ProfileUpdateState.Success
                },
                onFailure = { error ->
                    ProfileUpdateState.Error(error.message ?: "Failed to update profile")
                }
            )
        }
    }

    fun clearState() {
        _updateState.value = ProfileUpdateState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        authApiService.close()
    }
}
