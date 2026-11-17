package com.example.medicare.model

data class DoctorSchedule(
    val id: String,
    val doctorId: String,
    val doctorName: String,
    val specialtyName: String,
    val workDate: String,
    val shiftId: String,
    val shiftName: String,
    val startTime: String,
    val endTime: String,
    val room: String,
    val appointmentDurationMinutes: Int,
    val maximumAppointments: Int,
    val isLocked: Boolean,
    val note: String,
)
