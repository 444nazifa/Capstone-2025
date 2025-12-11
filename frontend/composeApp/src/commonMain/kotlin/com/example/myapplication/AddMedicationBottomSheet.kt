package com.example.myapplication

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.MedicationSearchResult
import com.example.myapplication.data.CreateMedicationRequest
import com.example.myapplication.data.CreateScheduleRequest
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Data class to hold schedule details
data class ScheduleDetail(
    val time: String,
    val daysOfWeek: List<Int> = listOf(0, 1, 2, 3, 4, 5, 6)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationBottomSheet(
    medication: MedicationSearchResult,
    onDismiss: () -> Unit,
    onSave: (CreateMedicationRequest) -> Unit,
    isLoading: Boolean = false
) {
    var dosage by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("Every day") }
    var schedules by remember { mutableStateOf(listOf(ScheduleDetail(time = "08:00"))) }
    var doctorName by remember { mutableStateOf("") }
    var pharmacyName by remember { mutableStateOf("") }
    var pharmacyLocation by remember { mutableStateOf("") }
    var quantityTotal by remember { mutableStateOf("") }
    var quantityRemaining by remember { mutableStateOf("") }
    var refillReminderDays by remember { mutableStateOf("7") }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf("#4CAF50") }
    var expandedScheduleIndex by remember { mutableStateOf<Int?>(null) }

    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header with medication info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Add Medication",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Medication card preview
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        medication.imageUrl?.let { imageUrl ->
                            AsyncImage(
                                imageUrl = imageUrl,
                                contentDescription = medication.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } ?: Icon(
                            Icons.Default.Medication,
                            null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            medication.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            color = Color(0xFF1B5E20)
                        )
                        medication.labeler?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                maxLines = 1
                            )
                        }
                        medication.ndc?.firstOrNull()?.let {
                            Text(
                                "NDC: $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Dosage & Instructions Section
            SectionHeader("Dosage & Instructions", Icons.Default.Medication)

            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = { Text("Dosage *") },
                placeholder = { Text("e.g., 500mg, 1 tablet") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.LocalPharmacy, null, tint = Color(0xFF2E7D32)) },
                colors = outlinedTextFieldColors()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                label = { Text("Instructions") },
                placeholder = { Text("e.g., Take with food") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                leadingIcon = { Icon(Icons.Default.Description, null, tint = Color(0xFF2E7D32)) },
                colors = outlinedTextFieldColors()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Schedule Section
            SectionHeader("Medication Schedule", Icons.Default.Schedule)

            // Frequency selector
            FrequencySelector(
                selected = frequency,
                onSelect = { frequency = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Schedules list
            Text(
                "Schedule Times (Tap to configure days)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            schedules.forEachIndexed { index, schedule ->
                SimpleScheduleCard(
                    schedule = schedule,
                    isExpanded = expandedScheduleIndex == index,
                    onExpand = { expandedScheduleIndex = if (expandedScheduleIndex == index) null else index },
                    onUpdate = { updated ->
                        schedules = schedules.toMutableList().also { it[index] = updated }
                    },
                    onRemove = {
                        if (schedules.size > 1) {
                            schedules = schedules.filterIndexed { i, _ -> i != index }
                            if (expandedScheduleIndex == index) expandedScheduleIndex = null
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Add new schedule button
            OutlinedButton(
                onClick = { showTimePickerDialog = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF2E7D32)
                ),
                border = BorderStroke(2.dp, Color(0xFF2E7D32)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Schedule Time", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Doctor & Pharmacy Section
            SectionHeader("Doctor & Pharmacy", Icons.Default.LocalHospital)

            OutlinedTextField(
                value = doctorName,
                onValueChange = { doctorName = it },
                label = { Text("Doctor Name") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, null, tint = Color(0xFF2E7D32)) },
                colors = outlinedTextFieldColors()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = pharmacyName,
                onValueChange = { pharmacyName = it },
                label = { Text("Pharmacy Name") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.LocalPharmacy, null, tint = Color(0xFF2E7D32)) },
                colors = outlinedTextFieldColors()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = pharmacyLocation,
                onValueChange = { pharmacyLocation = it },
                label = { Text("Pharmacy Location") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = Color(0xFF2E7D32)) },
                colors = outlinedTextFieldColors()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Refill Information Section
            SectionHeader("Refill Information", Icons.Default.Inventory)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = quantityTotal,
                    onValueChange = { if (it.all { char -> char.isDigit() }) quantityTotal = it },
                    label = { Text("Total Quantity") },
                    placeholder = { Text("30") },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Inventory, null, tint = Color(0xFF2E7D32)) },
                    colors = outlinedTextFieldColors()
                )

                OutlinedTextField(
                    value = quantityRemaining,
                    onValueChange = { if (it.all { char -> char.isDigit() }) quantityRemaining = it },
                    label = { Text("Remaining") },
                    placeholder = { Text("30") },
                    modifier = Modifier.weight(1f),
                    colors = outlinedTextFieldColors()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = refillReminderDays,
                onValueChange = { if (it.all { char -> char.isDigit() }) refillReminderDays = it },
                label = { Text("Refill Reminder (days before)") },
                placeholder = { Text("7") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Notifications, null, tint = Color(0xFF2E7D32)) },
                colors = outlinedTextFieldColors()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Color picker
            SectionHeader("Display Color", Icons.Default.Palette)

            ColorPicker(
                selectedColor = selectedColor,
                onColorSelect = { selectedColor = it }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Save button
            Button(
                onClick = {
                    if (dosage.isNotBlank() && !isLoading) {
                        val request = CreateMedicationRequest(
                            medicationName = medication.title,
                            dosage = dosage,
                            setId = medication.setId,
                            ndc = medication.ndc?.firstOrNull()?.takeIf { it.isNotBlank() },
                            instructions = instructions.ifBlank { null },
                            frequency = frequency,
                            doctorName = doctorName.ifBlank { null },
                            pharmacyName = pharmacyName.ifBlank { null },
                            pharmacyLocation = pharmacyLocation.ifBlank { null },
                            quantityTotal = quantityTotal.toIntOrNull(),
                            quantityRemaining = quantityRemaining.toIntOrNull(),
                            refillReminderDays = refillReminderDays.toIntOrNull() ?: 7,
                            startDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString(),
                            color = selectedColor,
                            schedules = schedules.map { schedule ->
                                CreateScheduleRequest(
                                    scheduledTime = schedule.time,
                                    daysOfWeek = schedule.daysOfWeek,
                                    isEnabled = true
                                )
                            }
                        )
                        onSave(request)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(12.dp),
                enabled = dosage.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Check, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Medication", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    if (showTimePickerDialog) {
        TimePickerDialog(
            onDismiss = { showTimePickerDialog = false },
            onTimeSelected = { hour, minute ->
                val timeString = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
                if (!schedules.any { it.time == timeString }) {
                    schedules = schedules + ScheduleDetail(
                        time = timeString,
                        daysOfWeek = listOf(0, 1, 2, 3, 4, 5, 6)
                    )
                }
                showTimePickerDialog = false
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF2E7D32),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32)
        )
    }
}

@Composable
private fun FrequencySelector(selected: String, onSelect: (String) -> Unit) {
    val frequencies = listOf("Every day", "Every other day", "As needed", "Weekly")

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(frequencies) { frequency ->
            FilterChip(
                selected = selected == frequency,
                onClick = { onSelect(frequency) },
                label = { Text(frequency) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF2E7D32),
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun SimpleScheduleCard(
    schedule: ScheduleDetail,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onUpdate: (ScheduleDetail) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).clickable { onExpand() }
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            schedule.time,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            getDaysText(schedule.daysOfWeek),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onExpand,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            "Expand",
                            tint = Color(0xFF2E7D32)
                        )
                    }

                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            "Remove",
                            tint = Color(0xFFE57373),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Expanded content
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFCCCCCC))
                Spacer(modifier = Modifier.height(16.dp))

                // Days of week for this schedule
                Text(
                    "Active Days",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                DaysOfWeekSelector(
                    selectedDays = schedule.daysOfWeek,
                    onDaysChange = { onUpdate(schedule.copy(daysOfWeek = it)) }
                )
            }
        }
    }
}

private fun getDaysText(days: List<Int>): String {
    return when {
        days.size == 7 -> "Every day"
        days.isEmpty() -> "No days selected"
        days.size == 5 && days.containsAll(listOf(1, 2, 3, 4, 5)) -> "Weekdays"
        days.size == 2 && days.containsAll(listOf(0, 6)) -> "Weekends"
        else -> {
            val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            days.sorted().joinToString(", ") { dayNames[it] }
        }
    }
}

@Composable
private fun TimeChip(time: String, onRemove: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.Schedule,
                null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Text(
                time,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun DaysOfWeekSelector(
    selectedDays: List<Int>,
    onDaysChange: (List<Int>) -> Unit
) {
    val days = listOf("S", "M", "T", "W", "T", "F", "S")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEachIndexed { index, day ->
            val isSelected = selectedDays.contains(index)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) Color(0xFF2E7D32) else Color(0xFFF5F5F5))
                    .clickable {
                        onDaysChange(
                            if (isSelected) selectedDays - index
                            else selectedDays + index
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    day,
                    color = if (isSelected) Color.White else Color.Gray,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ColorPicker(selectedColor: String, onColorSelect: (String) -> Unit) {
    val colors = listOf(
        "#4CAF50", "#2196F3", "#FF9800", "#E91E63",
        "#9C27B0", "#00BCD4", "#FF5722", "#795548"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(colors) { colorHex ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(parseColor(colorHex))
                    .border(
                        width = if (selectedColor == colorHex) 3.dp else 0.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable { onColorSelect(colorHex) },
                contentAlignment = Alignment.Center
            ) {
                if (selectedColor == colorHex) {
                    Icon(
                        Icons.Default.Check,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    onDismiss: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = 8,
        initialMinute = 0,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            TextButton(onClick = {
                onTimeSelected(timePickerState.hour, timePickerState.minute)
            }) {
                Text("OK", color = Color(0xFF2E7D32))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF2E7D32),
    focusedLabelColor = Color(0xFF2E7D32),
    cursorColor = Color(0xFF2E7D32)
)

private fun parseColor(hex: String): Color {
    val cleanHex = hex.removePrefix("#")
    val colorInt = cleanHex.toLong(16)
    return Color(
        red = ((colorInt shr 16) and 0xFF) / 255f,
        green = ((colorInt shr 8) and 0xFF) / 255f,
        blue = (colorInt and 0xFF) / 255f,
        alpha = 1f
    )
}

