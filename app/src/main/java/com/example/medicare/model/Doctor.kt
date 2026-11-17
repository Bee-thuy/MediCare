package com.example.medicare.model

data class Doctor(
    val id: String,
    val fullName: String,
    val specialtyId: String,
    val specialty: String,
    val email: String,
    val phone: String,
    val experienceYears: String,
    val qualification: String,
    val biography: String,
    val isActive: Boolean,
)
