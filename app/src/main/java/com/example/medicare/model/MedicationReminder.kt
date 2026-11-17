package com.example.medicare.model

data class MedicationReminder(
    val id: String,
    val patientId: String,
    val medicationName: String,
    val dosage: String,
    val reminderTime: String,
    val frequency: String,
    val isActive: Boolean,
    val lastTakenDate: String,
)
