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
import androidx.compose.ui.unit.sp
import kotlinx.datetime.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.UserSession
import com.example.myapplication.viewmodel.ProfileViewModel
import com.example.myapplication.viewmodel.ProfileUpdateState

// Simple profile model for this screen
data class UserProfile(
    val name: String,
    val email: String,
    val phone: String,
    val dateOfBirth: String
)

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onSignOut: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel { ProfileViewModel() }
) {
    // --- demo state (wire to real data later) ---
    var medReminders by remember { mutableStateOf(true) }
    var refillAlerts by remember { mutableStateOf(true) }
    var genericInfo by remember { mutableStateOf(true) }

    // Get user from session
    val sessionUser by UserSession.currentUser.collectAsState()
    val token by UserSession.currentToken.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    // Convert backend User to UserProfile format
    val initialProfile = sessionUser?.let {
        UserProfile(
            name = it.name,
            email = it.email,
            phone = it.phone ?: "",
            dateOfBirth = formatDateForDisplay(it.date_of_birth)
        )
    } ?: UserProfile(
        name = "Guest User",
        email = "guest@example.com",
        phone = "",
        dateOfBirth = "01/01/1990"
    )

    // current user + edit state
    var user by remember(sessionUser) { mutableStateOf(initialProfile) }
    var isEditing by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf(user) }

    val brandGreen = Color(0xFF2E7D32)
    val cardShape = RoundedCornerShape(12.dp)

    // Handle update state
    LaunchedEffect(updateState) {
        when (updateState) {
            is ProfileUpdateState.Success -> {
                // Update local state with session data
                sessionUser?.let {
                    user = UserProfile(
                        name = it.name,
                        email = it.email,
                        phone = it.phone ?: "",
                        dateOfBirth = formatDateForDisplay(it.date_of_birth)
                    )
                    edit = user
                }
                isEditing = false
                viewModel.clearState()
            }
            is ProfileUpdateState.Error -> {
                // Error is shown in the EditProfileCard
            }
            else -> {}
        }
    }

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

            // ───────── Header Card (Avatar + Email + Edit) ─────────
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

                    // Name + email
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF3F3F3F)
                        )
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF7A7A7A)
                        )
                        Spacer(Modifier.height(8.dp))
                        // small green button
                        FilledTonalButton(
                            onClick = {
                                edit = user
                                isEditing = true
                            },
                            enabled = !isEditing,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isEditing) MaterialTheme.colorScheme.surfaceVariant
                                else brandGreen.copy(alpha = .15f),
                                contentColor = if (isEditing) MaterialTheme.colorScheme.onSurfaceVariant
                                else brandGreen
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Edit Profile", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // ───────── Account Information (Email + Phone Number + DOB) ─────────
            if (!isEditing) {
                OutlinedCard(
                    shape = cardShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .6f)),
                    colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "ACCOUNT INFORMATION",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF3F3F3F)
                        )
                        InfoRow(Icons.Default.Email, user.email)
                        InfoRow(Icons.Default.Phone, user.phone)
                        InfoRow(Icons.Default.CalendarToday, "Born ${user.dateOfBirth}")
                    }
                }
            }

            // ───────── Notification Settings ─────────
            if (!isEditing) {
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

            // ───────── Sign Out Button ─────────
            if (sessionUser != null && !isEditing) {
                OutlinedCard(
                    shape = cardShape,
                    border = BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.5f)),
                    colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
                ) {
                    Button(
                        onClick = {
                            UserSession.logout()
                            onSignOut()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Sign Out",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // ───────── Edit Profile (shown when editing) ─────────
            if (isEditing) {
                EditProfileCard(
                    value = edit,
                    onChange = { edit = it },
                    onSave = {
                        // Call backend API to update profile
                        token?.let { authToken ->
                            viewModel.updateProfile(
                                token = authToken,
                                name = edit.name,
                                email = edit.email,
                                dateOfBirth = formatDateForBackend(edit.dateOfBirth),
                                phone = edit.phone.ifBlank { null }
                            )
                        }
                    },
                    onCancel = {
                        edit = user
                        isEditing = false
                        viewModel.clearState()
                    },
                    brandGreen = brandGreen,
                    cardShape = cardShape,
                    isLoading = updateState is ProfileUpdateState.Loading,
                    errorMessage = (updateState as? ProfileUpdateState.Error)?.message
                )
            }
        }
    }
}

// Small info row (icon + text)
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

// Setting row with a switch
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


/* ───────── Edit card with validation ───────── */

@Composable
private fun EditProfileCard(
    value: UserProfile,
    onChange: (UserProfile) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    brandGreen: Color,
    cardShape: RoundedCornerShape,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var name by remember { mutableStateOf(value.name) }
    var email by remember { mutableStateOf(value.email) }
    var phone by remember { mutableStateOf(value.phone) }
    var dob by remember { mutableStateOf(value.dateOfBirth) }

    // live validation
    val emailOk = remember(email) { isValidEmail(email) }
    val phoneOk = remember(phone) { phone.isBlank() || isValidUsPhone(phone) }
    val dobParsed = remember(dob) { parseDobOrNull(dob) }
    val dobOk = remember(dobParsed) { dobParsed?.let { isReasonableDob(it) } == true }

    val allOk = name.isNotBlank() && emailOk && phoneOk && dobOk

    OutlinedCard(
        shape = cardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .6f)),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Edit Profile", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    onChange(value.copy(name = it))
                },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    onChange(value.copy(email = it))
                },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                isError = email.isNotBlank() && !emailOk,
                supportingText = {
                    if (email.isNotBlank() && !emailOk)
                        Text("Enter a valid email, e.g. name@example.com")
                }
            )

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    onChange(value.copy(phone = it))
                },
                label = { Text("Phone (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                isError = phone.isNotBlank() && !phoneOk,
                supportingText = {
                    if (phone.isNotBlank() && !phoneOk)
                        Text("Enter a valid US phone number, e.g. (555) 123-4567")
                }
            )

            OutlinedTextField(
                value = dob,
                onValueChange = {
                    dob = it
                    onChange(value.copy(dateOfBirth = it))
                },
                label = { Text("Date of Birth") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                isError = dob.isNotBlank() && !dobOk,
                supportingText = {
                    if (dob.isNotBlank() && !dobOk)
                        Text("Use MM/DD/YYYY, past date, age ≤ 120")
                }
            )

            // Error message display
            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = it,
                        color = Color(0xFFD32F2F),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { if (allOk && !isLoading) onSave() },
                    enabled = allOk && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = brandGreen,
                        disabledContainerColor = brandGreen.copy(alpha = 0.4f)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text("Save Changes")
                    }
                }

                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isLoading
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

/* ───────── Validation helpers ───────── */

private fun isValidEmail(s: String): Boolean {
    val re = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    return re.matches(s.trim())
}

private fun isValidUsPhone(s: String): Boolean {
    // Accepts: 1234567890, (123) 456-7890, 123-456-7890, +1 123 456 7890
    val re = Regex("^\\+?1?[-.\\s]?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}$")
    return re.matches(s.trim())
}

private fun parseDobOrNull(s: String): LocalDate? {
    val parts = s.trim().split("/")
    if (parts.size != 3) return null
    val (mmS, ddS, yyyyS) = parts
    val mm = mmS.toIntOrNull() ?: return null
    val dd = ddS.toIntOrNull() ?: return null
    val yyyy = yyyyS.toIntOrNull() ?: return null
    return try { LocalDate(yyyy, mm, dd) } catch (_: Throwable) { null }
}

private fun isReasonableDob(dob: LocalDate): Boolean {
    val tz = TimeZone.currentSystemDefault()
    val today = Clock.System.now().toLocalDateTime(tz).date
    if (dob >= today) return false
    val years = today.year - dob.year - if (
        today.monthNumber < dob.monthNumber ||
        (today.monthNumber == dob.monthNumber && today.dayOfMonth < dob.dayOfMonth)
    ) 1 else 0
    return years in 0..120
}

// Convert YYYY-MM-DD from backend to MM/DD/YYYY for display
private fun formatDateForDisplay(backendDate: String): String {
    return try {
        val parts = backendDate.split("-")
        if (parts.size == 3) {
            val year = parts[0]
            val month = parts[1]
            val day = parts[2]
            "$month/$day/$year"
        } else {
            backendDate
        }
    } catch (e: Exception) {
        backendDate
    }
}

// Convert MM/DD/YYYY from display to YYYY-MM-DD for backend
private fun formatDateForBackend(displayDate: String): String {
    return try {
        val parts = displayDate.split("/")
        if (parts.size == 3) {
            val month = parts[0].padStart(2, '0')
            val day = parts[1].padStart(2, '0')
            val year = parts[2]
            "$year-$month-$day"
        } else {
            displayDate
        }
    } catch (e: Exception) {
        displayDate
    }
}

@Preview
@Composable
fun ProfileScreenPreview() {
    ProfileScreen()
}