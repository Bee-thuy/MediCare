package com.example.medicare.data.reminder

import com.example.medicare.model.MedicationReminder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MedicationReminderRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun getReminders(callback: (Result<List<MedicationReminder>>) -> Unit) {
        val patientId = auth.currentUser?.uid ?: run {
            callback(Result.failure(IllegalStateException("Vui lòng đăng nhập lại.")))
            return
        }
        firestore.collection(REMINDERS_COLLECTION).whereEqualTo("patientId", patientId).get()
            .addOnSuccessListener { snapshot ->
                callback(
                    Result.success(
                        snapshot.documents.map { document ->
                            MedicationReminder(
                                id = document.id,
                                patientId = patientId,
                                medicationName = document.getString("medicationName").orEmpty(),
                                dosage = document.getString("dosage").orEmpty(),
                                reminderTime = document.getString("reminderTime").orEmpty(),
                                frequency = document.getString("frequency").orEmpty(),
                                isActive = document.getBoolean("isActive") ?: true,
                                lastTakenDate = document.getString("lastTakenDate").orEmpty(),
                            )
                        }.sortedBy { it.reminderTime },
                    ),
                )
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun addReminder(
        medicationName: String,
        dosage: String,
        reminderTime: String,
        frequency: String,
        callback: (Result<String>) -> Unit,
    ) {
        val patientId = auth.currentUser?.uid ?: run {
            callback(Result.failure(IllegalStateException("Vui lòng đăng nhập lại.")))
            return
        }
        val document = firestore.collection(REMINDERS_COLLECTION).document()
        document.set(
            hashMapOf(
                "id" to document.id,
                "patientId" to patientId,
                "medicationName" to medicationName,
                "dosage" to dosage,
                "reminderTime" to reminderTime,
                "frequency" to frequency,
                "isActive" to true,
                "lastTakenDate" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        )
            .addOnSuccessListener { callback(Result.success(document.id)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun markTaken(reminderId: String, date: String, callback: (Result<Unit>) -> Unit) {
        firestore.collection(REMINDERS_COLLECTION).document(reminderId)
            .update(mapOf("lastTakenDate" to date, "updatedAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun setActive(reminderId: String, active: Boolean, callback: (Result<Unit>) -> Unit) {
        firestore.collection(REMINDERS_COLLECTION).document(reminderId)
            .update(mapOf("isActive" to active, "updatedAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun updateReminder(reminderId: String, medicationName: String, dosage: String,
                       reminderTime: String, frequency: String, callback: (Result<Unit>) -> Unit) {
        firestore.collection(REMINDERS_COLLECTION).document(reminderId)
            .update(mapOf("medicationName" to medicationName, "dosage" to dosage,
                "reminderTime" to reminderTime, "frequency" to frequency,
                "updatedAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    private companion object {
        const val REMINDERS_COLLECTION = "medicationReminders"
    }
}
