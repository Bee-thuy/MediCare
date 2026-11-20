package com.example.medicare.model

data class AppNotification(
    val id: String,
    val userId: String,
    val title: String,
    val body: String,
    val type: String,
    val appointmentId: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = 0L,
    val audience: String = "",
)
