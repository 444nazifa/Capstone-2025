package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.ChatApi
import com.example.myapplication.ChatMessageUi
import com.example.myapplication.MedicationPayload
import com.example.myapplication.ReminderPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessageUi>>(
        listOf(
            ChatMessageUi(
                text = "Hi! I'm your CareCapsule assistant. I can help you with reminders, medication information, and drug interactions. How can I help you today?",
                isUser = false
            )
        )
    )
    val messages: StateFlow<List<ChatMessageUi>> = _messages

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    fun sendMessage(
        text: String,
        reminders: List<ReminderPayload>,
        medications: List<MedicationPayload>
    ) {
        if (text.isBlank() || _isSending.value) return

        // Add user message
        _messages.value = _messages.value + ChatMessageUi(text = text, isUser = true)
        _isSending.value = true

        viewModelScope.launch {
            try {
                val response = ChatApi.sendMessage(
                    message = text,
                    reminders = reminders,
                    medications = medications
                )
                _messages.value = _messages.value + ChatMessageUi(
                    text = response.reply,
                    isUser = false
                )
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessageUi(
                    text = "Sorry, I couldn't reach the assistant. Please try again.",
                    isUser = false
                )
            } finally {
                _isSending.value = false
            }
        }
    }

    fun clearChat() {
        _messages.value = listOf(
            ChatMessageUi(
                text = "Hi! I'm your CareCapsule assistant. I can help you with reminders, medication information, and drug interactions. How can I help you today?",
                isUser = false
            )
        )
    }
}
