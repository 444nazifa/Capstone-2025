package com.example.myapplication

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

    // 🔹 Use Box to layer background + content
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
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
                .background(Color.White.copy(alpha = 0.85f)) // optional slight white overlay
                .padding(24.dp),
            color = Color.Transparent // keep transparent since background image already visible
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
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

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
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

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
                            onValueChange = { dob = it },
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

                        Spacer(modifier = Modifier.height(24.dp))

                        // SIGN UP BUTTON
                        Button(
                            onClick = onSignUp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("Sign Up", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp)) //Linnks are outside of the card

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
                            TextButton(
                                onClick = onLoginClick,
                                contentPadding = PaddingValues(0.dp) // keeps it tight like your design
                            ) {
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

@Preview
@Composable
fun CreateAccountScreenPreview() {
    MaterialTheme {
        CreateAccountScreen()
    }
}
