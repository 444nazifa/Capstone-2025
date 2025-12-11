package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class ChatMessageUi(
    val text: String,
    val isUser: Boolean
)

@Composable
fun ChatbotScreen(
    modifier: Modifier = Modifier,
    reminders: List<MedicationReminder> = emptyList(),
    medications: List<com.example.myapplication.data.UserMedication> = emptyList()
) {
    var input by remember { mutableStateOf(TextFieldValue("")) }
    val messages = remember {
        mutableStateListOf(
            ChatMessageUi(
                text = "Hi! I'm your CareCapsule assistant. I can help you with reminders, medication information, and drug interactions. How can I help you today?",
                isUser = false
            )
        )
    }

    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "CareCapsule Assistant",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))
        Divider()
        Spacer(modifier = Modifier.height(8.dp))

        // Messages
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = false
        ) {
            items(messages) { msg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (msg.isUser) 16.dp else 0.dp,
                                    bottomEnd = if (msg.isUser) 0.dp else 16.dp
                                )
                            )
                            .background(
                                if (msg.isUser)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.secondaryContainer
                            )
                            .padding(12.dp)
                            .widthIn(max = 260.dp)
                    ) {
                        Text(
                            text = msg.text,
                            color = if (msg.isUser)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about medications, interactions, or reminders") },
                maxLines = 3
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    val text = input.text.trim()
                    if (text.isNotEmpty()) {
                        messages.add(ChatMessageUi(text = text, isUser = true))
                        input = TextFieldValue("")

                        scope.launch {
                            try {
                                val reminderPayloads = reminders.map {
                                    ReminderPayload(
                                        name = it.name,
                                        time = it.time,
                                        notes = it.dosage
                                    )
                                }

                                val medicationPayloads = medications.map {
                                    MedicationPayload(
                                        name = it.medicationName,
                                        dosage = it.dosage,
                                        frequency = it.frequency,
                                        instructions = it.instructions
                                    )
                                }

                                val response = ChatApi.sendMessage(
                                    message = text,
                                    reminders = reminderPayloads,
                                    medications = medicationPayloads
                                )
                                messages.add(
                                    ChatMessageUi(
                                        text = response.reply,
                                        isUser = false
                                    )
                                )
                            } catch (e: Exception) {
                                messages.add(
                                    ChatMessageUi(
                                        text = "Sorry, I couldn't reach the assistant. Please try again.",
                                        isUser = false
                                    )
                                )
                            }
                        }
                    }
                }
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}
