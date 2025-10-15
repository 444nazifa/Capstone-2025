package com.example.myapplication

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    // --- demo state (wire to real data later) ---
    var medReminders by remember { mutableStateOf(true) }
    var refillAlerts by remember { mutableStateOf(true) }
    var genericInfo by remember { mutableStateOf(true) }

    val brandGreen = Color(0xFF2E7D32)
    val cardShape = RoundedCornerShape(12.dp)

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 28.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ───────── Title ─────────
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = brandGreen,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // ───────── Header Card (Avatar + email + Edit) ─────────
            OutlinedCard(
                shape = cardShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .6f)),
                colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Name + email (placeholders)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "John Doe",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF3F3F3F)
                        )
                        Text(
                            text = "johndoe@gmail.com",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF7A7A7A)
                        )
                        Spacer(Modifier.height(8.dp))
                        // small green button
                        FilledTonalButton(
                            onClick = { /* TODO: open edit screen */ },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = brandGreen.copy(alpha = .15f),
                                contentColor = brandGreen
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Edit Profile", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // ───────── Account Information ─────────
            OutlinedCard(
                shape = cardShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .6f)),
                colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "ACCOUNT INFORMATION",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF3F3F3F)
                    )
                    InfoRow(Icons.Default.Email, "johndoe@gmail.com")
                    InfoRow(Icons.Default.Phone, "+1 (123) 456 - 7890")
                    InfoRow(Icons.Default.CalendarToday, "Born 01/01/1900")
                }
            }

            // ───────── Notification Settings ─────────
            OutlinedCard(
                shape = cardShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .6f)),
                colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NOTIFICATION SETTINGS",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF3F3F3F)
                        )
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color(0xFF616161)
                        )
                    }

                    SettingRow(
                        title = "Medication Reminders",
                        subtitle = "Get notified when it’s time to take your medications",
                        checked = medReminders,
                        onChange = { medReminders = it }
                    )

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = .3f)
                    )

                    SettingRow(
                        title = "Refill Alert",
                        subtitle = "Alerts when you're running low on medications",
                        checked = refillAlerts,
                        onChange = { refillAlerts = it }
                    )

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = .3f)
                    )

                    SettingRow(
                        title = "Title",
                        subtitle = "Information",
                        checked = genericInfo,
                        onChange = { genericInfo = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF5F5F5F))
        Text(text, color = Color(0xFF4C4C4C), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF303030))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF7A7A7A))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
