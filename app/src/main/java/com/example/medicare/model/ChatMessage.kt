package com.example.medicare.model

data class ChatMessage(
    val id: String,
    val senderId: String,
    val content: String,
    val readBy: List<String> = emptyList(),
)
