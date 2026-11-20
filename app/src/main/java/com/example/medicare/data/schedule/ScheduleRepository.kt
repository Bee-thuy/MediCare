package com.example.medicare.data.schedule

import com.example.medicare.model.Doctor
import com.example.medicare.model.DoctorSchedule
import com.example.medicare.model.WorkShift
import com.example.medicare.model.ShiftTimeNormalizer
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.example.medicare.model.AppointmentStatus
import java.time.LocalDate
import java.time.LocalTime

class ScheduleRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun getAllSchedules(callback: (Result<List<DoctorSchedule>>) -> Unit) {
        firestore.collection(SCHEDULES_COLLECTION).get()
            .addOnSuccessListener { snapshot -> callback(Result.success(snapshot.documents.map(::toSchedule))) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }
    fun getSchedulesForDate(workDate: String, callback: (Result<List<DoctorSchedule>>) -> Unit) {
        firestore.collection(SCHEDULES_COLLECTION)
            .whereEqualTo("workDate", workDate)
            .get()
            .addOnSuccessListener { snapshot ->
                callback(Result.success(snapshot.documents.map(::toSchedule).sortedBy { it.startTime }))
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun getSchedule(scheduleId: String, callback: (Result<DoctorSchedule>) -> Unit) {
        firestore.collection(SCHEDULES_COLLECTION).document(scheduleId).get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    callback(Result.failure(NoSuchElementException("Không tìm thấy lịch làm việc.")))
                } else {
                    callback(Result.success(toSchedule(document)))
                }
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun getOpenSchedulesForDoctor(doctorId: String, callback: (Result<List<DoctorSchedule>>) -> Unit) {
        firestore.collection(SCHEDULES_COLLECTION)
            .whereEqualTo("doctorId", doctorId)
            .whereEqualTo("isLocked", false)
            .get()
            .addOnSuccessListener { snapshot ->
                callback(Result.success(snapshot.documents.map(::toSchedule)
                    .filter(::isBookableNow)
                    .sortedWith(compareBy({ it.workDate }, { it.startTime }))))
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun isBookableNow(schedule: DoctorSchedule): Boolean = runCatching {
        val date = LocalDate.parse(schedule.workDate)
        val today = LocalDate.now()
        date.isAfter(today) || (date == today && LocalTime.parse(schedule.endTime).isAfter(LocalTime.now()))
    }.getOrDefault(false)

    /** Returns every assigned shift, including a temporarily locked shift, for its owner. */
    fun getSchedulesForDoctor(doctorId: String, callback: (Result<List<DoctorSchedule>>) -> Unit) {
        firestore.collection(SCHEDULES_COLLECTION)
            .whereEqualTo("doctorId", doctorId)
            .get()
            .addOnSuccessListener { snapshot ->
                callback(Result.success(snapshot.documents.map(::toSchedule).sortedWith(compareBy({ it.workDate }, { it.startTime }))))
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun createSchedule(
        doctor: Doctor,
        shift: WorkShift,
        workDate: String,
        room: String,
        appointmentDurationMinutes: Int,
        maximumAppointments: Int,
        note: String,
        callback: (Result<Unit>) -> Unit,
    ) {
        val document = firestore.collection(SCHEDULES_COLLECTION).document()
        document.set(
            hashMapOf(
                "id" to document.id,
                "doctorId" to doctor.id,
                "doctorName" to doctor.fullName,
                "specialtyName" to doctor.specialty,
                "workDate" to workDate,
                "shiftId" to shift.id,
                "shiftName" to shift.name,
                "startTime" to ShiftTimeNormalizer.normalize(shift.name, shift.startTime, shift.endTime).first,
                "endTime" to ShiftTimeNormalizer.normalize(shift.name, shift.startTime, shift.endTime).second,
                "room" to room,
                "appointmentDurationMinutes" to appointmentDurationMinutes,
                "maximumAppointments" to maximumAppointments,
                "isLocked" to false,
                "lockedSlots" to emptyList<String>(),
                "note" to note,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        )
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun setScheduleLocked(scheduleId: String, isLocked: Boolean, callback: (Result<Unit>) -> Unit) {
        firestore.collection(SCHEDULES_COLLECTION).document(scheduleId)
            .update(mapOf("isLocked" to isLocked, "updatedAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    /** Locks one appointment slot without closing the doctor's entire shift. */
    fun setSlotLocked(scheduleId: String, startTime: String, locked: Boolean, callback: (Result<Unit>) -> Unit) {
        val scheduleRef = firestore.collection(SCHEDULES_COLLECTION).document(scheduleId)
        firestore.collection("appointments").whereEqualTo("scheduleId", scheduleId).get()
            .addOnSuccessListener { appointments ->
                val active = appointments.documents.filter { it.getString("startTime") == startTime &&
                    it.getString("status") !in listOf(AppointmentStatus.CANCELLED.name, AppointmentStatus.REJECTED.name) }
                val refs = active.map { it.reference }
                firestore.runTransaction { transaction ->
                    val schedule = transaction.get(scheduleRef)
                    if (!schedule.exists()) throw NoSuchElementException("Không tìm thấy ca làm việc.")
                    val normalized = ShiftTimeNormalizer.normalize(schedule.getString("shiftName").orEmpty(),
                        schedule.getString("startTime").orEmpty(), schedule.getString("endTime").orEmpty())
                    val first = LocalTime.parse(normalized.first)
                    val end = LocalTime.parse(normalized.second)
                    val requested = LocalTime.parse(startTime)
                    val duration = (schedule.getLong("appointmentDurationMinutes") ?: 30L).coerceAtLeast(1L)
                    if (requested.isBefore(first) || requested.plusMinutes(duration).isAfter(end) ||
                        java.time.Duration.between(first, requested).toMinutes() % duration != 0L)
                        throw IllegalArgumentException("Khung giờ không thuộc ca làm việc.")
                    if (locked && refs.map(transaction::get).any { it.getString("status") !in
                            listOf(AppointmentStatus.CANCELLED.name, AppointmentStatus.REJECTED.name) })
                        throw IllegalStateException("Khung giờ đã có lịch hẹn. Hãy xử lý lịch trước khi khóa.")
                    val current = (schedule.get("lockedSlots") as? List<*>)?.filterIsInstance<String>().orEmpty()
                    transaction.update(scheduleRef, mapOf(
                        "lockedSlots" to (if (locked) (current + startTime).distinct() else current - startTime),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ))
                    Unit
                }.addOnSuccessListener { callback(Result.success(Unit)) }
                    .addOnFailureListener { callback(Result.failure(it)) }
            }.addOnFailureListener { callback(Result.failure(it)) }
    }

    fun updateSchedule(
        scheduleId: String,
        room: String,
        duration: Int,
        capacity: Int,
        note: String,
        callback: (Result<Unit>) -> Unit,
    ) {
        val reference = firestore.collection(SCHEDULES_COLLECTION).document(scheduleId)
        reference.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                callback(Result.failure(NoSuchElementException("Không tìm thấy ca làm việc.")))
                return@addOnSuccessListener
            }
            val times = ShiftTimeNormalizer.normalize(document.getString("shiftName").orEmpty(),
                document.getString("startTime").orEmpty(), document.getString("endTime").orEmpty())
            val changes = mutableMapOf<String, Any>(
                "room" to room.trim(),
                "appointmentDurationMinutes" to duration,
                "maximumAppointments" to capacity,
                "note" to note.trim(),
                "startTime" to times.first,
                "endTime" to times.second,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (!document.contains("isLocked")) changes["isLocked"] = false
            reference.update(changes)
                .addOnSuccessListener { callback(Result.success(Unit)) }
                .addOnFailureListener { callback(Result.failure(it)) }
        }.addOnFailureListener { callback(Result.failure(it)) }
    }

    fun deleteScheduleIfNoActiveAppointments(scheduleId: String, callback: (Result<Unit>) -> Unit) {
        firestore.collection("appointments").whereEqualTo("scheduleId", scheduleId).get()
            .addOnSuccessListener { appointments ->
                val hasActive = appointments.documents.any { it.getString("status") !in listOf("CANCELLED", "REJECTED", "COMPLETED") }
                if (hasActive) callback(Result.failure(IllegalStateException("Không thể xóa vì ca này vẫn có lịch hẹn đang chờ hoặc đã xác nhận.")))
                else firestore.collection(SCHEDULES_COLLECTION).document(scheduleId).delete()
                    .addOnSuccessListener { callback(Result.success(Unit)) }
                    .addOnFailureListener { callback(Result.failure(it)) }
            }.addOnFailureListener { callback(Result.failure(it)) }
    }

    private fun toSchedule(document: com.google.firebase.firestore.DocumentSnapshot): DoctorSchedule {
        val times = ShiftTimeNormalizer.normalize(document.getString("shiftName").orEmpty(),
            document.getString("startTime").orEmpty(), document.getString("endTime").orEmpty())
        return DoctorSchedule(
        id = document.id,
        doctorId = document.getString("doctorId").orEmpty(),
        doctorName = document.getString("doctorName").orEmpty(),
        specialtyName = document.getString("specialtyName").orEmpty(),
        workDate = document.getString("workDate").orEmpty(),
        shiftId = document.getString("shiftId").orEmpty(),
        shiftName = document.getString("shiftName").orEmpty(),
        startTime = times.first,
        endTime = times.second,
        room = document.getString("room").orEmpty(),
        appointmentDurationMinutes = document.getLong("appointmentDurationMinutes")?.toInt() ?: 30,
        maximumAppointments = document.getLong("maximumAppointments")?.toInt() ?: 0,
        isLocked = document.getBoolean("isLocked") ?: false,
        note = document.getString("note").orEmpty(),
        lockedSlots = (document.get("lockedSlots") as? List<*>)?.filterIsInstance<String>().orEmpty(),
    )
    }

    private companion object {
        const val SCHEDULES_COLLECTION = "doctorSchedules"
    }
}
