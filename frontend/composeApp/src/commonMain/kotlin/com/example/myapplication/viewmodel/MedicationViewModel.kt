package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.api.MedicationApiService
import com.example.myapplication.data.MedicationSummary
import com.example.myapplication.data.UserMedication
import com.example.myapplication.storage.SecureStorage
import com.example.myapplication.storage.getToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MedicationViewModel(
    private val medicationApi: MedicationApiService = MedicationApiService(),
    private val secureStorage: SecureStorage
) : ViewModel() {

    // Medication summary
    private val _summary = MutableStateFlow<MedicationSummary?>(null)
    val summary: StateFlow<MedicationSummary?> = _summary

    // All medications list
    private val _medications = MutableStateFlow<List<UserMedication>>(emptyList())
    val medications: StateFlow<List<UserMedication>> = _medications

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Filtered medications based on search
    private val _filteredMedications = MutableStateFlow<List<UserMedication>>(emptyList())
    val filteredMedications: StateFlow<List<UserMedication>> = _filteredMedications

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadMedicationData()
    }

    fun loadMedicationData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val token = secureStorage.getToken()
            if (token.isNullOrEmpty()) {
                _error.value = "Not authenticated"
                _isLoading.value = false
                return@launch
            }

            // Load summary
            medicationApi.getMedicationSummary(token)
                .onSuccess { summary ->
                    _summary.value = summary
                }
                .onFailure { exception ->
                    _error.value = exception.message
                }

            // Load all medications
            medicationApi.getAllMedications(token, activeOnly = true)
                .onSuccess { medications ->
                    _medications.value = medications
                    _filteredMedications.value = medications
                }
                .onFailure { exception ->
                    _error.value = exception.message
                }

            _isLoading.value = false
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterMedications(query)
    }

    private fun filterMedications(query: String) {
        _filteredMedications.value = if (query.isBlank()) {
            _medications.value
        } else {
            _medications.value.filter { medication ->
                medication.medicationName.contains(query, ignoreCase = true) ||
                medication.dosage.contains(query, ignoreCase = true)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        medicationApi.close()
    }
}
