package com.example.myapplication

import androidx.compose.ui.graphics.Color

data class MedicationReminder(
    val id: Int,
    val name: String,
    val dosage: String,
    val time: String,
    val instructions: String? = null,
    val taken: Boolean = false,
    val frequency: String = "Every day",
    val color: Color // 👈 add this
)

