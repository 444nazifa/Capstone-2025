package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val medications by viewModel.medications.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val weekDays = viewModel.getWeekDays()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "My Medications",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Calendar card
            WeeklyCalendarCard(
                currentDate = currentDate,
                weekDays = weekDays,
                isToday = viewModel::isToday,
                onNavigateWeek = viewModel::navigateWeek
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Medication list
            Text(
                text = "Today's Schedule",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            medications.forEach { medication ->
                MedicationItem(
                    medication = medication,
                    onToggle = { viewModel.toggleMedication(medication.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun MedicationItem(
    medication: MedicationReminder,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = medication.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${medication.dosage} - ${medication.time}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                medication.instructions?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = medication.taken,
                onCheckedChange = { onToggle() }
            )
        }
    }
}
