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
    private val medicationApi: MedicationApiService = MedicationApiService.getInstance(),
    private val secureStorage: SecureStorage,
    private val onMedicationDeleted: (() -> Unit)? = null
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

    fun updateMedication(updatedMedication: UserMedication, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _error.value = null

            val token = secureStorage.getToken()
            if (token.isNullOrEmpty()) {
                _error.value = "Not authenticated"
                onComplete(false)
                return@launch
            }

            // Build the update map with all fields that can be updated
            val updates = mutableMapOf<String, Any>()

            updates["medication_name"] = updatedMedication.medicationName
            updates["dosage"] = updatedMedication.dosage
            updates["frequency"] = updatedMedication.frequency
            updates["start_date"] = updatedMedication.startDate

            // Optional fields - only include if not null
            updatedMedication.doctorName?.let { updates["doctor_name"] = it }
            updatedMedication.pharmacyName?.let { updates["pharmacy_name"] = it }
            updatedMedication.instructions?.let { updates["instructions"] = it }
            updatedMedication.quantityTotal?.let { updates["quantity_total"] = it }
            updatedMedication.quantityRemaining?.let { updates["quantity_remaining"] = it }
            updatedMedication.refillReminderDays?.let { updates["refill_reminder_days"] = it }

            medicationApi.updateMedication(token, updatedMedication.id, updates)
                .onSuccess { updated ->
                    // Update local state with the returned medication
                    _medications.value = _medications.value.map { med ->
                        if (med.id == updated.id) updated else med
                    }
                    filterMedications(_searchQuery.value)

                    // Reload summary to update counts
                    medicationApi.getMedicationSummary(token)
                        .onSuccess { summary ->
                            _summary.value = summary
                        }

                    onComplete(true)
                }
                .onFailure { exception ->
                    _error.value = exception.message
                    onComplete(false)
                }
        }
    }

    fun deleteMedication(medicationId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _error.value = null

            val token = secureStorage.getToken()
            if (token.isNullOrEmpty()) {
                _error.value = "Not authenticated"
                onComplete(false)
                return@launch
            }

            medicationApi.deleteMedication(token, medicationId)
                .onSuccess {
                    // Remove from local state
                    _medications.value = _medications.value.filter { it.id != medicationId }
                    filterMedications(_searchQuery.value)

                    // Reload summary to update counts
                    medicationApi.getMedicationSummary(token)
                        .onSuccess { summary ->
                            _summary.value = summary
                        }

                    // Notify home screen to refresh
                    onMedicationDeleted?.invoke()

                    onComplete(true)
                }
                .onFailure { exception ->
                    _error.value = exception.message
                    onComplete(false)
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        medicationApi.close()
    }
}
