package com.example.myapplication

import com.example.myapplication.MedicationReminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.*
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class HomeViewModel {
    private val _medications = MutableStateFlow<List<MedicationReminder>>(emptyList())
    val medications: StateFlow<List<MedicationReminder>> = _medications.asStateFlow()

    private val _currentDate = MutableStateFlow<LocalDateTime>(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))
    val currentDate: StateFlow<LocalDateTime> = _currentDate.asStateFlow()

    init {
        loadMedications()
    }

    private fun loadMedications() {
        // Sample data - in production, this would come from a repository
        _medications.value = listOf(
            MedicationReminder(
                id = "1",
                name = "Metformin",
                dosage = "500mg",
                time = "8:00 AM",
                taken = false,
                priority = MedicationReminder.Priority.HIGH,
                instructions = "Take with breakfast"
            ),
            MedicationReminder(
                id = "2",
                name = "Lisinopril",
                dosage = "10mg",
                time = "8:00 AM",
                taken = true,
                priority = MedicationReminder.Priority.HIGH,
                instructions = "Take on empty stomach"
            ),
            MedicationReminder(
                id = "3",
                name = "Vitamin D3",
                dosage = "1000 IU",
                time = "12:00 PM",
                taken = false,
                priority = MedicationReminder.Priority.MEDIUM,
                instructions = "Take with lunch"
            ),
            MedicationReminder(
                id = "4",
                name = "Omega-3",
                dosage = "1000mg",
                time = "6:00 PM",
                taken = false,
                priority = MedicationReminder.Priority.LOW,
                instructions = "Take with dinner"
            )
        )
    }

    fun toggleMedication(id: String) {
        _medications.update { currentList ->
            currentList.map { med ->
                if (med.id == id) med.copy(taken = !med.taken) else med
            }
        }
    }

    fun navigateWeek(forward: Boolean) {
        _currentDate.update { current ->
            if (forward) {
                current.date.plus(7, DateTimeUnit.DAY).atTime(current.hour, current.minute, current.second, current.nanosecond)
            } else {
                current.date.minus(7, DateTimeUnit.DAY).atTime(current.hour, current.minute, current.second, current.nanosecond)
            }
        }
    }

    fun getWeekDays(): List<LocalDate> {
        val current = _currentDate.value.date
        val dayOfWeek = current.dayOfWeek.ordinal
        val weekStart = current.minus(dayOfWeek, DateTimeUnit.DAY)

        return (0..6).map { offset ->
            weekStart.plus(offset, DateTimeUnit.DAY)
        }
    }

    fun isToday(date: LocalDate): Boolean {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return date == today
    }
}
