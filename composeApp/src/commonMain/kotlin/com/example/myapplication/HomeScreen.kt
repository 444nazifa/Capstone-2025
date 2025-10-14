package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp) // top padding added
        ) {
            // Title
            Text(
                text = "My Medications",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 28.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Calendar Card
            WeeklyCalendarCardStyled(
                currentDate = currentDate,
                weekDays = weekDays,
                isToday = viewModel::isToday,
                onNavigateWeek = viewModel::navigateWeek
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Section title
            Text(
                text = "Today's Schedule",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Medication cards
            medications.forEach { medication ->
                MedicationItemStyled(
                    medication = medication,
                    onToggle = { viewModel.toggleMedication(medication.id) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun WeeklyCalendarCardStyled(
    currentDate: String,
    weekDays: List<String>,
    isToday: (String) -> Boolean,
    onNavigateWeek: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEDEAF1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Month header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Oct 2025", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("←")
                    Text("→")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Week row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEach { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(40.dp)
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isToday(day)) Color(0xFF7A5CDA) else Color.Transparent
                            )
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = day.take(2), // Su, Mo, Tu...
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (isToday(day)) Color.White else Color.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = day.takeLast(2), // number part
                            fontSize = 14.sp,
                            color = if (isToday(day)) Color.White else Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MedicationItemStyled(
    medication: MedicationReminder,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1EDF3)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = medication.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Text(
                    text = "${medication.dosage} - ${medication.time}",
                    color = Color(0xFF555555),
                    fontSize = 13.sp
                )
                medication.instructions?.let {
                    Text(
                        text = it,
                        color = Color(0xFF777777),
                        fontSize = 12.sp
                    )
                }
            }

            Switch(
                checked = medication.taken,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF7A5CDA)
                )
            )
        }
    }
}
