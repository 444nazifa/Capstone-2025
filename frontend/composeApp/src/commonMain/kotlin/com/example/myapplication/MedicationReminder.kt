package com.example.myapplication

import androidx.compose.ui.graphics.Color

data class MedicationReminder(
    val id: Int,  // UI ID (for hashCode compatibility with existing code)
    val medicationId: String,  // Real backend medication UUID
    val scheduleId: String,  // Real backend schedule UUID
    val name: String,
    val dosage: String,
    val time: String,
    val instructions: String? = null,
    val taken: Boolean = false,
    val frequency: String = "Every day",
    val color: Color,
    val daysOfWeek: List<Int> = listOf(0, 1, 2, 3, 4, 5, 6)  // Which days this medication is scheduled
)

