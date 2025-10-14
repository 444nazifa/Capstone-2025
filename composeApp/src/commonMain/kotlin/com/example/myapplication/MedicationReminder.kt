package com.example.myapplication

data class MedicationReminder(
    val id: Int,
    val name: String,
    val dosage: String,
    val time: String,
    val instructions: String?,
    val taken: Boolean
)
