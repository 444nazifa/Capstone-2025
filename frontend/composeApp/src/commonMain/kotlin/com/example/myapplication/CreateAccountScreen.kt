package com.example.myapplication

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.painterResource
import myapplication.composeapp.generated.resources.Res
import myapplication.composeapp.generated.resources.care_capsule_logo
import myapplication.composeapp.generated.resources.login_background
import kotlinx.datetime.*

@Composable
fun CreateAccountScreen(
    onSignUp: () -> Unit = {},
    onLoginClick: () -> Unit = {}
) {
    // --- form state ---
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") } // MM/DD/YYYY

    // --- validation state (mirrors Profile) ---
    val emailOk = remember(email) { email.isBlank() || isValidEmail(email) }
    val dobParsed = remember(dob) { parseDobOrNull(dob) }
    val dobOk = remember(dob, dobParsed) {
        dob.isNotBlank() && dobParsed != null && isReasonableDob(dobParsed)
    }


    // Sign Up enabled only when everything looks good
    val formValid =
        name.isNotBlank() &&
                email.isNotBlank() && emailOk &&
                password.length >= 6 &&
                dobOk

    // Use Box to layer background + content
    Box(modifier = Modifier.fillMaxSize()) {
        // Background Image
        Image(
            painter = painterResource(Res.drawable.login_background),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Foreground overlay for readability
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.85f))
                .padding(24.dp),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // LOGO
                Image(
                    painter = painterResource(Res.drawable.care_capsule_logo),
                    contentDescription = "Care Capsule Logo",
                    modifier = Modifier
                        .width(310.dp)
                        .height(95.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Title
                Text(
                    text = "Create New Account",
                    color = Color(0xFF2E7D32),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Gray Card Background
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F3F3)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        // NAME
                        Text(
                            text = "Name",
                            color = Color.Black,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("Enter Name", color = Color(0xFF949292)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // EMAIL
                        Text(
                            text = "Email",
                            color = Color.Black,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = { Text("johndoe@gmail.com", color = Color(0xFF949292)) },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Email
                            ),
                            isError = email.isNotBlank() && !emailOk,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                        // Email error
                        AnimatedVisibility(
                            visible = email.isNotBlank() && !emailOk,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Text(
                                "Enter a valid email, e.g. name@example.com",
                                color = Color(0xFFD32F2F),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // PASSWORD
                        Text(
                            text = "Password",
                            color = Color.Black,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("Enter Password", color = Color(0xFF949292)) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Password
                            ),
                            isError = password.isNotBlank() && password.length < 6,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                        // Password error
                        AnimatedVisibility(
                            visible = password.isNotBlank() && password.length < 6,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Text(
                                "Password must be at least 6 characters",
                                color = Color(0xFFD32F2F),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // DATE OF BIRTH
                        Text(
                            text = "Date of Birth",
                            color = Color.Black,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = dob,
                            onValueChange = { new ->
                                // keep only digits and cap at 8
                                val digits = new.filter { it.isDigit() }.take(8)

                                // when we have all 8 digits, format once to MM/DD/YYYY
                                dob = if (digits.length == 8) {
                                    formatDobInput(digits)
                                } else {
                                    digits
                                }
                            },
                            placeholder = { Text("MM/DD/YYYY", color = Color(0xFF949292)) },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                        // DOB error
                        AnimatedVisibility(
                            visible = dob.length == 10 && !dobOk,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Text(
                                "Use MM/DD/YYYY, past date, age ≤ 120",
                                color = Color(0xFFD32F2F),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // SIGN UP BUTTON
                        Button(
                            onClick = onSignUp,
                            enabled = formValid,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50),
                                disabledContainerColor = Color(0xFFBDBDBD)
                            )
                        ) {
                            Text("Sign Up", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // LINK: Already Registered? Log in here.
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Already Registered?",
                                color = Color.Black,
                                fontSize = 14.sp
                            )
                            TextButton(onClick = onLoginClick, contentPadding = PaddingValues(0.dp)) {
                                Text(
                                    text = "Log in here.",
                                    color = Color(0xFF388E3C),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- helpers ---

// Email
private fun isValidEmail(s: String): Boolean {
    val re = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    return re.matches(s.trim())
}

// DOB parsing and reasonableness
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

// Formats raw input to MM/DD/YYYY, keeping only digits and inserting slashes
private fun formatDobInput(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(8) // MMDDYYYY
    val sb = StringBuilder()
    for ((i, c) in digits.withIndex()) {
        sb.append(c)
        if (i == 1 || i == 3) sb.append('/')
    }
    return sb.toString()
}

@Preview
@Composable
fun CreateAccountScreenPreview() {
    MaterialTheme {
        CreateAccountScreen()
    }
}
