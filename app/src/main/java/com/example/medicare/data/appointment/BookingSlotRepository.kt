package com.example.medicare.data.appointment

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FieldValue
import java.time.LocalDate

/** Public availability only. Patient details remain in the protected appointments collection. */
class BookingSlotRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun observeReservedForDoctor(
        doctorId: String,
        callback: (Result<Set<String>>) -> Unit,
    ): ListenerRegistration = firestore.collection(COLLECTION)
        .whereEqualTo("doctorId", doctorId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                callback(Result.failure(error))
                return@addSnapshotListener
            }
            callback(Result.success(snapshot?.documents.orEmpty()
                .filter { it.getString("state") == RESERVED }
                .mapTo(mutableSetOf()) { it.id }))
        }

    /** Admin migration for appointments saved before public slot availability existed. */
    fun backfillExistingAppointments() {
        firestore.collection("appointments").get().addOnSuccessListener { appointments ->
            appointments.documents.filter { it.getString("status") in setOf(
                "PENDING", "CONFIRMED", "ARRIVED", "IN_PROGRESS", "COMPLETED") &&
                it.getString("appointmentDate").orEmpty() >= LocalDate.now().toString() }.forEach { appointment ->
                val scheduleId = appointment.getString("scheduleId").orEmpty()
                val startTime = appointment.getString("startTime").orEmpty()
                val doctorId = appointment.getString("doctorId").orEmpty()
                if (scheduleId.isBlank() || startTime.isBlank() || doctorId.isBlank()) return@forEach
                val slotRef = firestore.collection(COLLECTION).document(id(scheduleId, startTime))
                firestore.runTransaction { transaction ->
                    val latestAppointment = transaction.get(appointment.reference)
                    val current = transaction.get(slotRef)
                    if (latestAppointment.getString("status") in setOf(
                            "PENDING", "CONFIRMED", "ARRIVED", "IN_PROGRESS", "COMPLETED") &&
                        (!current.exists() || current.getString("state") == FREE)) {
                        transaction.set(slotRef, mapOf(
                            "id" to slotRef.id,
                            "doctorId" to doctorId,
                            "scheduleId" to scheduleId,
                            "startTime" to startTime,
                            "appointmentId" to appointment.id,
                            "state" to RESERVED,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ))
                    }
                    Unit
                }
            }
        }
    }

    companion object {
        const val COLLECTION = "bookingSlots"
        const val RESERVED = "RESERVED"
        const val FREE = "FREE"
        fun id(scheduleId: String, startTime: String) =
            "${scheduleId}_${startTime.replace(":", "")}"
    }
}
