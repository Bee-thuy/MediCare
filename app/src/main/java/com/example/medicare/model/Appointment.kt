package com.example.medicare.model

data class Appointment(
    val id: String,
    val patientId: String,
    val patientName: String,
    val doctorId: String,
    val doctorName: String,
    val specialtyName: String,
    val scheduleId: String,
    val appointmentDate: String,
    val startTime: String,
    val endTime: String,
    val reason: String,
    val status: AppointmentStatus,
    val rescheduledFromId: String = "",
)
