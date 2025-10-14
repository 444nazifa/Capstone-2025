package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.myapplication.theme.CareCapsuleTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults


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
        ) {

            // ──────────────── Section 1: Header + Calendar (fixed) ────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 48.dp, bottom = 16.dp)
            ) {
                Text(
                    text = "My Prescriptions",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(20.dp))

                WeeklyCalendarCardStyled(
                    currentDate = currentDate,
                    weekDays = weekDays,
                    isToday = viewModel::isToday,
                    onNavigateWeek = viewModel::navigateWeek
                )
            }

            // ──────────────── Divider ────────────────
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )

            // ───────────── Scrollable “Today’s Medication” section ─────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 90.dp)
                ) {
                    Text(
                        text = "Today's Medication",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

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
                .padding(horizontal = 16.dp, vertical = 25.dp)
        ) {
            // Month header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Oct 2025", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Previous week",
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onNavigateWeek(-1) }, // 👈 go back a week
                        tint = Color(0xFF555555)
                    )

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Next week",
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onNavigateWeek(1) }, // 👈 go forward a week
                        tint = Color(0xFF555555)
                    )
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
                            .padding(vertical = 15.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isToday(day)) MaterialTheme.colorScheme.primary else Color.Transparent
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
            verticalAlignment = Alignment.Top
        ) {
            // ✅ Checkbox
            Checkbox(
                checked = medication.taken,
                onCheckedChange = { onToggle() },
                modifier = Modifier
                    .offset(y = (3).dp)       // lift slightly to align with text
                    .padding(start = 0.dp, end = 0.dp) // 👈 remove extra space
                    .size(16.dp),              // optional: slightly smaller checkbox
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = Color.Gray
                )
            )


            Spacer(modifier = Modifier.width(8.dp))

            // ✅ Medication info
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
                    text = medication.dosage,
                    color = Color(0xFF555555),
                    fontSize = 13.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Time",
                        tint = Color(0xFF777777),
                        modifier = Modifier
                            .size(14.dp)
                            .padding(end = 4.dp)
                    )
                    Text(
                        text = medication.time,
                        color = Color(0xFF777777),
                        fontSize = 12.sp
                    )
                }
                medication.instructions?.let {
                    Text(
                        text = it,
                        color = Color(0xFF777777),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}


@Preview
@Composable
fun HomeScreenPreview() {
    CareCapsuleTheme {
        val fakeViewModel = HomeViewModel()
        HomeScreen(viewModel = fakeViewModel)
    }
}
