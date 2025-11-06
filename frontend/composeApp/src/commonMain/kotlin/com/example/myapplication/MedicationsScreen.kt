package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.viewmodel.MedicationViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.example.myapplication.storage.createSecureStorage

@Composable
fun MedicationScreen(viewModel: MedicationViewModel, modifier: Modifier = Modifier) {
    val summary by viewModel.summary.collectAsState()
    val medications by viewModel.filteredMedications.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(modifier)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp)
                .padding(top = 10.dp)
        ) {
            // 🟢 Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Prescription",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        lineHeight = 36.sp
                    ),
                    color = Color(0xFF2E7D32)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "Information",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        lineHeight = 36.sp
                    ),
                    color = Color(0xFF2E7D32)
                )
            }

            // 🔹 Subtitle
            Text(
                text = "Manage your prescription medications, refills, and schedules",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    color = Color(0xFF5E5E5E)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 🔍 Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = {
                    Text(
                        text = "Search Medications...",
                        color = Color(0xFF9E9E9E)
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF757575)
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF5F5F5),
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            Spacer(modifier = Modifier.height(5.dp))

            // ─────────────── MEDICATIONS SECTION ───────────────
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp, vertical = 6.dp)
                ) {

                    // 🔹 Active & Low Supply Cards — small summary boxes side by side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCardSmall(
                            title = "Active",
                            value = summary?.totalActive?.toString() ?: "0",
                            color = Color(0xFF4CAF50),
                            iconType = "active",
                            modifier = Modifier.weight(1f)
                        )

                        SummaryCardSmall(
                            title = "Low Supply",
                            value = summary?.lowSupplyCount?.toString() ?: "0",
                            color = Color(0xFFFFA000),
                            iconType = "lowSupply",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    // 🔹 Medication Cards (larger and detailed)
                    if (medications.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.LocalPharmacy,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No medications found",
                                    fontSize = 16.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "Try a different search" else "Add medications to get started",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        medications.forEach { medication ->
                            MedicationCardExpanded(
                                name = medication.medicationName,
                                dosage = medication.dosage,
                                frequency = medication.frequency,
                                time = medication.schedules?.firstOrNull()?.scheduledTime?.let { formatTime(it) } ?: "Not scheduled",
                                nextRefill = medication.nextRefillDate ?: "Not set",
                                doctor = medication.doctorName ?: "Unknown",
                                pharmacy = medication.pharmacyName ?: "Unknown",
                                color = parseColor(medication.color),
                                supplyRemaining = (medication.supplyRemainingPercentage ?: 0.0).toFloat() / 100f
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper functions from HomeViewModel
private fun formatTime(time: String): String {
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

@Composable
fun SummaryCardSmall(
    title: String,
    value: String,
    color: Color,
    iconType: String = "active",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(90.dp)
            .clip(RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🔹 Row for icon + title (side by side)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    imageVector = if (iconType == "active") Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 🔹 Value (number) underneath
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun MedicationCardExpanded(
    name: String,
    dosage: String,
    frequency: String,
    time: String,
    nextRefill: String,
    doctor: String,
    pharmacy: String,
    color: Color,
    supplyRemaining: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(20.dp)
        ) {
            // 🔹 Top Row — Name + Dosage on left, Edit button on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Name + Dosage
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dosage,
                        fontSize = 13.sp,
                        color = Color(0xFF555555)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { /* TODO: handle edit click */ }
                        .padding(vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Medication",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Edit",
                        fontSize = 14.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 🔹 Frequency + Time + Next Refill
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = "Time",
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$time • $frequency",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Next refill: $nextRefill",
                fontSize = 13.sp,
                color = Color(0xFF777777)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 🔹 Doctor + Pharmacy Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Doctor",
                        tint = Color(0xFF4E4E4E),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = doctor,
                        fontSize = 13.sp,
                        color = Color(0xFF4E4E4E)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalPharmacy,
                        contentDescription = "Pharmacy",
                        tint = Color(0xFF4E4E4E),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = pharmacy,
                        fontSize = 13.sp,
                        color = Color(0xFF4E4E4E)
                    )
                }
            }
            // 🔹 Supply Remaining bar
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Supply Remaining: ${(supplyRemaining * 100).toInt()}%",
                fontSize = 13.sp,
                color = Color(0xFF444444),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { supplyRemaining },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF4CAF50),
                trackColor = Color(0xFFE0E0E0)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}


@Preview
@Composable
fun MedicationScreenPreview() {
    MaterialTheme {
        MedicationScreen(MedicationViewModel(secureStorage = createSecureStorage()))
    }
}
