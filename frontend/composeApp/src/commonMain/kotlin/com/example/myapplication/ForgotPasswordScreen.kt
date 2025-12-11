package com.example.myapplication

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.painterResource
import myapplication.composeapp.generated.resources.Res
import myapplication.composeapp.generated.resources.care_capsule_logo
import myapplication.composeapp.generated.resources.login_background
import com.example.myapplication.viewmodel.ForgotPasswordViewModel
import com.example.myapplication.theme.CareCapsuleTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextAlign

@Composable
fun ForgotPasswordScreen(
    onBackToLogin: () -> Unit = {},
    viewModel: ForgotPasswordViewModel = viewModel { ForgotPasswordViewModel() }
) {
    val email by viewModel.email.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSent by viewModel.isSent.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val emailOk = remember(email) { email.isBlank() || isValidEmail(email) }
    val canSubmit = email.isNotBlank() && emailOk && !isLoading

    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        Image(
            painter = painterResource(Res.drawable.login_background),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Foreground overlay + content
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
                // Logo
                Image(
                    painter = painterResource(Res.drawable.care_capsule_logo),
                    contentDescription = "Care Capsule Logo",
                    modifier = Modifier
                        .width(310.dp)
                        .height(95.dp)
                )

                Spacer(Modifier.height(24.dp))

                // Title
                Text(
                    text = "Forgot Password",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Enter the email associated with your account.",
                    color = Color.Black,
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(32.dp))

                // Card container
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
                        // Email field
                        Text(
                            text = "Email",
                            color = Color.Black,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                if (errorMessage != null || isSent) viewModel.clearBanners()
                                viewModel.setEmail(it)
                            },
                            placeholder = { Text("johndoe@gmail.com", color = Color(0xFF949292)) },
                            singleLine = true,
                            isError = email.isNotBlank() && !emailOk,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        AnimatedVisibility(
                            visible = email.isNotBlank() && !emailOk,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Text(
                                "Enter a valid email, e.g. name@example.com",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // Error banner
                        AnimatedVisibility(
                            visible = errorMessage != null,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = errorMessage ?: "",
                                    color = Color(0xFFD32F2F),
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        // Success banner
                        AnimatedVisibility(
                            visible = isSent && errorMessage == null,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = if (errorMessage != null) 8.dp else 0.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "If that email exists, we’ve sent a reset link.\nCheck your inbox and spam folder.",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Submit button
                        Button(
                            onClick = { viewModel.requestReset() }, // will call API later
                            enabled = canSubmit,
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
                                Text("Send reset link", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Back to login
                        TextButton(
                            onClick = onBackToLogin,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = "Back to Login",
                                color = MaterialTheme.colorScheme.primary,
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

// same validator style as your other screens
private fun isValidEmail(s: String): Boolean {
    val re = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    return re.matches(s.trim())
}

@Preview
@Composable
fun ForgotPasswordScreenPreview() {
    CareCapsuleTheme {
        ForgotPasswordScreen(
            onBackToLogin = {}
        )
    }
}
