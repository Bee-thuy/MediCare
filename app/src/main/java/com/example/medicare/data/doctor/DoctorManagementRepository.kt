package com.example.medicare.data.doctor

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.example.medicare.model.Doctor
import com.example.medicare.model.Specialty
import com.example.medicare.model.WorkShift

/** Firebase-backed manager data. A secondary Auth instance preserves the admin session. */
class DoctorManagementRepository(
    private val context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    fun getDoctors(callback: (Result<List<Doctor>>) -> Unit) {
        firestore.collection(DOCTORS_COLLECTION).orderBy("fullName").get()
            .addOnSuccessListener { snapshot ->
                callback(Result.success(snapshot.documents.map { document ->
                    Doctor(
                        id = document.id,
                        fullName = document.getString("fullName").orEmpty(),
                        specialtyId = document.getString("specialtyId").orEmpty(),
                        specialty = document.getString("specialtyName").orEmpty(),
                        email = document.getString("email").orEmpty(),
                        phone = document.getString("phone").orEmpty(),
                        experienceYears = document.getString("experienceYears").orEmpty(),
                        qualification = document.getString("qualification").orEmpty(),
                        biography = document.getString("biography").orEmpty(),
                        isActive = document.getBoolean("isActive") ?: true,
                    )
                }))
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun getActiveDoctors(callback: (Result<List<Doctor>>) -> Unit) {
        firestore.collection(DOCTORS_COLLECTION)
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { snapshot ->
                callback(Result.success(snapshot.documents.map { document ->
                    Doctor(
                        id = document.id,
                        fullName = document.getString("fullName").orEmpty(),
                        specialtyId = document.getString("specialtyId").orEmpty(),
                        specialty = document.getString("specialtyName").orEmpty(),
                        email = document.getString("email").orEmpty(),
                        phone = document.getString("phone").orEmpty(),
                        experienceYears = document.getString("experienceYears").orEmpty(),
                        qualification = document.getString("qualification").orEmpty(),
                        biography = document.getString("biography").orEmpty(),
                        isActive = true,
                    )
                }.sortedBy { it.fullName }))
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun getPatientCount(callback: (Result<Int>) -> Unit) {
        firestore.collection(USERS_COLLECTION)
            .whereEqualTo("type", "USER")
            .get()
            .addOnSuccessListener { callback(Result.success(it.size())) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun setDoctorActive(doctorId: String, active: Boolean, callback: (Result<Unit>) -> Unit) {
        firestore.batch()
            .update(firestore.collection(DOCTORS_COLLECTION).document(doctorId), "isActive", active, "updatedAt", FieldValue.serverTimestamp())
            .update(firestore.collection(USERS_COLLECTION).document(doctorId), "accountStatus", if (active) "ACTIVE" else "DISABLED", "updatedAt", FieldValue.serverTimestamp())
            .commit()
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun updateDoctor(
        doctorId: String,
        fullName: String,
        specialty: Specialty,
        phone: String,
        experienceYears: String,
        callback: (Result<Unit>) -> Unit,
    ) {
        firestore.batch()
            .update(firestore.collection(DOCTORS_COLLECTION).document(doctorId), mapOf(
                "fullName" to fullName,
                "specialtyId" to specialty.id,
                "specialtyName" to specialty.name,
                "phone" to phone,
                "experienceYears" to experienceYears,
                "updatedAt" to FieldValue.serverTimestamp(),
            ))
            .update(firestore.collection(USERS_COLLECTION).document(doctorId), mapOf(
                "fullName" to fullName,
                "phone" to phone,
                "updatedAt" to FieldValue.serverTimestamp(),
            ))
            .commit()
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun createDoctorAccount(
        fullName: String,
        specialty: Specialty,
        email: String,
        password: String,
        phone: String,
        experienceYears: String,
        qualification: String,
        biography: String,
        isActive: Boolean,
        callback: (Result<Unit>) -> Unit,
    ) {
        if (auth.currentUser == null) {
            callback(Result.failure(IllegalStateException("Hãy đăng nhập bằng tài khoản ADMIN Firebase để tạo bác sĩ.")))
            return
        }
        val secondaryAuth = runCatching { secondaryAuth() }.getOrElse {
            callback(Result.failure(it))
            return
        }
        secondaryAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: run {
                    callback(Result.failure(IllegalStateException("Không thể tạo tài khoản bác sĩ.")))
                    return@addOnSuccessListener
                }
                val profile = hashMapOf(
                    "id" to uid,
                    "fullName" to fullName,
                    "email" to email,
                    "phone" to phone,
                    "type" to "DOCTOR",
                    "accountStatus" to if (isActive) "ACTIVE" else "DISABLED",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                val doctor = hashMapOf(
                    "id" to uid,
                    "fullName" to fullName,
                    "email" to email,
                    "phone" to phone,
                    "specialtyId" to specialty.id,
                    "specialtyName" to specialty.name,
                    "experienceYears" to experienceYears,
                    "qualification" to qualification,
                    "biography" to biography,
                    "isActive" to isActive,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                firestore.batch()
                    .set(firestore.collection(USERS_COLLECTION).document(uid), profile)
                    .set(firestore.collection(DOCTORS_COLLECTION).document(uid), doctor)
                    .commit()
                    .addOnSuccessListener {
                        secondaryAuth.signOut()
                        callback(Result.success(Unit))
                    }
                    .addOnFailureListener { error ->
                        authResult.user?.delete()
                        secondaryAuth.signOut()
                        callback(Result.failure(error))
                    }
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun getSpecialties(callback: (Result<List<Specialty>>) -> Unit) {
        firestore.collection(SPECIALTIES_COLLECTION).orderBy("name").get()
            .addOnSuccessListener { snapshot ->
                callback(Result.success(snapshot.documents.map { document ->
                    Specialty(
                        id = document.id,
                        name = document.getString("name").orEmpty(),
                        description = document.getString("description").orEmpty(),
                        isActive = document.getBoolean("isActive") ?: true,
                    )
                }))
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun addSpecialty(name: String, description: String, callback: (Result<Unit>) -> Unit) {
        val document = firestore.collection(SPECIALTIES_COLLECTION).document()
        document.set(
            hashMapOf(
                "id" to document.id,
                "name" to name,
                "description" to description,
                "isActive" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        )
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun updateSpecialty(specialty: Specialty, name: String, description: String, callback: (Result<Unit>) -> Unit) {
        firestore.collection(SPECIALTIES_COLLECTION).document(specialty.id).update(mapOf(
            "name" to name.trim(), "description" to description.trim(), "updatedAt" to FieldValue.serverTimestamp(),
        )).addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun setSpecialtyActive(specialtyId: String, active: Boolean, callback: (Result<Unit>) -> Unit) {
        firestore.collection(SPECIALTIES_COLLECTION).document(specialtyId).update(mapOf(
            "isActive" to active, "updatedAt" to FieldValue.serverTimestamp(),
        )).addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun deleteSpecialtyIfUnused(specialty: Specialty, callback: (Result<Unit>) -> Unit) {
        firestore.collection(DOCTORS_COLLECTION).whereEqualTo("specialtyId", specialty.id).get()
            .addOnSuccessListener { doctors ->
                if (!doctors.isEmpty) return@addOnSuccessListener callback(Result.failure(IllegalStateException("Không thể xóa vì chuyên khoa đang có bác sĩ.")))
                firestore.collection("doctorSchedules").whereEqualTo("specialtyName", specialty.name).limit(1).get()
                    .addOnSuccessListener { schedules ->
                        if (!schedules.isEmpty) callback(Result.failure(IllegalStateException("Không thể xóa vì chuyên khoa đã có lịch làm việc.")))
                        else firestore.collection(SPECIALTIES_COLLECTION).document(specialty.id).delete()
                            .addOnSuccessListener { callback(Result.success(Unit)) }
                            .addOnFailureListener { callback(Result.failure(it)) }
                    }.addOnFailureListener { callback(Result.failure(it)) }
            }.addOnFailureListener { callback(Result.failure(it)) }
    }

    fun getWorkShifts(callback: (Result<List<WorkShift>>) -> Unit) {
        firestore.collection(WORK_SHIFTS_COLLECTION).orderBy("startTime").get()
            .addOnSuccessListener { snapshot ->
                callback(Result.success(snapshot.documents.map { document ->
                    WorkShift(
                        id = document.id,
                        name = document.getString("name").orEmpty(),
                        startTime = document.getString("startTime").orEmpty(),
                        endTime = document.getString("endTime").orEmpty(),
                        description = document.getString("description").orEmpty(),
                        isActive = document.getBoolean("isActive") ?: true,
                    )
                }))
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun addWorkShift(
        name: String,
        startTime: String,
        endTime: String,
        description: String,
        isActive: Boolean,
        callback: (Result<Unit>) -> Unit,
    ) {
        val document = firestore.collection(WORK_SHIFTS_COLLECTION).document()
        document.set(
            hashMapOf(
                "id" to document.id,
                "name" to name,
                "startTime" to startTime,
                "endTime" to endTime,
                "description" to description,
                "isActive" to isActive,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        )
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun updateWorkShift(shift: WorkShift, name: String, startTime: String, endTime: String, description: String, callback: (Result<Unit>) -> Unit) {
        firestore.collection(WORK_SHIFTS_COLLECTION).document(shift.id).update(mapOf(
            "name" to name.trim(), "startTime" to startTime.trim(), "endTime" to endTime.trim(),
            "description" to description.trim(), "updatedAt" to FieldValue.serverTimestamp(),
        )).addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun setWorkShiftActive(shiftId: String, active: Boolean, callback: (Result<Unit>) -> Unit) {
        firestore.collection(WORK_SHIFTS_COLLECTION).document(shiftId).update(mapOf(
            "isActive" to active, "updatedAt" to FieldValue.serverTimestamp(),
        )).addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun deleteWorkShiftIfUnused(shift: WorkShift, callback: (Result<Unit>) -> Unit) {
        firestore.collection("doctorSchedules").whereEqualTo("shiftId", shift.id).limit(1).get()
            .addOnSuccessListener { schedules ->
                if (!schedules.isEmpty) callback(Result.failure(IllegalStateException("Không thể xóa vì ca làm đã được phân lịch.")))
                else firestore.collection(WORK_SHIFTS_COLLECTION).document(shift.id).delete()
                    .addOnSuccessListener { callback(Result.success(Unit)) }
                    .addOnFailureListener { callback(Result.failure(it)) }
            }.addOnFailureListener { callback(Result.failure(it)) }
    }

    private fun secondaryAuth(): FirebaseAuth {
        val app = FirebaseApp.getApps(context).firstOrNull { it.name == CREATION_APP_NAME }
            ?: FirebaseApp.initializeApp(
                context,
                FirebaseOptions.fromResource(context) ?: error("Không tìm thấy cấu hình Firebase."),
                CREATION_APP_NAME,
            )
            ?: error("Không thể khởi tạo Firebase phụ.")
        return FirebaseAuth.getInstance(app)
    }

    private companion object {
        const val CREATION_APP_NAME = "doctor-account-creation"
        const val USERS_COLLECTION = "users"
        const val DOCTORS_COLLECTION = "doctors"
        const val SPECIALTIES_COLLECTION = "specialties"
        const val WORK_SHIFTS_COLLECTION = "workShifts"
    }
}
