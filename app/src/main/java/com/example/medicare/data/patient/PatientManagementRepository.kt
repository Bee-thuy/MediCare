package com.example.medicare.data.patient

import com.example.medicare.model.AccountStatus
import com.example.medicare.model.Patient
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class PatientManagementRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun getPatients(callback: (Result<List<Patient>>) -> Unit) {
        firestore.collection(USERS_COLLECTION)
            .whereEqualTo("type", "USER")
            .get()
            .addOnSuccessListener { snapshot ->
                callback(
                    Result.success(
                        snapshot.documents.map { document ->
                            Patient(
                                id = document.id,
                                fullName = document.getString("fullName").orEmpty(),
                                email = document.getString("email").orEmpty(),
                                phone = document.getString("phone").orEmpty(),
                                accountStatus = AccountStatus.fromStoredValue(document.getString("accountStatus")),
                            )
                        }.sortedBy { it.fullName },
                    ),
                )
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun updateAccountStatus(
        patientId: String,
        status: AccountStatus,
        callback: (Result<Unit>) -> Unit,
    ) {
        firestore.collection(USERS_COLLECTION).document(patientId)
            .update(
                mapOf(
                    "accountStatus" to status.name,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    private companion object {
        const val USERS_COLLECTION = "users"
    }
}
