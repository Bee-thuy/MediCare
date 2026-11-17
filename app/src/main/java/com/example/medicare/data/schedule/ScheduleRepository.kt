package com.example.medicare.data.schedule

import com.example.medicare.model.Doctor
import com.example.medicare.model.DoctorSchedule
import com.example.medicare.model.WorkShift
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
                "startTime" to shift.startTime,
                "endTime" to shift.endTime,
                "room" to room,
                "appointmentDurationMinutes" to appointmentDurationMinutes,
                "maximumAppointments" to maximumAppointments,
                "isLocked" to false,
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

    fun updateSchedule(
        scheduleId: String,
        room: String,
        duration: Int,
        capacity: Int,
        note: String,
        callback: (Result<Unit>) -> Unit,
    ) {
        firestore.collection(SCHEDULES_COLLECTION).document(scheduleId).update(mapOf(
            "room" to room.trim(),
            "appointmentDurationMinutes" to duration,
            "maximumAppointments" to capacity,
            "note" to note.trim(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )).addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
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

    private fun toSchedule(document: com.google.firebase.firestore.DocumentSnapshot): DoctorSchedule = DoctorSchedule(
        id = document.id,
        doctorId = document.getString("doctorId").orEmpty(),
        doctorName = document.getString("doctorName").orEmpty(),
        specialtyName = document.getString("specialtyName").orEmpty(),
        workDate = document.getString("workDate").orEmpty(),
        shiftId = document.getString("shiftId").orEmpty(),
        shiftName = document.getString("shiftName").orEmpty(),
        startTime = document.getString("startTime").orEmpty(),
        endTime = document.getString("endTime").orEmpty(),
        room = document.getString("room").orEmpty(),
        appointmentDurationMinutes = document.getLong("appointmentDurationMinutes")?.toInt() ?: 30,
        maximumAppointments = document.getLong("maximumAppointments")?.toInt() ?: 0,
        isLocked = document.getBoolean("isLocked") ?: false,
        note = document.getString("note").orEmpty(),
    )

    private companion object {
        const val SCHEDULES_COLLECTION = "doctorSchedules"
    }
}
