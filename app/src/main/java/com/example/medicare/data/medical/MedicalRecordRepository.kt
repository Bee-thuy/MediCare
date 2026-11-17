package com.example.medicare.data.medical

import com.example.medicare.model.Appointment
import com.example.medicare.model.MedicalRecord
import com.example.medicare.model.PrescriptionItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MedicalRecordRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun completeAppointment(
        appointment: Appointment,
        symptoms: String,
        diagnosis: String,
        treatmentNotes: String,
        instructions: String,
        prescriptionItems: List<PrescriptionItem>,
        callback: (Result<Unit>) -> Unit,
    ) {
        val doctorId = auth.currentUser?.uid ?: run {
            callback(Result.failure(IllegalStateException("Vui lòng đăng nhập lại.")))
            return
        }
        if (doctorId != appointment.doctorId) {
            callback(Result.failure(IllegalStateException("Bạn không có quyền hoàn thành lịch khám này.")))
            return
        }
        val record = firestore.collection(MEDICAL_RECORDS_COLLECTION).document(appointment.id)
        val appointmentDocument = firestore.collection(APPOINTMENTS_COLLECTION).document(appointment.id)
        firestore.batch()
            .set(
                record,
                hashMapOf(
                    "id" to record.id,
                    "appointmentId" to appointment.id,
                    "patientId" to appointment.patientId,
                    "doctorId" to appointment.doctorId,
                    "doctorName" to appointment.doctorName,
                    "specialtyName" to appointment.specialtyName,
                    "appointmentDate" to appointment.appointmentDate,
                    "symptoms" to symptoms,
                    "diagnosis" to diagnosis,
                    "treatmentNotes" to treatmentNotes,
                    "instructions" to instructions,
                    "prescriptionItems" to prescriptionItems.map {
                        hashMapOf(
                            "name" to it.name,
                            "dosage" to it.dosage,
                            "instructions" to it.instructions,
                            "durationDays" to it.durationDays,
                        )
                    },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .update(
                appointmentDocument,
                mapOf(
                    "status" to "COMPLETED",
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .commit()
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun getMedicalRecord(appointmentId: String, callback: (Result<MedicalRecord?>) -> Unit) {
        firestore.collection(MEDICAL_RECORDS_COLLECTION).document(appointmentId).get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    callback(Result.success(null))
                    return@addOnSuccessListener
                }
                callback(
                    Result.success(
                        MedicalRecord(
                            id = document.getString("id").orEmpty().ifBlank { document.id },
                            appointmentId = document.getString("appointmentId").orEmpty(),
                            patientId = document.getString("patientId").orEmpty(),
                            doctorId = document.getString("doctorId").orEmpty(),
                            doctorName = document.getString("doctorName").orEmpty(),
                            specialtyName = document.getString("specialtyName").orEmpty(),
                            appointmentDate = document.getString("appointmentDate").orEmpty(),
                            symptoms = document.getString("symptoms").orEmpty(),
                            diagnosis = document.getString("diagnosis").orEmpty(),
                            treatmentNotes = document.getString("treatmentNotes").orEmpty(),
                            instructions = document.getString("instructions").orEmpty(),
                            prescriptionItems = (document.get("prescriptionItems") as? List<*>)
                                .orEmpty()
                                .mapNotNull { it as? Map<*, *> }
                                .mapNotNull { item ->
                                    val name = item["name"] as? String ?: return@mapNotNull null
                                    PrescriptionItem(
                                        name = name,
                                        dosage = item["dosage"] as? String ?: "",
                                        instructions = item["instructions"] as? String ?: "",
                                        durationDays = item["durationDays"] as? String ?: "",
                                    )
                                },
                        ),
                    ),
                )
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun getRecordsForPatient(patientId: String, callback: (Result<List<MedicalRecord>>) -> Unit) {
        firestore.collection(MEDICAL_RECORDS_COLLECTION)
            .whereEqualTo("patientId", patientId)
            .get()
            .addOnSuccessListener { snapshot ->
                callback(Result.success(snapshot.documents.mapNotNull { document ->
                    val appointmentId = document.getString("appointmentId") ?: return@mapNotNull null
                    MedicalRecord(
                        id = document.getString("id").orEmpty().ifBlank { document.id },
                        appointmentId = appointmentId,
                        patientId = patientId,
                        doctorId = document.getString("doctorId").orEmpty(),
                        doctorName = document.getString("doctorName").orEmpty(),
                        specialtyName = document.getString("specialtyName").orEmpty(),
                        appointmentDate = document.getString("appointmentDate").orEmpty(),
                        symptoms = document.getString("symptoms").orEmpty(),
                        diagnosis = document.getString("diagnosis").orEmpty(),
                        treatmentNotes = document.getString("treatmentNotes").orEmpty(),
                        instructions = document.getString("instructions").orEmpty(),
                        prescriptionItems = emptyList(),
                    )
                }.sortedByDescending { it.appointmentDate }))
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun getRecordsForDoctor(doctorId: String, callback: (Result<List<MedicalRecord>>) -> Unit) {
        firestore.collection(MEDICAL_RECORDS_COLLECTION)
            .whereEqualTo("doctorId", doctorId)
            .get()
            .addOnSuccessListener { snapshot ->
                callback(Result.success(snapshot.documents.mapNotNull { document ->
                    val appointmentId = document.getString("appointmentId") ?: return@mapNotNull null
                    MedicalRecord(
                        id = document.getString("id").orEmpty().ifBlank { document.id },
                        appointmentId = appointmentId,
                        patientId = document.getString("patientId").orEmpty(),
                        doctorId = doctorId,
                        doctorName = document.getString("doctorName").orEmpty(),
                        specialtyName = document.getString("specialtyName").orEmpty(),
                        appointmentDate = document.getString("appointmentDate").orEmpty(),
                        symptoms = document.getString("symptoms").orEmpty(),
                        diagnosis = document.getString("diagnosis").orEmpty(),
                        treatmentNotes = document.getString("treatmentNotes").orEmpty(),
                        instructions = document.getString("instructions").orEmpty(),
                        prescriptionItems = (document.get("prescriptionItems") as? List<*>).orEmpty()
                            .mapNotNull { it as? Map<*, *> }
                            .mapNotNull { item ->
                                val name = item["name"] as? String ?: return@mapNotNull null
                                PrescriptionItem(name, item["dosage"] as? String ?: "",
                                    item["instructions"] as? String ?: "", item["durationDays"] as? String ?: "")
                            },
                    )
                }.sortedByDescending { it.appointmentDate }))
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    private companion object {
        const val APPOINTMENTS_COLLECTION = "appointments"
        const val MEDICAL_RECORDS_COLLECTION = "medicalRecords"
    }
}
