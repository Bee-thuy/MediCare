package com.example.medicare.data.appointment

import com.example.medicare.model.AppointmentStatus
import com.example.medicare.model.Appointment
import com.example.medicare.model.DoctorSchedule
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.LocalTime

class AppointmentRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun getAppointments(callback: (Result<List<Appointment>>) -> Unit) {
        firestore.collection(APPOINTMENTS_COLLECTION)
            .get()
            .addOnSuccessListener { snapshot ->
                val appointments = snapshot.documents.mapNotNull { document ->
                    val patientId = document.getString("patientId") ?: return@mapNotNull null
                    val doctorId = document.getString("doctorId") ?: return@mapNotNull null
                    val scheduleId = document.getString("scheduleId") ?: return@mapNotNull null
                    Appointment(
                        id = document.getString("id").orEmpty().ifBlank { document.id },
                        patientId = patientId,
                        patientName = document.getString("patientName").orEmpty(),
                        doctorId = doctorId,
                        doctorName = document.getString("doctorName").orEmpty(),
                        specialtyName = document.getString("specialtyName").orEmpty(),
                        scheduleId = scheduleId,
                        appointmentDate = document.getString("appointmentDate").orEmpty(),
                        startTime = document.getString("startTime").orEmpty(),
                        endTime = document.getString("endTime").orEmpty(),
                        reason = document.getString("reason").orEmpty(),
                        status = AppointmentStatus.entries.firstOrNull {
                            it.name == document.getString("status")
                        } ?: AppointmentStatus.PENDING,
                    )
                }.sortedWith(
                    compareBy<Appointment> { it.appointmentDate }
                        .thenBy { it.startTime },
                )
                callback(Result.success(appointments))
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun getAppointmentsForDoctor(
        doctorId: String,
        callback: (Result<List<Appointment>>) -> Unit,
    ) {
        firestore.collection(APPOINTMENTS_COLLECTION)
            .whereEqualTo("doctorId", doctorId)
            .get()
            .addOnSuccessListener { snapshot ->
                val appointments = snapshot.documents.mapNotNull { document ->
                    val patientId = document.getString("patientId") ?: return@mapNotNull null
                    val scheduleId = document.getString("scheduleId") ?: return@mapNotNull null
                    Appointment(
                        id = document.getString("id").orEmpty().ifBlank { document.id },
                        patientId = patientId,
                        patientName = document.getString("patientName").orEmpty(),
                        doctorId = doctorId,
                        doctorName = document.getString("doctorName").orEmpty(),
                        specialtyName = document.getString("specialtyName").orEmpty(),
                        scheduleId = scheduleId,
                        appointmentDate = document.getString("appointmentDate").orEmpty(),
                        startTime = document.getString("startTime").orEmpty(),
                        endTime = document.getString("endTime").orEmpty(),
                        reason = document.getString("reason").orEmpty(),
                        status = AppointmentStatus.entries.firstOrNull {
                            it.name == document.getString("status")
                        } ?: AppointmentStatus.PENDING,
                    )
                }.sortedWith(
                    compareBy<Appointment> { it.appointmentDate }
                        .thenBy { it.startTime },
                )
                callback(Result.success(appointments))
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun getAppointmentsForPatient(
        patientId: String,
        callback: (Result<List<Appointment>>) -> Unit,
    ) {
        firestore.collection(APPOINTMENTS_COLLECTION)
            .whereEqualTo("patientId", patientId)
            .get()
            .addOnSuccessListener { snapshot ->
                val appointments = snapshot.documents.mapNotNull { document ->
                    val doctorId = document.getString("doctorId") ?: return@mapNotNull null
                    val scheduleId = document.getString("scheduleId") ?: return@mapNotNull null
                    Appointment(
                        id = document.getString("id").orEmpty().ifBlank { document.id },
                        patientId = patientId,
                        patientName = document.getString("patientName").orEmpty(),
                        doctorId = doctorId,
                        doctorName = document.getString("doctorName").orEmpty(),
                        specialtyName = document.getString("specialtyName").orEmpty(),
                        scheduleId = scheduleId,
                        appointmentDate = document.getString("appointmentDate").orEmpty(),
                        startTime = document.getString("startTime").orEmpty(),
                        endTime = document.getString("endTime").orEmpty(),
                        reason = document.getString("reason").orEmpty(),
                        status = AppointmentStatus.entries.firstOrNull {
                            it.name == document.getString("status")
                        } ?: AppointmentStatus.PENDING,
                    )
                }.sortedWith(
                    compareByDescending<Appointment> { it.appointmentDate }
                        .thenByDescending { it.startTime },
                )
                callback(Result.success(appointments))
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun updateAppointmentStatus(
        appointmentId: String,
        status: AppointmentStatus,
        callback: (Result<Unit>) -> Unit,
    ) {
        firestore.collection(APPOINTMENTS_COLLECTION).document(appointmentId)
            .update(
                mapOf(
                    "status" to status.name,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    /** Updates an appointment and writes the patient's in-app notification in the same batch. */
    fun updateAppointmentStatusWithNotification(
        appointmentId: String,
        status: AppointmentStatus,
        title: String,
        body: String,
        callback: (Result<Unit>) -> Unit,
    ) {
        val appointment = firestore.collection(APPOINTMENTS_COLLECTION).document(appointmentId)
        appointment.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) return@addOnSuccessListener callback(Result.failure(NoSuchElementException("Không tìm thấy lịch hẹn.")))
            val patientId = snapshot.getString("patientId").orEmpty()
            val batch = firestore.batch()
            batch.update(appointment, mapOf("status" to status.name, "updatedAt" to FieldValue.serverTimestamp()))
            if (patientId.isNotBlank()) {
                val notification = firestore.collection(NOTIFICATIONS_COLLECTION).document()
                batch.set(notification, mapOf(
                    "id" to notification.id,
                    "userId" to patientId,
                    "title" to title,
                    "body" to body,
                    "type" to "APPOINTMENT_${status.name}",
                    "appointmentId" to appointmentId,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                ))
            }
            batch.commit().addOnSuccessListener { callback(Result.success(Unit)) }
                .addOnFailureListener { callback(Result.failure(it)) }
        }.addOnFailureListener { callback(Result.failure(it)) }
    }

    /**
     * Rescheduling preserves the old appointment as cancelled history and reserves a new slot.
     * This avoids silently reopening/overwriting another patient's deterministic slot document.
     */
    fun rescheduleAppointment(
        appointmentId: String,
        newStartTime: String,
        newEndTime: String,
        note: String,
        callback: (Result<String>) -> Unit,
    ) {
        val currentRef = firestore.collection(APPOINTMENTS_COLLECTION).document(appointmentId)
        firestore.runTransaction { transaction ->
            val current = transaction.get(currentRef)
            if (!current.exists()) throw NoSuchElementException("Không tìm thấy lịch hẹn.")
            val scheduleId = current.getString("scheduleId").orEmpty()
            if (scheduleId.isBlank()) throw IllegalStateException("Lịch hẹn thiếu thông tin ca làm việc.")
            val newId = "${scheduleId}_${newStartTime.replace(":", "")}"
            val newRef = firestore.collection(APPOINTMENTS_COLLECTION).document(newId)
            val occupied = transaction.get(newRef)
            if (occupied.exists() && occupied.id != current.id &&
                occupied.getString("status") !in listOf(AppointmentStatus.CANCELLED.name, AppointmentStatus.REJECTED.name)
            ) throw IllegalStateException("Khung giờ mới đã có người đặt.")

            val oldReason = listOf("Đổi lịch bởi quản trị viên", note.trim()).filter { it.isNotBlank() }.joinToString(". ")
            transaction.update(currentRef, mapOf(
                "status" to AppointmentStatus.CANCELLED.name,
                "cancellationReason" to oldReason,
                "rescheduledToId" to newId,
                "updatedAt" to FieldValue.serverTimestamp(),
            ))
            val replacement = current.data.orEmpty().toMutableMap().apply {
                put("id", newId)
                put("startTime", newStartTime)
                put("endTime", newEndTime)
                put("status", AppointmentStatus.PENDING.name)
                put("rescheduledFromId", appointmentId)
                put("rescheduleNote", note.trim())
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            transaction.set(newRef, replacement)
            val patientId = current.getString("patientId").orEmpty()
            if (patientId.isNotBlank()) {
                val notification = firestore.collection(NOTIFICATIONS_COLLECTION).document()
                transaction.set(notification, mapOf(
                    "id" to notification.id,
                    "userId" to patientId,
                    "title" to "Lịch khám được đề xuất lại",
                    "body" to "Lịch mới: ${current.getString("appointmentDate").orEmpty()} · $newStartTime - $newEndTime.${if (note.isBlank()) "" else " $note"}",
                    "type" to "APPOINTMENT_RESCHEDULED",
                    "appointmentId" to newId,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                ))
            }
            newId
        }.addOnSuccessListener { callback(Result.success(it)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun startAppointment(appointmentId: String, callback: (Result<Unit>) -> Unit) {
        updateAppointmentStatus(appointmentId, AppointmentStatus.IN_PROGRESS, callback)
    }

    fun markPatientArrived(appointmentId: String, callback: (Result<Unit>) -> Unit) {
        updateAppointmentStatus(appointmentId, AppointmentStatus.ARRIVED, callback)
    }

    fun cancelAppointmentByPatient(
        appointmentId: String,
        reason: String,
        callback: (Result<Unit>) -> Unit,
    ) {
        firestore.collection(APPOINTMENTS_COLLECTION).document(appointmentId)
            .update(
                mapOf(
                    "status" to AppointmentStatus.CANCELLED.name,
                    "cancellationReason" to reason,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun createAppointment(
        schedule: DoctorSchedule,
        startTime: String,
        endTime: String,
        reason: String,
        callback: (Result<Unit>) -> Unit,
    ) {
        val user = auth.currentUser ?: run {
            callback(Result.failure(IllegalStateException("Vui lòng đăng nhập để đặt lịch.")))
            return
        }
        val isExpired = runCatching {
            val date = LocalDate.parse(schedule.workDate)
            date.isBefore(LocalDate.now()) ||
                (date == LocalDate.now() && !LocalTime.parse(endTime).isAfter(LocalTime.now()))
        }.getOrDefault(true)
        if (isExpired) {
            writeAuditLog(user.uid, schedule, startTime, "EXPIRED_SCHEDULE_ATTEMPT")
            callback(Result.failure(IllegalStateException("Khung giờ khám này đã quá hạn. Vui lòng chọn lịch khác.")))
            return
        }
        // One deterministic document per schedule and start time prevents double booking
        // without exposing another patient's appointments to the current user.
        val document = firestore.collection(APPOINTMENTS_COLLECTION)
            .document("${schedule.id}_${startTime.replace(":", "")}")
        firestore.collection(USERS_COLLECTION).document(user.uid).get()
            .addOnSuccessListener { profile ->
                document.set(
                    hashMapOf(
                        "id" to document.id,
                        "patientId" to user.uid,
                        "patientName" to profile.getString("fullName").orEmpty().ifBlank { user.email.orEmpty() },
                        "doctorId" to schedule.doctorId,
                        "doctorName" to schedule.doctorName,
                        "specialtyName" to schedule.specialtyName,
                        "scheduleId" to schedule.id,
                        "appointmentDate" to schedule.workDate,
                        "startTime" to startTime,
                        "endTime" to endTime,
                        "reason" to reason,
                        "status" to AppointmentStatus.PENDING.name,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
                    .addOnSuccessListener { callback(Result.success(Unit)) }
                    .addOnFailureListener { error -> callback(Result.failure(error)) }
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    private fun writeAuditLog(userId: String, schedule: DoctorSchedule, startTime: String, type: String) {
        val document = firestore.collection(AUDIT_LOGS_COLLECTION).document()
        document.set(mapOf(
            "id" to document.id,
            "userId" to userId,
            "type" to type,
            "scheduleId" to schedule.id,
            "doctorId" to schedule.doctorId,
            "workDate" to schedule.workDate,
            "startTime" to startTime,
            "createdAt" to FieldValue.serverTimestamp(),
        ))
    }

    private companion object {
        const val APPOINTMENTS_COLLECTION = "appointments"
        const val USERS_COLLECTION = "users"
        const val NOTIFICATIONS_COLLECTION = "notifications"
        const val AUDIT_LOGS_COLLECTION = "appointmentAuditLogs"
    }
}
