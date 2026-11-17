package com.example.medicare.model

data class MedicalRecord(
    val id: String,
    val appointmentId: String,
    val patientId: String,
    val doctorId: String,
    val doctorName: String,
    val specialtyName: String,
    val appointmentDate: String,
    val symptoms: String,
    val diagnosis: String,
    val treatmentNotes: String,
    val instructions: String,
    val prescriptionItems: List<PrescriptionItem>,
)
