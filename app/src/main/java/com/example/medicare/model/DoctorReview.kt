package com.example.medicare.model

data class DoctorReview(val id: String, val appointmentId: String, val doctorId: String, val doctorName: String, val rating: Int, val comment: String, val createdAt: Long)
