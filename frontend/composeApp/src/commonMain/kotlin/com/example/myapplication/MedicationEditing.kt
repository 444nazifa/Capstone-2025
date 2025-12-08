package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.UserMedication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMedicationScreen(
    medication: UserMedication,
    onBack: () -> Unit,
    onSave: (UserMedication) -> Unit,
    modifier: Modifier = Modifier
) {
    // ---------- STATE ----------
    var currentTab by remember { mutableStateOf(EditMedicationTab.Basic) }

    // Basic
    var brandName by remember { mutableStateOf(medication.medicationName) }
    var dosage by remember { mutableStateOf(medication.dosage) }
    var doctorName by remember { mutableStateOf(medication.doctorName.orEmpty()) }
    var pharmacyName by remember { mutableStateOf(medication.pharmacyName.orEmpty()) }
    var notes by remember { mutableStateOf(medication.instructions.orEmpty()) }

    // Schedule
    var frequency by remember { mutableStateOf(medication.frequency) }
    var scheduleTimes by remember {
        mutableStateOf(
            medication.schedules?.map { it.scheduledTime } ?: listOf("08:00")
        )
    }

    // Refills / inventory-ish
    var quantityTotal by remember { mutableStateOf(medication.quantityTotal?.toString().orEmpty()) }
    var quantityRemaining by remember { mutableStateOf(medication.quantityRemaining?.toString().orEmpty()) }
    var refillReminderDays by remember { mutableStateOf(medication.refillReminderDays?.toString() ?: "7") }
    var startDate by remember { mutableStateOf(medication.startDate.orEmpty()) }

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column {
                        Text("Edit Medication", fontWeight = FontWeight.Bold)
                        Text(
                            text = brandName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            // Build updated medication object
                            val updated = medication.copy(
                                medicationName = brandName,
                                dosage = dosage,
                                frequency = frequency,
                                doctorName = doctorName.ifBlank { null },
                                pharmacyName = pharmacyName.ifBlank { null },
                                instructions = notes.ifBlank { null },
                                quantityTotal = quantityTotal.toIntOrNull(),
                                quantityRemaining = quantityRemaining.toIntOrNull(),
                                refillReminderDays = refillReminderDays.toIntOrNull(),
                                startDate = startDate.ifBlank { medication.startDate }
                                // ⚠️ If you want schedule editing to persist, map `scheduleTimes`
                                // into your schedule model here and pass it into `copy(schedules = ...)`.
                            )
                            onSave(updated)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // ---------- TABS ----------
            TabRow(
                selectedTabIndex = currentTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                EditMedicationTab.values().forEachIndexed { index, tab ->
                    Tab(
                        selected = currentTab.ordinal == index,
                        onClick = { currentTab = tab },
                        text = { Text(tab.label) }
                    )
                }
            }

            // ---------- CONTENT ----------
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (currentTab) {
                    EditMedicationTab.Basic -> {
                        BasicInfoCard(
                            brandName = brandName,
                            onBrandNameChange = { brandName = it },
                            dosage = dosage,
                            onDosageChange = { dosage = it },
                            doctorName = doctorName,
                            onDoctorChange = { doctorName = it },
                            pharmacyName = pharmacyName,
                            onPharmacyChange = { pharmacyName = it }
                        )
                    }

                    EditMedicationTab.Schedule -> {
                        ScheduleCard(
                            frequency = frequency,
                            onFrequencyChange = { frequency = it },
                            scheduleTimes = scheduleTimes,
                            onScheduleTimesChange = { scheduleTimes = it }
                        )
                    }

                    EditMedicationTab.Refills -> {
                        RefillsCard(
                            quantityTotal = quantityTotal,
                            onQuantityTotalChange = { quantityTotal = it },
                            quantityRemaining = quantityRemaining,
                            onQuantityRemainingChange = { quantityRemaining = it },
                            refillReminderDays = refillReminderDays,
                            onRefillReminderDaysChange = { refillReminderDays = it },
                            startDate = startDate,
                            onStartDateChange = { startDate = it }
                        )
                    }

                    EditMedicationTab.Notes -> {
                        NotesCard(
                            notes = notes,
                            onNotesChange = { notes = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private enum class EditMedicationTab(val label: String) {
    Basic("Basic"),
    Schedule("Schedule"),
    Refills("Refills"),
    Notes("Notes")
}

// ---------- BASIC TAB ----------

@Composable
private fun BasicInfoCard(
    brandName: String,
    onBrandNameChange: (String) -> Unit,
    dosage: String,
    onDosageChange: (String) -> Unit,
    doctorName: String,
    onDoctorChange: (String) -> Unit,
    pharmacyName: String,
    onPharmacyChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Medication Details",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = brandName,
                onValueChange = onBrandNameChange,
                label = { Text("Brand Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = dosage,
                onValueChange = onDosageChange,
                label = { Text("Dosage") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = doctorName,
                    onValueChange = onDoctorChange,
                    label = { Text("Prescribed By") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = pharmacyName,
                    onValueChange = onPharmacyChange,
                    label = { Text("Pharmacy") },
                    leadingIcon = {
                        Icon(Icons.Default.LocalPharmacy, contentDescription = null)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ---------- SCHEDULE TAB ----------

@Composable
private fun ScheduleCard(
    frequency: String,
    onFrequencyChange: (String) -> Unit,
    scheduleTimes: List<String>,
    onScheduleTimesChange: (List<String>) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Dosing Schedule",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Frequency selector (simple chips)
            Text("Frequency", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            val options = listOf(
                "Every day",
                "Every other day",
                "As needed",
                "Weekly",
                "Twice daily"
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    FilterChip(
                        selected = frequency == option,
                        onClick = { onFrequencyChange(option) },
                        label = { Text(option) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2E7D32),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Dosing Times", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            scheduleTimes.forEachIndexed { index, time ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF616161),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedTextField(
                        value = time,
                        onValueChange = { new ->
                            val updated = scheduleTimes.toMutableList()
                            updated[index] = new
                            onScheduleTimesChange(updated)
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Time (HH:MM)") }
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    if (scheduleTimes.size > 1) {
                        TextButton(
                            onClick = {
                                val updated = scheduleTimes.toMutableList()
                                updated.removeAt(index)
                                onScheduleTimesChange(updated)
                            }
                        ) {
                            Text("Remove")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedButton(
                onClick = {
                    val updated = scheduleTimes.toMutableList()
                    updated.add("08:00")
                    onScheduleTimesChange(updated)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Time")
            }
        }
    }
}

// ---------- REFILLS TAB ----------

@Composable
private fun RefillsCard(
    quantityTotal: String,
    onQuantityTotalChange: (String) -> Unit,
    quantityRemaining: String,
    onQuantityRemainingChange: (String) -> Unit,
    refillReminderDays: String,
    onRefillReminderDaysChange: (String) -> Unit,
    startDate: String,
    onStartDateChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Refill Information",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = quantityTotal,
                    onValueChange = { if (it.all(Char::isDigit)) onQuantityTotalChange(it) },
                    label = { Text("Total Pills") },
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = quantityRemaining,
                    onValueChange = { if (it.all(Char::isDigit)) onQuantityRemainingChange(it) },
                    label = { Text("Pills Remaining") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = refillReminderDays,
                onValueChange = { if (it.all(Char::isDigit)) onRefillReminderDaysChange(it) },
                label = { Text("Refill Reminder (days before)") },
                leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = startDate,
                onValueChange = onStartDateChange,
                label = { Text("Date Prescribed (YYYY-MM-DD)") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ---------- NOTES TAB ----------

@Composable
private fun NotesCard(
    notes: String,
    onNotesChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Personal Notes / Instructions",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                label = { Text("Notes") },
                singleLine = false,
                maxLines = 6
            )
        }
    }
}
