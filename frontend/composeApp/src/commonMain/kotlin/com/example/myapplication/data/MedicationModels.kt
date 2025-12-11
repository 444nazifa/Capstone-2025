package com.example.myapplication.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserMedication(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("medication_name") val medicationName: String,
    val dosage: String,
    @SerialName("set_id") val setId: String? = null,
    val ndc: String? = null,
    val instructions: String? = null,
    val frequency: String,
    @SerialName("doctor_name") val doctorName: String? = null,
    @SerialName("pharmacy_name") val pharmacyName: String? = null,
    @SerialName("pharmacy_location") val pharmacyLocation: String? = null,
    @SerialName("quantity_total") val quantityTotal: Int? = null,
    @SerialName("quantity_remaining") val quantityRemaining: Int? = null,
    @SerialName("supply_remaining_percentage") val supplyRemainingPercentage: Double? = null,
    @SerialName("next_refill_date") val nextRefillDate: String? = null,
    @SerialName("refill_reminder_days") val refillReminderDays: Int? = null,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String? = null,
    val color: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val schedules: List<MedicationSchedule>? = null
)

@Serializable
data class MedicationSchedule(
    val id: String,
    @SerialName("user_medication_id") val userMedicationId: String,
    @SerialName("scheduled_time") val scheduledTime: String,
    @SerialName("days_of_week") val daysOfWeek: List<Int>,
    @SerialName("is_enabled") val isEnabled: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class MedicationHistory(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("user_medication_id") val userMedicationId: String,
    @SerialName("medication_schedule_id") val medicationScheduleId: String? = null,
    @SerialName("scheduled_at") val scheduledAt: String,
    @SerialName("taken_at") val takenAt: String? = null,
    val status: String,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class MedicationSummary(
    @SerialName("total_active") val totalActive: Int,
    @SerialName("low_supply_count") val lowSupplyCount: Int,
    @SerialName("upcoming_refills") val upcomingRefills: Int,
    @SerialName("medications_due_today") val medicationsDueToday: Int,
    @SerialName("adherence_rate") val adherenceRate: Int? = null
)

@Serializable
data class CreateMedicationRequest(
    @SerialName("medication_name") val medicationName: String,
    val dosage: String,
    @SerialName("set_id") val setId: String? = null,
    val ndc: String? = null,
    val instructions: String? = null,
    val frequency: String = "Every day",
    @SerialName("doctor_name") val doctorName: String? = null,
    @SerialName("pharmacy_name") val pharmacyName: String? = null,
    @SerialName("pharmacy_location") val pharmacyLocation: String? = null,
    @SerialName("quantity_total") val quantityTotal: Int? = null,
    @SerialName("quantity_remaining") val quantityRemaining: Int? = null,
    @SerialName("next_refill_date") val nextRefillDate: String? = null,
    @SerialName("refill_reminder_days") val refillReminderDays: Int? = 7,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    val color: String? = "#4CAF50",
    val schedules: List<CreateScheduleRequest>? = null
)

@Serializable
data class CreateScheduleRequest(
    @SerialName("scheduled_time") val scheduledTime: String,
    @SerialName("days_of_week") val daysOfWeek: List<Int> = listOf(0, 1, 2, 3, 4, 5, 6),
    @SerialName("is_enabled") val isEnabled: Boolean = true
)

@Serializable
data class UpdateMedicationRequest(
    @SerialName("medication_name") val medicationName: String? = null,
    val dosage: String? = null,
    val instructions: String? = null,
    val frequency: String? = null,
    @SerialName("doctor_name") val doctorName: String? = null,
    @SerialName("pharmacy_name") val pharmacyName: String? = null,
    @SerialName("pharmacy_location") val pharmacyLocation: String? = null,
    @SerialName("quantity_total") val quantityTotal: Int? = null,
    @SerialName("quantity_remaining") val quantityRemaining: Int? = null,
    @SerialName("next_refill_date") val nextRefillDate: String? = null,
    @SerialName("refill_reminder_days") val refillReminderDays: Int? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    @SerialName("end_date") val endDate: String? = null,
    val color: String? = null
)

@Serializable
data class MarkMedicationRequest(
    val status: String,
    @SerialName("taken_at") val takenAt: String? = null,
    val notes: String? = null
)

@Serializable
data class MedicationResponse(
    val success: Boolean,
    val message: String,
    val medication: UserMedication? = null,
    val medications: List<UserMedication>? = null,
    val summary: MedicationSummary? = null,
    val error: String? = null
)

@Serializable
data class MedicationHistoryResponse(
    val success: Boolean,
    val message: String? = null,
    val history: List<MedicationHistory>? = null,
    val total: Int? = null,
    val error: String? = null
)

// DailyMed Search Models
@Serializable
data class MedicationSearchResult(
    @SerialName("setId") val setId: String,
    val title: String,
    val ndc: List<String>? = null,
    val labeler: String? = null,
    val published: String? = null,
    val updated: String? = null,
    @SerialName("image") val imageUrl: String? = null
)

@Serializable
data class MedicationSearchResponse(
    val success: Boolean,
    val data: List<MedicationSearchResult>? = null,
    val total: Int? = null,
    val error: String? = null
)

