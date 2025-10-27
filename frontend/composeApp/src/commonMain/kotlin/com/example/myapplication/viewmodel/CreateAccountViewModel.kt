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
import kotlinx.datetime.*

class CreateAccountViewModel(
    private val authApiService: AuthApiService = AuthApiService()
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError.asStateFlow()

    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError.asStateFlow()

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> = _passwordError.asStateFlow()

    private val _dobError = MutableStateFlow<String?>(null)
    val dobError: StateFlow<String?> = _dobError.asStateFlow()

    fun register(name: String, email: String, password: String, dateOfBirth: String) {
        // Validate inputs
        var hasError = false

        if (name.isBlank()) {
            _nameError.value = "Name is required"
            hasError = true
        } else {
            _nameError.value = null
        }

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
        } else if (!isValidPassword(password)) {
            _passwordError.value = "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character"
            hasError = true
        } else {
            _passwordError.value = null
        }

        val parsedDob = parseDobOrNull(dateOfBirth)
        if (dateOfBirth.isBlank()) {
            _dobError.value = "Date of birth is required"
            hasError = true
        } else if (parsedDob == null) {
            _dobError.value = "Please use MM/DD/YYYY format"
            hasError = true
        } else if (!isReasonableDob(parsedDob)) {
            _dobError.value = "Please enter a valid date of birth (age ≤ 120, past date)"
            hasError = true
        } else {
            _dobError.value = null
        }

        if (hasError) return

        // Convert DOB to YYYY-MM-DD format for the API
        val apiDateFormat = parsedDob?.let {
            "${it.year}-${it.monthNumber.toString().padStart(2, '0')}-${it.dayOfMonth.toString().padStart(2, '0')}"
        } ?: return

        // Make API call
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authApiService.register(name, email, password, apiDateFormat)
            _authState.value = result.fold(
                onSuccess = { response ->
                    // Store user data in session
                    UserSession.login(response.user!!, response.token!!)
                    AuthState.Success(response.user!!, response.token!!)
                },
                onFailure = { error ->
                    AuthState.Error(error.message ?: "Registration failed")
                }
            )
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val re = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return re.matches(email.trim())
    }

    private fun isValidPassword(password: String): Boolean {
        if (password.length < 8) return false

        val hasLowercase = password.any { it.isLowerCase() }
        val hasUppercase = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { it in "@$!%*?&" }

        return hasLowercase && hasUppercase && hasDigit && hasSpecialChar
    }

    private fun parseDobOrNull(s: String): LocalDate? {
        val parts = s.trim().split("/")
        if (parts.size != 3) return null
        val (mmS, ddS, yyyyS) = parts
        val mm = mmS.toIntOrNull() ?: return null
        val dd = ddS.toIntOrNull() ?: return null
        val yyyy = yyyyS.toIntOrNull() ?: return null
        return try { LocalDate(yyyy, mm, dd) } catch (_: Throwable) { null }
    }

    private fun isReasonableDob(dob: LocalDate): Boolean {
        val tz = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(tz).date
        if (dob >= today) return false
        val years = today.year - dob.year - if (
            today.monthNumber < dob.monthNumber ||
            (today.monthNumber == dob.monthNumber && today.dayOfMonth < dob.dayOfMonth)
        ) 1 else 0
        return years in 0..120
    }

    fun clearError() {
        _authState.value = AuthState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        authApiService.close()
    }
}
