package com.example.myapplication

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.example.myapplication.viewmodel.HomeViewModel
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.theme.CareCapsuleTheme
import kotlinx.datetime.*
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val medications by viewModel.medications.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val weekOffset by viewModel.weekOffset.collectAsState()
    val weekDays by remember(currentDate) { mutableStateOf(viewModel.getWeekDays()) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ─────────────── Header + Calendar ───────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 55.dp, bottom = 16.dp)
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
                    weekOffset = weekOffset,
                    currentDate = currentDate,
                    weekDays = weekDays,
                    isToday = viewModel::isToday,
                    onNavigateWeek = viewModel::navigateWeek,
                    viewModel = viewModel
                )
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )

            // ─────────────── Today's Medication (scrollable) ───────────────
            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 120.dp)
            ) {
                Column {
                    Text(
                        text = "Today's Medication",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 25.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val upcomingMedications: List<MedicationReminder> = medications.filter { !it.taken }
                    val completedMedications: List<MedicationReminder> = medications.filter { it.taken }

                    // UPCOMING SECTION
                    SectionHeader(
                        icon = Icons.Default.ErrorOutline,
                        iconColor = Color(0xFFD32F2F),
                        label = "Upcoming (${upcomingMedications.size})"
                    )

                    upcomingMedications.forEach { med: MedicationReminder ->
                        MedicationItemStyled(med) { viewModel.toggleMedication(med.id) }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // COMPLETED SECTION
                    SectionHeader(
                        icon = Icons.Default.Check,
                        iconColor = Color(0xFF388E3C),
                        label = "Completed (${completedMedications.size})"
                    )

                    completedMedications.forEach { med: MedicationReminder ->
                        MedicationItemStyled(med) { viewModel.toggleMedication(med.id) }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(icon: ImageVector, iconColor: Color, label: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(25.dp)
                )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color.Black
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}



@Composable
fun WeeklyCalendarCardStyled(
    weekOffset: Int,
    currentDate: String,
    weekDays: List<String>,
    isToday: (String) -> Boolean,
    onNavigateWeek: (Int) -> Unit,
    viewModel: HomeViewModel
) {
    val baseDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val offsetDate = baseDate.plus(weekOffset * 7, DateTimeUnit.DAY)
    val monthName = offsetDate.month.name.lowercase().replaceFirstChar { it.uppercase() }
    val year = offsetDate.year

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEDEAF1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
            // Month header with arrows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$monthName $year", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Previous week",
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onNavigateWeek(-1) },
                        tint = Color(0xFF555555)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Next week",
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onNavigateWeek(1) },
                        tint = Color(0xFF555555)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Week row with dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEach { day ->
                    val dateNumber = day.takeLast(2).trim().toIntOrNull() ?: 1
                    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                    val startOfWeek = today.minus(today.dayOfWeek.ordinal.toLong(), DateTimeUnit.DAY)
                    val dateForThisDay =
                        startOfWeek.plus(dateNumber - startOfWeek.dayOfMonth, DateTimeUnit.DAY)

                    val medsForDay = viewModel.getMedicationsForDay(dateForThisDay)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(40.dp)
                            .padding(vertical = 10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isToday(day)) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = day.take(2),
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (isToday(day)) Color.White else Color.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = day.takeLast(2),
                            fontSize = 14.sp,
                            color = if (isToday(day)) Color.White else Color.Black
                        )

                        // Medication dots
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            medsForDay.take(3).forEach { med: MedicationReminder ->
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .padding(horizontal = 1.dp)
                                        .background(color = med.color, shape = CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MedicationItemStyled(medication: MedicationReminder, onToggle: () -> Unit) {
    val containerAlpha = if (medication.taken) 0.5f else 1f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1EDF3)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .alpha(containerAlpha),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = medication.taken,
                onCheckedChange = { onToggle() },
                modifier = Modifier
                    .offset(y = (3).dp)
                    .size(18.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medication.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black,
                    textDecoration = if (medication.taken) TextDecoration.LineThrough else null
                )
                Text(
                    text = medication.dosage,
                    color = Color(0xFF555555),
                    fontSize = 13.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Time",
                        tint = Color(0xFF777777),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = medication.time,
                        color = Color(0xFF777777),
                        fontSize = 12.sp
                    )
                }
                medication.instructions?.let {
                    Text(text = it, color = Color(0xFF777777), fontSize = 12.sp)
                }
                Text(
                    text = "Frequency: ${medication.frequency}",
                    color = Color(0xFF777777),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    CareCapsuleTheme {
        HomeScreen(HomeViewModel())
    }
}
