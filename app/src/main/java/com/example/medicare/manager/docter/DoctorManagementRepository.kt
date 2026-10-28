package com.example.medicare.manager.docter

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

data class ManagedDoctor(
    val id: String,
    val fullName: String,
    val specialtyId: String,
    val specialty: String,
    val email: String,
    val phone: String,
    val experienceYears: String,
    val qualification: String,
    val biography: String,
    val isActive: Boolean,
)

data class WorkShift(
    val id: String,
    val name: String,
    val startTime: String,
    val endTime: String,
    val description: String,
    val isActive: Boolean,
)

data class ManagedSpecialty(
    val id: String,
    val name: String,
    val description: String,
)

/** Firebase-backed manager data. A secondary Auth instance preserves the admin session. */
class DoctorManagementRepository(
    private val context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    fun getDoctors(callback: (Result<List<ManagedDoctor>>) -> Unit) {
        firestore.collection(DOCTORS_COLLECTION).orderBy("fullName").get()
            .addOnSuccessListener { snapshot ->
                callback(Result.success(snapshot.documents.map { document ->
                    ManagedDoctor(
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

    fun getPatientCount(callback: (Result<Int>) -> Unit) {
        firestore.collection(USERS_COLLECTION)
            .whereEqualTo("type", "USER")
            .get()
            .addOnSuccessListener { callback(Result.success(it.size())) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun createDoctorAccount(
        fullName: String,
        specialty: ManagedSpecialty,
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

    fun getSpecialties(callback: (Result<List<ManagedSpecialty>>) -> Unit) {
        firestore.collection(SPECIALTIES_COLLECTION).orderBy("name").get()
            .addOnSuccessListener { snapshot ->
                callback(Result.success(snapshot.documents.map { document ->
                    ManagedSpecialty(
                        id = document.id,
                        name = document.getString("name").orEmpty(),
                        description = document.getString("description").orEmpty(),
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
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        )
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
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
