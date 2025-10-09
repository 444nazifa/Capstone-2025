package com.example.myapplication

import kotlinx.datetime.LocalDateTime

data class MedicationReminder(
    val id: String,
    val name: String,
    val dosage: String,
    val time: String,
    val taken: Boolean = false,
    val priority: Priority,
    val instructions: String? = null
) {
    enum class Priority {
        HIGH, MEDIUM, LOW
    }
}

data class DaySchedule(
    val date: LocalDateTime,
    val medications: List<MedicationReminder>
)
