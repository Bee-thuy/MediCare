package com.example.medicare.model

data class Patient(
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val accountStatus: AccountStatus,
)
