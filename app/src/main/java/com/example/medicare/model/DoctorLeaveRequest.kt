package com.example.medicare.model

data class DoctorLeaveRequest(
    val id: String,
    val doctorId: String,
    val date: String,
    val reason: String,
    val status: String,
    val createdAt: Long,
)
