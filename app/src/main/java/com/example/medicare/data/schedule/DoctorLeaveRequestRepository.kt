package com.example.medicare.data.schedule

import com.example.medicare.model.DoctorLeaveRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class DoctorLeaveRequestRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun submit(date: String, reason: String, callback: (Result<Unit>) -> Unit) {
        val uid = auth.currentUser?.uid ?: return callback(Result.failure(IllegalStateException("Vui lòng đăng nhập lại.")))
        val request = firestore.collection(COLLECTION).document()
        request.set(mapOf(
            "id" to request.id,
            "doctorId" to uid,
            "date" to date,
            "reason" to reason,
            "status" to PENDING,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )).addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun getMine(callback: (Result<List<DoctorLeaveRequest>>) -> Unit) {
        val uid = auth.currentUser?.uid ?: return callback(Result.failure(IllegalStateException("Vui lòng đăng nhập lại.")))
        firestore.collection(COLLECTION).whereEqualTo("doctorId", uid).get()
            .addOnSuccessListener { snapshot ->
                callback(Result.success(snapshot.documents.map { doc ->
                    DoctorLeaveRequest(
                        id = doc.id,
                        doctorId = uid,
                        date = doc.getString("date").orEmpty(),
                        reason = doc.getString("reason").orEmpty(),
                        status = doc.getString("status").orEmpty().ifBlank { PENDING },
                        createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                    )
                }.sortedByDescending { it.createdAt }))
            }.addOnFailureListener { callback(Result.failure(it)) }
    }

    fun getPending(callback: (Result<List<DoctorLeaveRequest>>) -> Unit) {
        firestore.collection(COLLECTION).whereEqualTo("status", PENDING).get()
            .addOnSuccessListener { snapshot ->
                callback(Result.success(snapshot.documents.map { doc ->
                    DoctorLeaveRequest(doc.id, doc.getString("doctorId").orEmpty(), doc.getString("date").orEmpty(), doc.getString("reason").orEmpty(), PENDING, doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L)
                }.sortedBy { it.date }))
            }.addOnFailureListener { callback(Result.failure(it)) }
    }

    fun decide(request: DoctorLeaveRequest, approved: Boolean, callback: (Result<Unit>) -> Unit) {
        firestore.collection("doctorSchedules")
            .whereEqualTo("doctorId", request.doctorId)
            .whereEqualTo("workDate", request.date)
            .get()
            .addOnSuccessListener { schedules ->
                firestore.collection("appointments").whereEqualTo("doctorId", request.doctorId).get()
                    .addOnSuccessListener { appointments ->
                        val isShiftChange = request.reason.startsWith("Đổi ca:")
                        val affected = if (approved && !isShiftChange) appointments.documents.filter { document ->
                            document.getString("appointmentDate") == request.date &&
                                document.getString("status") in listOf("PENDING", "CONFIRMED", "ARRIVED")
                        } else emptyList()
                        val batch = firestore.batch()
                        batch.update(
                            firestore.collection(COLLECTION).document(request.id),
                            mapOf("status" to if (approved) APPROVED else REJECTED, "updatedAt" to FieldValue.serverTimestamp()),
                        )
                        if (approved && !isShiftChange) schedules.documents.forEach { schedule ->
                            batch.update(schedule.reference, mapOf("isLocked" to true, "updatedAt" to FieldValue.serverTimestamp()))
                        }
                        affected.forEach { appointment ->
                            batch.update(appointment.reference, mapOf("requiresReschedule" to true, "updatedAt" to FieldValue.serverTimestamp()))
                            val patientId = appointment.getString("patientId").orEmpty()
                            if (patientId.isNotBlank()) batch.setNotification(
                                patientId,
                                "Lịch khám cần đổi giờ",
                                "Bác sĩ có lịch nghỉ ngày ${request.date}. MediCare sẽ liên hệ để sắp xếp lại lịch khám của bạn.",
                                "APPOINTMENT_RESCHEDULE_NEEDED",
                                appointment.id,
                            )
                        }
                        batch.setNotification(
                            request.doctorId,
                            if (approved) "Yêu cầu nghỉ đã được duyệt" else "Yêu cầu nghỉ đã bị từ chối",
                            when {
                                !approved -> "Yêu cầu ngày ${request.date} chưa được chấp thuận."
                                isShiftChange -> "Đề nghị đổi ca ngày ${request.date} đã được duyệt. Admin sẽ cập nhật ca mới."
                                else -> "Lịch làm việc ngày ${request.date} đã được khóa."
                            },
                            "LEAVE_REQUEST",
                            "",
                        )
                        batch.commit().addOnSuccessListener { callback(Result.success(Unit)) }
                            .addOnFailureListener { callback(Result.failure(it)) }
                    }.addOnFailureListener { callback(Result.failure(it)) }
            }.addOnFailureListener { callback(Result.failure(it)) }
    }

    private fun com.google.firebase.firestore.WriteBatch.setNotification(
        userId: String,
        title: String,
        body: String,
        type: String,
        appointmentId: String,
    ) {
        val document = firestore.collection("notifications").document()
        set(document, mapOf(
            "id" to document.id,
            "userId" to userId,
            "title" to title,
            "body" to body,
            "type" to type,
            "appointmentId" to appointmentId,
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp(),
        ))
    }

    private companion object {
        const val COLLECTION = "doctorLeaveRequests"
        const val PENDING = "PENDING"
        const val APPROVED = "APPROVED"
        const val REJECTED = "REJECTED"
    }
}
