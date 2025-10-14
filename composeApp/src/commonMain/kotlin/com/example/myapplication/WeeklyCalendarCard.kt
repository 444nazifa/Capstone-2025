package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.datetime.*

@Composable
fun WeeklyCalendarCard(
    currentDate: LocalDateTime,
    weekDays: List<LocalDate>,
    isToday: (LocalDate) -> Boolean,
    onNavigateWeek: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {

            val header = weekDays.first()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${getMonthName(header.monthNumber)} ${header.year}", // ✅ CHANGED
                    style = MaterialTheme.typography.titleMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { onNavigateWeek(false) }, modifier = Modifier.size(32.dp)) {
                        Text("←", style = MaterialTheme.typography.titleMedium)
                    }
                    IconButton(onClick = { onNavigateWeek(true) }, modifier = Modifier.size(32.dp)) {
                        Text("→", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEach { date ->
                    WeekDayItem(
                        date = date,
                        dayName = dayAbbrevFor(date),
                        isToday = isToday(date),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun WeekDayItem(
    date: LocalDate,
    dayName: String,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(4.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (isToday) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = 1.dp,
                color = if (isToday) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.medium
            )
            .padding(12.dp)
    ) {
        Text(
            text = dayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyLarge,
            color = if (isToday) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (date.dayOfMonth % 3 == 0) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                )
            }
            if (date.dayOfMonth % 5 == 0) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                )
            }
        }
    }
}

// UPDATE: Generates a Sunday-starting week (prevents 1-day delay)
fun weekStartingSunday(anchor: LocalDate): List<LocalDate> {
    val daysFromSunday = (anchor.dayOfWeek.ordinal + 1) % 7 // ✅ CHANGED
    val start = anchor.minus(daysFromSunday, DateTimeUnit.DAY)
    return (0..6).map { start.plus(it, DateTimeUnit.DAY) }
}

// UPDATE: Derive day name directly from the actual date (not index)
private fun dayAbbrevFor(date: LocalDate): String = when (date.dayOfWeek) {
    DayOfWeek.MONDAY -> "Mon"
    DayOfWeek.TUESDAY -> "Tue"
    DayOfWeek.WEDNESDAY -> "Wed"
    DayOfWeek.THURSDAY -> "Thu"
    DayOfWeek.FRIDAY -> "Fri"
    DayOfWeek.SATURDAY -> "Sat"
    DayOfWeek.SUNDAY -> "Sun"
    else -> ""
}

private fun getMonthName(month: Int): String = when (month) {
    1 -> "Jan"
    2 -> "Feb"
    3 -> "Mar"
    4 -> "Apr"
    5 -> "May"
    6 -> "Jun"
    7 -> "Jul"
    8 -> "Aug"
    9 -> "Sep"
    10 -> "Oct"
    11 -> "Nov"
    12 -> "Dec"
    else -> ""
}