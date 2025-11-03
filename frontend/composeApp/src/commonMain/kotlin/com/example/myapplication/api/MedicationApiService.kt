package com.example.myapplication.api

import com.example.myapplication.data.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class MedicationApiService private constructor(
    private val baseUrl: String = "https://backend-ts-theta.vercel.app"
) {
    companion object {
        @Volatile
        private var instance: MedicationApiService? = null

        fun getInstance(context: Any? = null): MedicationApiService {
            return instance ?: synchronized(this) {
                instance ?: MedicationApiService().also {
                    instance = it
                }
            }
        }
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun createMedication(
        token: String,
        request: CreateMedicationRequest
    ): Result<UserMedication> {
        return try {
            val response = client.post("$baseUrl/api/medications/user") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                setBody(request)
            }

            val medicationResponse: MedicationResponse = response.body()

            if (response.status.isSuccess() && medicationResponse.success && medicationResponse.medication != null) {
                Result.success(medicationResponse.medication)
            } else {
                Result.failure(Exception(medicationResponse.error ?: medicationResponse.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun getAllMedications(
        token: String,
        activeOnly: Boolean = false
    ): Result<List<UserMedication>> {
        return try {
            val url = if (activeOnly) {
                "$baseUrl/api/medications/user?active=true"
            } else {
                "$baseUrl/api/medications/user"
            }

            val response = client.get(url) {
                header("Authorization", "Bearer $token")
            }

            val medicationResponse: MedicationResponse = response.body()

            if (response.status.isSuccess() && medicationResponse.success) {
                Result.success(medicationResponse.medications ?: emptyList())
            } else {
                Result.failure(Exception(medicationResponse.error ?: medicationResponse.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun getMedication(
        token: String,
        medicationId: String
    ): Result<UserMedication> {
        return try {
            val response = client.get("$baseUrl/api/medications/user/$medicationId") {
                header("Authorization", "Bearer $token")
            }

            val medicationResponse: MedicationResponse = response.body()

            if (response.status.isSuccess() && medicationResponse.success && medicationResponse.medication != null) {
                Result.success(medicationResponse.medication)
            } else {
                Result.failure(Exception(medicationResponse.error ?: medicationResponse.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun updateMedication(
        token: String,
        medicationId: String,
        updates: Map<String, Any>
    ): Result<UserMedication> {
        return try {
            val response = client.put("$baseUrl/api/medications/user/$medicationId") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                setBody(updates)
            }

            val medicationResponse: MedicationResponse = response.body()

            if (response.status.isSuccess() && medicationResponse.success && medicationResponse.medication != null) {
                Result.success(medicationResponse.medication)
            } else {
                Result.failure(Exception(medicationResponse.error ?: medicationResponse.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun deleteMedication(
        token: String,
        medicationId: String
    ): Result<Unit> {
        return try {
            val response = client.delete("$baseUrl/api/medications/user/$medicationId") {
                header("Authorization", "Bearer $token")
            }

            val medicationResponse: MedicationResponse = response.body()

            if (response.status.isSuccess() && medicationResponse.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(medicationResponse.error ?: medicationResponse.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun getMedicationSummary(token: String): Result<MedicationSummary> {
        return try {
            val response = client.get("$baseUrl/api/medications/summary") {
                header("Authorization", "Bearer $token")
            }

            val medicationResponse: MedicationResponse = response.body()

            if (response.status.isSuccess() && medicationResponse.success && medicationResponse.summary != null) {
                Result.success(medicationResponse.summary)
            } else {
                Result.failure(Exception(medicationResponse.error ?: medicationResponse.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun markMedicationTaken(
        token: String,
        medicationId: String,
        status: String,
        notes: String? = null
    ): Result<MedicationHistory> {
        return try {
            val request = MarkMedicationRequest(
                status = status,
                notes = notes
            )

            val response = client.post("$baseUrl/api/medications/user/$medicationId/mark") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                setBody(request)
            }

            val historyResponse: MedicationHistoryResponse = response.body()

            if (response.status.isSuccess() && historyResponse.success && !historyResponse.history.isNullOrEmpty()) {
                Result.success(historyResponse.history.first())
            } else {
                Result.failure(Exception(historyResponse.error ?: historyResponse.message ?: "Failed to mark medication"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun getMedicationHistory(
        token: String,
        medicationId: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        status: String? = null,
        limit: Int = 50
    ): Result<List<MedicationHistory>> {
        return try {
            val urlParams = mutableListOf<String>()
            if (medicationId != null) urlParams.add("medication_id=$medicationId")
            if (startDate != null) urlParams.add("start_date=$startDate")
            if (endDate != null) urlParams.add("end_date=$endDate")
            if (status != null) urlParams.add("status=$status")
            urlParams.add("limit=$limit")

            val queryString = if (urlParams.isNotEmpty()) "?${urlParams.joinToString("&")}" else ""
            val url = "$baseUrl/api/medications/history$queryString"

            val response = client.get(url) {
                header("Authorization", "Bearer $token")
            }

            val historyResponse: MedicationHistoryResponse = response.body()

            if (response.status.isSuccess() && historyResponse.success) {
                Result.success(historyResponse.history ?: emptyList())
            } else {
                Result.failure(Exception(historyResponse.error ?: historyResponse.message ?: "Failed to fetch history"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun addSchedule(
        token: String,
        medicationId: String,
        schedule: CreateScheduleRequest
    ): Result<Unit> {
        return try {
            val response = client.post("$baseUrl/api/medications/user/$medicationId/schedules") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                setBody(schedule)
            }

            val medicationResponse: MedicationResponse = response.body()

            if (response.status.isSuccess() && medicationResponse.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(medicationResponse.error ?: medicationResponse.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun updateSchedule(
        token: String,
        scheduleId: String,
        updates: Map<String, Any>
    ): Result<Unit> {
        return try {
            val response = client.put("$baseUrl/api/medications/schedules/$scheduleId") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                setBody(updates)
            }

            val medicationResponse: MedicationResponse = response.body()

            if (response.status.isSuccess() && medicationResponse.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(medicationResponse.error ?: medicationResponse.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    suspend fun deleteSchedule(
        token: String,
        scheduleId: String
    ): Result<Unit> {
        return try {
            val response = client.delete("$baseUrl/api/medications/schedules/$scheduleId") {
                header("Authorization", "Bearer $token")
            }

            val medicationResponse: MedicationResponse = response.body()

            if (response.status.isSuccess() && medicationResponse.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(medicationResponse.error ?: medicationResponse.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }

    // NOTE: Not automatically called. Called from PushNotificationManager when enabling notifications.
    suspend fun registerDeviceToken(
        token: String,
        platform: String
    ) {
        val response = client.post("$baseUrl/api/device-tokens") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "token" to token,
                "platform" to platform
            ))
        }

        if (!response.status.isSuccess()) {
            throw Exception("Failed to register device token: ${response.status}")
        }
    }

    suspend fun unregisterDeviceToken(token: String) {
        val response = client.delete("$baseUrl/api/device-tokens/$token")

        if (!response.status.isSuccess()) {
            throw Exception("Failed to unregister device token: ${response.status}")
        }
    }

    fun close() {
        client.close()
    }
}
