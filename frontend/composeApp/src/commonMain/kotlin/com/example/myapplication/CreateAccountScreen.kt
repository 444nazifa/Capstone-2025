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
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.painterResource
import myapplication.composeapp.generated.resources.Res
import myapplication.composeapp.generated.resources.care_capsule_logo
import myapplication.composeapp.generated.resources.login_background
import kotlinx.datetime.*
import com.example.myapplication.viewmodel.CreateAccountViewModel
import com.example.myapplication.data.AuthState

@Composable
fun CreateAccountScreen(
    onSignUpSuccess: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    viewModel: CreateAccountViewModel = viewModel { CreateAccountViewModel() }
) {
    // --- form state ---
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") } // MM/DD/YYYY

    // Collect state from ViewModel
    val authState by viewModel.authState.collectAsState()
    val nameError by viewModel.nameError.collectAsState()
    val emailError by viewModel.emailError.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()
    val dobError by viewModel.dobError.collectAsState()

    // --- validation state (mirrors Profile) ---
    val emailOk = remember(email) { email.isBlank() || isValidEmail(email) }
    val passwordOk = remember(password) { password.isBlank() || isValidPassword(password) }
    val dobParsed = remember(dob) { parseDobOrNull(dob) }
    val dobOk = remember(dob, dobParsed) {
        dob.isNotBlank() && dobParsed != null && isReasonableDob(dobParsed)
    }


    // Sign Up enabled only when everything looks good
    val formValid =
        name.isNotBlank() &&
                email.isNotBlank() && emailOk &&
                password.isNotBlank() && passwordOk &&
                dobOk

    val isLoading = authState is AuthState.Loading

    // Handle success state
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onSignUpSuccess()
        }
    }

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
                            isError = password.isNotBlank() && !passwordOk,
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
                            visible = password.isNotBlank() && !passwordOk,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Text(
                                "Password must be at least 8 characters, including uppercase, lowercase, digit, and special character",
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

                        // API Error Message
                        AnimatedVisibility(
                            visible = authState is AuthState.Error,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = (authState as? AuthState.Error)?.message ?: "An error occurred",
                                    color = Color(0xFFD32F2F),
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (authState is AuthState.Error) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // SIGN UP BUTTON
                        Button(
                            onClick = {
                                viewModel.register(name, email, password, dob)
                            },
                            enabled = formValid && !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50),
                                disabledContainerColor = Color(0xFFBDBDBD)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text("Sign Up", color = Color.White, fontWeight = FontWeight.Bold)
                            }
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
    val re = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)
    return re.matches(s)
}

// Password
private fun isValidPassword(password: String): Boolean {
    if (password.length < 8) return false

    val hasLowercase = password.any { it.isLowerCase() }
    val hasUppercase = password.any { it.isUpperCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecialChar = password.any { it in "@$!%*?&" }

    return hasLowercase && hasUppercase && hasDigit && hasSpecialChar
}

// Date of Birth
private fun parseDobOrNull(s: String): LocalDate? {
    return try {
        // MM/DD/YYYY
        val parts = s.split("/")
        if (parts.size != 3) return null

        val month = parts[0].toIntOrNull() ?: return null
        val day = parts[1].toIntOrNull() ?: return null
        val year = parts[2].toIntOrNull() ?: return null

        LocalDate(year, month, day)
    } catch (e: Exception) {
        null
    }
}

private fun isReasonableDob(date: LocalDate): Boolean {
    val now = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val age = now.year - date.year - if (now.dayOfYear < date.dayOfYear) 1 else 0

    return age in 0..120
}

private fun formatDobInput(dob: String): String {
    // MM/DD/YYYY
    val year = dob.substring(6, 10)
    val month = dob.substring(0, 2)
    val day = dob.substring(3, 5)

    return "$month/$day/$year"
}
