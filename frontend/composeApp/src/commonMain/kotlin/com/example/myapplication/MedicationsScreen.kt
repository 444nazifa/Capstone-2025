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
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun MedicationScreen(modifier: Modifier = Modifier) {
    // 👇 Declare state variable here (must be inside @Composable)
    var searchQuery by remember { mutableStateOf("") }

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
                onValueChange = { searchQuery = it },
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
                        value = "6",
                        color = Color(0xFF4CAF50),
                        iconType = "active",
                        modifier = Modifier.weight(1f)
                    )

                    SummaryCardSmall(
                        title = "Low Supply",
                        value = "2",
                        color = Color(0xFFFFA000),
                        iconType = "lowSupply",
                        modifier = Modifier.weight(1f)
                    )
                }


                Spacer(modifier = Modifier.height(5.dp))

                // 🔹 Medication Cards (larger and detailed)
                MedicationCardExpanded(
                    name = "Metformin",
                    dosage = "500mg",
                    frequency = "Every day",
                    time = "8:00 AM",
                    nextRefill = "Oct 22, 2025",
                    doctor = "Dr. Hernandez",
                    pharmacy = "Walgreens #2413",
                    color = Color(0xFF4CAF50),
                    supplyRemaining = 0.65f
                )

                MedicationCardExpanded(
                    name = "Lisinopril",
                    dosage = "20mg",
                    frequency = "Every 2 days",
                    time = "12:00 PM",
                    nextRefill = "Nov 3, 2025",
                    doctor = "Dr. Chang",
                    pharmacy = "CVS Pharmacy",
                    color = Color(0xFFFFA000),
                    supplyRemaining = 0.25f
                )
            }
        }
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Name + Dosage
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dosage,
                        fontSize = 13.sp,
                        color = Color(0xFF555555)
                    )
                }

// ✏️ Edit button (no extra padding)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { /* TODO: handle edit click */ }
                        .padding(vertical = 0.dp) // 👈 remove top/bottom padding
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
                progress = supplyRemaining,
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
        MedicationScreen()
    }
}
