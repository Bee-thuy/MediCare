package com.example.medicare.model

data class WorkShift(
    val id: String,
    val name: String,
    val startTime: String,
    val endTime: String,
    val description: String,
    val isActive: Boolean,
)
