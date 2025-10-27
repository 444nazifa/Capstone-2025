package com.example.myapplication.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.*

class HomeViewModel : ViewModel() {

    // Current date stored as a string (e.g., "Mon 14")
    private val _currentDate = MutableStateFlow(getTodayString())
    val currentDate: StateFlow<String> = _currentDate

    // Tracks which week we’re viewing relative to today
    private val _weekOffset = MutableStateFlow(0)
    val weekOffset: StateFlow<Int> = _weekOffset

    // Medication list
    private val _medications = MutableStateFlow(
        listOf(
            MedicationReminder(
                id = 1,
                name = "Metformin",
                dosage = "500mg",
                time = "8:00 AM",
                instructions = "Take with breakfast",
                taken = false,
                frequency = "NONE",
                color = Color(0xFFFFB6C1) // pink
            ),
            MedicationReminder(
                id = 2,
                name = "Lisinopril",
                dosage = "20mg",
                time = "12:00 PM",
                instructions = "After lunch",
                taken = false,
                frequency = "Every 2 days",
                color = Color(0xFFFFA000) // amber
            ),
            MedicationReminder(
                id = 3,
                name = "Atorvastatin",
                dosage = "10mg",
                time = "9:00 PM",
                instructions = "Before bed",
                taken = true,
                frequency = "Every week",
                color = Color(0xFF7B1FA2) // purple
            ),
            MedicationReminder(
                id = 4,
                name = "Vitamin D3",
                dosage = "2000 IU",
                time = "7:30 AM",
                instructions = "Take with a full glass of water",
                taken = false,
                frequency = "Every 3 days",
                color = Color(0xFF2196F3) // blue
            ),
            MedicationReminder(
                id = 5,
                name = "Ibuprofen",
                dosage = "400mg",
                time = "2:00 PM",
                instructions = "Take after a meal if needed for pain",
                taken = false,
                frequency = "Every 4 days",
                color = Color(0xFFD32F2F) // red
            ),
            MedicationReminder(
                id = 6,
                name = "Tylenol",
                dosage = "200mg",
                time = "1:00 PM",
                instructions = "Take after a meal if needed for pain",
                taken = false,
                frequency = "Every day",
                color = Color(0xFFFF4081) // pink
            )
        )
    )

    val medications: StateFlow<List<MedicationReminder>> = _medications

    fun getMedicationsForDay(day: LocalDate): List<MedicationReminder> {
        // Mock logic for now: “Every day” meds show every day,
        // “Every 2 days” every other day, “Every week” on Sunday, etc.
        return _medications.value.filter { med ->
            when (med.frequency) {
                " " -> true
                "Every 2 days" -> day.dayOfMonth % 2 == 0
                "Every 3 days" -> day.dayOfMonth % 3 == 0
                "Every 4 days" -> day.dayOfMonth % 4 == 0
                "Every week" -> day.dayOfWeek == DayOfWeek.SUNDAY
                else -> false
            }
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
        _medications.value = _medications.value.map {
            if (it.id == id) it.copy(taken = !it.taken) else it
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
}
