package com.example.myapplication.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.MedicationReminder
import com.example.myapplication.api.MedicationApiService
import com.example.myapplication.data.UserMedication
import com.example.myapplication.storage.SecureStorage
import com.example.myapplication.storage.getToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.*

class HomeViewModel(
    private val medicationApi: MedicationApiService = MedicationApiService.getInstance(),
    private val secureStorage: SecureStorage
) : ViewModel() {

    // Current date stored as a string (e.g., "Mon 14")
    private val _currentDate = MutableStateFlow(getTodayString())
    val currentDate: StateFlow<String> = _currentDate

    // Tracks which week we're viewing relative to today
    private val _weekOffset = MutableStateFlow(0)
    val weekOffset: StateFlow<Int> = _weekOffset

    // Medication list
    private val _medications = MutableStateFlow<List<MedicationReminder>>(emptyList())
    val medications: StateFlow<List<MedicationReminder>> = _medications

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadMedications()
    }

    fun loadMedications() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val token = secureStorage.getToken()
            if (token.isNullOrEmpty()) {
                _error.value = "Not authenticated"
                _isLoading.value = false
                return@launch
            }

            // Get today's date range for history query
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val startOfDay = today.date.atTime(0, 0, 0).toInstant(TimeZone.currentSystemDefault()).toString()
            val endOfDay = today.date.atTime(23, 59, 59).toInstant(TimeZone.currentSystemDefault()).toString()

            // Load medications and history in parallel
            val medicationsResult = medicationApi.getAllMedications(token, activeOnly = true)
            val historyResult = medicationApi.getMedicationHistory(
                token = token,
                startDate = startOfDay,
                endDate = endOfDay,
                status = "taken"
            )

            medicationsResult.onSuccess { userMedications ->
                historyResult.onSuccess { history ->
                    _medications.value = convertToMedicationReminders(userMedications, history)
                }.onFailure {
                    // If history fetch fails, still load medications but without taken status
                    _medications.value = convertToMedicationReminders(userMedications, emptyList())
                }
            }.onFailure { exception ->
                _error.value = exception.message
            }

            _isLoading.value = false
        }
    }

    private fun convertToMedicationReminders(
        userMedications: List<UserMedication>,
        history: List<com.example.myapplication.data.MedicationHistory> = emptyList()
    ): List<MedicationReminder> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val todayDayOfWeek = today.dayOfWeek.ordinal // 0 = Sunday

        // Create a set of medication IDs that have been taken today
        val takenMedicationIds = history
            .filter { it.status == "taken" }
            .map { it.userMedicationId }
            .toSet()

        return userMedications.flatMap { medication ->
            val schedules = medication.schedules ?: emptyList()

            // Filter schedules for today
            val todaySchedules = schedules.filter { schedule ->
                schedule.isEnabled && schedule.daysOfWeek.contains(todayDayOfWeek)
            }

            // Create a MedicationReminder for each schedule
            todaySchedules.map { schedule ->
                MedicationReminder(
                    id = "${medication.id}-${schedule.id}".hashCode(),
                    medicationId = medication.id,  // Store real medication ID
                    scheduleId = schedule.id,  // Store real schedule ID
                    name = medication.medicationName,
                    dosage = medication.dosage,
                    time = formatTime(schedule.scheduledTime),
                    instructions = medication.instructions,
                    taken = takenMedicationIds.contains(medication.id), // Check if medication was taken today
                    frequency = medication.frequency,
                    color = parseColor(medication.color),
                    daysOfWeek = schedule.daysOfWeek  // Store which days this medication is scheduled
                )
            }
        }.sortedBy { it.time }
    }

    private fun formatTime(time: String): String {
        // Convert "08:00:00" or "08:00" to "8:00 AM"
        val parts = time.split(":")
        if (parts.size >= 2) {
            val hour = parts[0].toIntOrNull() ?: 0
            val minute = parts[1]
            val period = if (hour < 12) "AM" else "PM"
            val hour12 = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            return "$hour12:$minute $period"
        }
        return time
    }

    private fun parseColor(hexColor: String): Color {
        return try {
            val cleanHex = hexColor.removePrefix("#")
            val colorInt = cleanHex.toLong(16)
            Color(0xFF000000 or colorInt)
        } catch (e: Exception) {
            Color(0xFF4CAF50) // Default green
        }
    }

    fun getMedicationsForDay(day: LocalDate): List<MedicationReminder> {
        val dayOfWeek = day.dayOfWeek.ordinal  // 0=Sunday, 1=Monday, etc.

        // Filter medications that are scheduled for this specific day
        return _medications.value.filter { med ->
            med.daysOfWeek.contains(dayOfWeek)
        }
    }

    fun getWeekDays(): List<String> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val targetWeekStart = today
            .plus(_weekOffset.value * 7, DateTimeUnit.DAY)
            .minus(today.dayOfWeek.ordinal.toLong(), DateTimeUnit.DAY)

        return (0..6).map { offset ->
            val date = targetWeekStart.plus(offset.toLong(), DateTimeUnit.DAY)
            "${getDayAbbrev(date.dayOfWeek)} ${date.dayOfMonth}"
        }
    }

    fun isToday(day: String): Boolean {
        return day == getTodayString()
    }

    fun navigateWeek(direction: Int) {
        _weekOffset.value += direction
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val newDate = today.plus(_weekOffset.value * 7, DateTimeUnit.DAY)
        _currentDate.value = "${getDayAbbrev(newDate.dayOfWeek)} ${newDate.dayOfMonth}"
    }

    fun toggleMedication(id: Int) {
        val medication = _medications.value.find { it.id == id } ?: return

        viewModelScope.launch {
            val token = secureStorage.getToken()
            if (token.isNullOrEmpty()) return@launch

            val newStatus = !medication.taken

            // Update locally first for immediate UI feedback
            _medications.value = _medications.value.map {
                if (it.id == id) it.copy(taken = newStatus) else it
            }

            // Call backend to mark medication
            val status = if (newStatus) "taken" else "skipped"
            medicationApi.markMedicationTaken(
                token = token,
                medicationId = medication.medicationId,  // Use real medication UUID
                status = status
            ).onSuccess {
                // Successfully recorded in backend
                println("Medication marked as $status")
            }.onFailure { exception ->
                // Revert local change if backend call fails
                _medications.value = _medications.value.map {
                    if (it.id == id) it.copy(taken = !newStatus) else it
                }
                _error.value = "Failed to mark medication: ${exception.message}"
            }
        }
    }

    private fun getTodayString(): String {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return "${getDayAbbrev(today.dayOfWeek)} ${today.dayOfMonth}"
    }

    private fun getDayAbbrev(day: DayOfWeek): String {
        return when (day) {
            DayOfWeek.SUNDAY -> "Sun"
            DayOfWeek.MONDAY -> "Mon"
            DayOfWeek.TUESDAY -> "Tue"
            DayOfWeek.WEDNESDAY -> "Wed"
            DayOfWeek.THURSDAY -> "Thu"
            DayOfWeek.FRIDAY -> "Fri"
            DayOfWeek.SATURDAY -> "Sat"
            else -> "?"
        }
    }

    override fun onCleared() {
        super.onCleared()
        medicationApi.close()
    }
}
