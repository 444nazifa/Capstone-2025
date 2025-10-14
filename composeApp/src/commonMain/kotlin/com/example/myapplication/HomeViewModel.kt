package com.example.myapplication

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.*

class HomeViewModel : ViewModel() {

    // Current date stored as a string (e.g., "Mon 14")
    private val _currentDate = MutableStateFlow(getTodayString())
    val currentDate: StateFlow<String> = _currentDate

    // Medication list
    private val _medications = MutableStateFlow(
        listOf(
            MedicationReminder(
                id = 1,
                name = "Metformin",
                dosage = "500mg",
                time = "8:00 AM",
                instructions = "Take with breakfast",
                taken = false
            ),
            MedicationReminder(
                id = 2,
                name = "Lisinopril",
                dosage = "20mg",
                time = "12:00 PM",
                instructions = "After lunch",
                taken = false
            ),
            MedicationReminder(
                id = 3,
                name = "Atorvastatin",
                dosage = "10mg",
                time = "9:00 PM",
                instructions = "Before bed",
                taken = true
            )
        )
    )
    val medications: StateFlow<List<MedicationReminder>> = _medications

    // Returns a list like ["Sun 12", "Mon 13", ..., "Sat 18"]
    fun getWeekDays(): List<String> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val startOfWeek = today.minus(today.dayOfWeek.ordinal.toLong(), DateTimeUnit.DAY)
        return (0..6).map { offset ->
            val date = startOfWeek.plus(offset.toLong(), DateTimeUnit.DAY)
            "${getDayAbbrev(date.dayOfWeek)} ${date.dayOfMonth}"
        }
    }

    // Checks if the given day string matches today
    fun isToday(day: String): Boolean {
        return day == getTodayString()
    }

    // Moves the current date forward or backward by 1 week
    fun navigateWeek(direction: Int) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val newDate = if (direction > 0)
            today.plus(7, DateTimeUnit.DAY)
        else
            today.minus(7, DateTimeUnit.DAY)
        _currentDate.value = "${getDayAbbrev(newDate.dayOfWeek)} ${newDate.dayOfMonth}"
    }

    // Toggle the medication's "taken" state
    fun toggleMedication(id: Int) {
        _medications.value = _medications.value.map {
            if (it.id == id) it.copy(taken = !it.taken) else it
        }
    }

    // Helper to get today's formatted string
    private fun getTodayString(): String {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return "${getDayAbbrev(today.dayOfWeek)} ${today.dayOfMonth}"
    }

    // Abbreviate days (Sun, Mon, Tue, etc.)
    private fun getDayAbbrev(day: DayOfWeek): String {
        return when (day) {
            DayOfWeek.MONDAY -> "Mon"
            DayOfWeek.TUESDAY -> "Tue"
            DayOfWeek.WEDNESDAY -> "Wed"
            DayOfWeek.THURSDAY -> "Thu"
            DayOfWeek.FRIDAY -> "Fri"
            DayOfWeek.SATURDAY -> "Sat"
            DayOfWeek.SUNDAY -> "Sun"
            else -> "?" // fallback for safety (required by multiplatform)
        }
    }

}
