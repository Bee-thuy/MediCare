package com.example.medicare.data.appointment

import com.example.medicare.model.AppointmentStatus
import com.example.medicare.model.Appointment
import com.example.medicare.model.DoctorSchedule
import com.example.medicare.model.ShiftTimeNormalizer
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
                        rescheduledFromId = document.getString("rescheduledFromId").orEmpty(),
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

    /** Updates an appointment and writes the patient's in-app notification atomically. */
    fun updateAppointmentStatusWithNotification(
        appointmentId: String,
        status: AppointmentStatus,
        title: String,
        body: String,
        callback: (Result<Unit>) -> Unit,
    ) {
        val appointment = firestore.collection(APPOINTMENTS_COLLECTION).document(appointmentId)
        val notification = firestore.collection(NOTIFICATIONS_COLLECTION).document()
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(appointment)
            if (!snapshot.exists()) throw NoSuchElementException("Không tìm thấy lịch hẹn.")
            if (status == AppointmentStatus.REJECTED && snapshot.getString("status") != AppointmentStatus.PENDING.name)
                throw IllegalStateException("Lịch hẹn không còn ở trạng thái chờ duyệt.")
            val slotRef = slotReference(snapshot.getString("scheduleId").orEmpty(), snapshot.getString("startTime").orEmpty())
            val slot = if (status == AppointmentStatus.REJECTED && slotRef != null) transaction.get(slotRef) else null
            val patientId = snapshot.getString("patientId").orEmpty()
            transaction.update(appointment, mapOf("status" to status.name, "updatedAt" to FieldValue.serverTimestamp()))
            if (slot?.getString("appointmentId") == appointmentId && slotRef != null)
                transaction.update(slotRef, mapOf("state" to BookingSlotRepository.FREE,
                    "updatedAt" to FieldValue.serverTimestamp()))
            if (patientId.isNotBlank()) {
                transaction.set(notification, mapOf(
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
            Unit
        }.addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    /** Confirms one pending request only if no other request already owns its doctor and time. */
    fun approveAppointmentWithNotification(
        appointmentId: String,
        callback: (Result<Unit>) -> Unit,
    ) {
        val currentRef = firestore.collection(APPOINTMENTS_COLLECTION).document(appointmentId)
        currentRef.get().addOnSuccessListener { initial ->
            if (!initial.exists()) return@addOnSuccessListener callback(Result.failure(NoSuchElementException("Không tìm thấy lịch hẹn.")))
            val doctorId = initial.getString("doctorId").orEmpty()
            val date = initial.getString("appointmentDate").orEmpty()
            val time = initial.getString("startTime").orEmpty()
            if (doctorId.isBlank() || date.isBlank() || time.isBlank())
                return@addOnSuccessListener callback(Result.failure(IllegalStateException("Lịch hẹn thiếu bác sĩ hoặc thời gian.")))
            firestore.collection(APPOINTMENTS_COLLECTION).whereEqualTo("doctorId", doctorId).get()
                .addOnSuccessListener { snapshot ->
                    val competingRefs = snapshot.documents.filter {
                        it.id != appointmentId && it.getString("appointmentDate") == date && it.getString("startTime") == time
                    }.map { it.reference }
                    val notification = firestore.collection(NOTIFICATIONS_COLLECTION).document()
                    firestore.runTransaction { transaction ->
                        val latest = transaction.get(currentRef)
                        if (!latest.exists() || latest.getString("status") != AppointmentStatus.PENDING.name)
                            throw IllegalStateException("Lịch hẹn không còn ở trạng thái chờ duyệt.")
                        if (latest.getString("doctorId") != doctorId || latest.getString("appointmentDate") != date ||
                            latest.getString("startTime") != time)
                            throw IllegalStateException("Lịch hẹn đã thay đổi. Hãy tải lại danh sách.")
                        val occupied = competingRefs.map(transaction::get).any {
                            it.getString("status") in setOf(AppointmentStatus.CONFIRMED.name,
                                AppointmentStatus.ARRIVED.name, AppointmentStatus.IN_PROGRESS.name,
                                AppointmentStatus.COMPLETED.name)
                        }
                        if (occupied) throw IllegalStateException("Khung giờ này đã có lịch được duyệt. Hãy đề xuất giờ khác.")
                        transaction.update(currentRef, mapOf(
                            "status" to AppointmentStatus.CONFIRMED.name,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ))
                        val patientId = latest.getString("patientId").orEmpty()
                        if (patientId.isNotBlank()) transaction.set(notification, mapOf(
                            "id" to notification.id,
                            "userId" to patientId,
                            "title" to "Lịch khám đã được xác nhận",
                            "body" to "Lịch khám $date · $time đã được Admin xác nhận.",
                            "type" to "APPOINTMENT_CONFIRMED",
                            "appointmentId" to appointmentId,
                            "isRead" to false,
                            "createdAt" to FieldValue.serverTimestamp(),
                        ))
                        Unit
                    }.addOnSuccessListener { callback(Result.success(Unit)) }
                        .addOnFailureListener { callback(Result.failure(it)) }
                }.addOnFailureListener { callback(Result.failure(it)) }
        }.addOnFailureListener { callback(Result.failure(it)) }
    }

    /**
     * Rescheduling preserves the old appointment as cancelled history and creates a distinct
     * replacement document, so an older cancelled request is never overwritten.
     */
    fun rescheduleAppointment(
        appointmentId: String,
        newStartTime: String,
        newEndTime: String,
        note: String,
        callback: (Result<String>) -> Unit,
    ) {
        val currentRef = firestore.collection(APPOINTMENTS_COLLECTION).document(appointmentId)
        val newRef = firestore.collection(APPOINTMENTS_COLLECTION).document()
        currentRef.get().addOnSuccessListener { initial ->
            if (!initial.exists()) return@addOnSuccessListener callback(Result.failure(NoSuchElementException("Không tìm thấy lịch hẹn.")))
            val doctorId = initial.getString("doctorId").orEmpty()
            val date = initial.getString("appointmentDate").orEmpty()
            if (doctorId.isBlank() || date.isBlank())
                return@addOnSuccessListener callback(Result.failure(IllegalStateException("Lịch hẹn thiếu bác sĩ hoặc ngày khám.")))
            firestore.collection(APPOINTMENTS_COLLECTION).whereEqualTo("doctorId", doctorId).get()
                .addOnSuccessListener { snapshot ->
                    val competingRefs = snapshot.documents.filter { it.id != appointmentId &&
                        it.getString("appointmentDate") == date && it.getString("startTime") == newStartTime
                    }.map { it.reference }
        firestore.runTransaction { transaction ->
            val current = transaction.get(currentRef)
            if (!current.exists()) throw NoSuchElementException("Không tìm thấy lịch hẹn.")
            if (current.getString("status") != AppointmentStatus.PENDING.name)
                throw IllegalStateException("Chỉ đề xuất giờ khác cho lịch đang chờ duyệt.")
            if (current.getString("doctorId") != doctorId || current.getString("appointmentDate") != date)
                throw IllegalStateException("Lịch hẹn đã thay đổi. Hãy tải lại danh sách.")
            val scheduleId = current.getString("scheduleId").orEmpty()
            if (scheduleId.isBlank()) throw IllegalStateException("Lịch hẹn thiếu thông tin ca làm việc.")
            val schedule = transaction.get(firestore.collection("doctorSchedules").document(scheduleId))
            if (!schedule.exists() || schedule.getBoolean("isLocked") == true)
                throw IllegalStateException("Ca làm việc không còn nhận lịch.")
            if (newStartTime in ((schedule.get("lockedSlots") as? List<*>)?.filterIsInstance<String>().orEmpty()))
                throw IllegalStateException("Khung giờ này đã bị khóa.")
            if (schedule.getString("doctorId") != current.getString("doctorId") ||
                schedule.getString("workDate") != current.getString("appointmentDate"))
                throw IllegalStateException("Lịch hẹn không khớp với ca làm việc.")
            val proposedStart = LocalTime.parse(newStartTime)
            val proposedEnd = LocalTime.parse(newEndTime)
            val shiftTimes = ShiftTimeNormalizer.normalize(schedule.getString("shiftName").orEmpty(),
                schedule.getString("startTime").orEmpty(), schedule.getString("endTime").orEmpty())
            if (proposedStart < LocalTime.parse(shiftTimes.first) ||
                proposedEnd > LocalTime.parse(shiftTimes.second) ||
                !proposedEnd.isAfter(proposedStart))
                throw IllegalStateException("Giờ mới nằm ngoài ca làm việc.")
            if (competingRefs.map(transaction::get).any { it.getString("status") !in
                    listOf(AppointmentStatus.CANCELLED.name, AppointmentStatus.REJECTED.name) })
                throw IllegalStateException("Khung giờ mới đã có người đặt.")
            if (newStartTime == current.getString("startTime"))
                throw IllegalArgumentException("Vui lòng chọn giờ khác giờ hiện tại.")
            val oldSlotRef = slotReference(scheduleId, current.getString("startTime").orEmpty())
            val newSlotRef = requireNotNull(slotReference(scheduleId, newStartTime))
            val oldSlot = oldSlotRef?.let(transaction::get)
            val newSlot = transaction.get(newSlotRef)
            if (newSlot.exists() && newSlot.getString("state") != BookingSlotRepository.FREE)
                throw IllegalStateException("Khung giờ mới đã có người đặt.")
            val newId = newRef.id

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
            if (oldSlot?.getString("appointmentId") == appointmentId && oldSlotRef != null)
                transaction.update(oldSlotRef, mapOf("state" to BookingSlotRepository.FREE,
                    "updatedAt" to FieldValue.serverTimestamp()))
            transaction.set(newSlotRef, mapOf(
                "id" to newSlotRef.id,
                "doctorId" to doctorId,
                "scheduleId" to scheduleId,
                "startTime" to newStartTime,
                "appointmentId" to newId,
                "state" to BookingSlotRepository.RESERVED,
                "updatedAt" to FieldValue.serverTimestamp(),
            ))
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
                }.addOnFailureListener { callback(Result.failure(it)) }
        }.addOnFailureListener { callback(Result.failure(it)) }
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
        val appointment = firestore.collection(APPOINTMENTS_COLLECTION).document(appointmentId)
        firestore.runTransaction { transaction ->
            val current = transaction.get(appointment)
            if (!current.exists()) throw NoSuchElementException("Không tìm thấy lịch hẹn.")
            if (current.getString("status") !in listOf(AppointmentStatus.PENDING.name, AppointmentStatus.CONFIRMED.name))
                throw IllegalStateException("Lịch hẹn không còn có thể hủy.")
            val slotRef = slotReference(current.getString("scheduleId").orEmpty(), current.getString("startTime").orEmpty())
            val slot = slotRef?.let(transaction::get)
            transaction.update(appointment, mapOf("status" to AppointmentStatus.CANCELLED.name,
                "cancellationReason" to reason, "updatedAt" to FieldValue.serverTimestamp()))
            if (slot?.getString("appointmentId") == appointmentId && slotRef != null)
                transaction.update(slotRef, mapOf("state" to BookingSlotRepository.FREE,
                    "updatedAt" to FieldValue.serverTimestamp()))
            Unit
        }
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    private fun slotReference(scheduleId: String, startTime: String) =
        if (scheduleId.isBlank() || startTime.isBlank()) null else
            firestore.collection(BookingSlotRepository.COLLECTION)
                .document(BookingSlotRepository.id(scheduleId, startTime))

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
        val document = firestore.collection(APPOINTMENTS_COLLECTION).document()
        val scheduleRef = firestore.collection("doctorSchedules").document(schedule.id)
        val slotRef = firestore.collection(BookingSlotRepository.COLLECTION)
            .document(BookingSlotRepository.id(schedule.id, startTime))
        firestore.collection(USERS_COLLECTION).document(user.uid).get()
            .addOnSuccessListener { profile ->
                firestore.runTransaction { transaction ->
                    val currentSchedule = transaction.get(scheduleRef)
                    val slot = transaction.get(slotRef)
                    if (!currentSchedule.exists() || currentSchedule.getBoolean("isLocked") == true ||
                        currentSchedule.getString("doctorId") != schedule.doctorId ||
                        currentSchedule.getString("workDate") != schedule.workDate ||
                        startTime in ((currentSchedule.get("lockedSlots") as? List<*>)?.filterIsInstance<String>().orEmpty()))
                        throw IllegalStateException("Khung giờ này đã bị khóa. Vui lòng chọn giờ khác.")
                    val shiftStart = LocalTime.parse(currentSchedule.getString("startTime"))
                    val shiftEnd = LocalTime.parse(currentSchedule.getString("endTime"))
                    val requestedStart = LocalTime.parse(startTime)
                    val requestedEnd = LocalTime.parse(endTime)
                    val duration = (currentSchedule.getLong("appointmentDurationMinutes") ?: 30L).coerceAtLeast(1L)
                    if (requestedStart.isBefore(shiftStart) || requestedEnd.isAfter(shiftEnd) ||
                        requestedStart.plusMinutes(duration) != requestedEnd ||
                        java.time.Duration.between(shiftStart, requestedStart).toMinutes() % duration != 0L)
                        throw IllegalArgumentException("Khung giờ không còn hợp lệ. Vui lòng tải lại lịch.")
                    if (slot.exists() && slot.getString("state") != BookingSlotRepository.FREE)
                        throw IllegalStateException("Giờ này đã có người đặt. Vui lòng chọn giờ khác.")
                    transaction.set(document, hashMapOf(
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
                    ))
                    transaction.set(slotRef, mapOf(
                        "id" to slotRef.id,
                        "doctorId" to schedule.doctorId,
                        "scheduleId" to schedule.id,
                        "startTime" to startTime,
                        "appointmentId" to document.id,
                        "state" to BookingSlotRepository.RESERVED,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ))
                    Unit
                }.addOnSuccessListener { callback(Result.success(Unit)) }
                    .addOnFailureListener { error ->
                        val message = if (error is com.google.firebase.firestore.FirebaseFirestoreException &&
                            error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED)
                            "Giờ này đã có người đặt hoặc lịch đã thay đổi. Vui lòng chọn giờ khác."
                        else error.message ?: "Không gửi được yêu cầu đặt lịch."
                        callback(Result.failure(IllegalStateException(message, error)))
                    }
            }.addOnFailureListener { callback(Result.failure(it)) }
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
