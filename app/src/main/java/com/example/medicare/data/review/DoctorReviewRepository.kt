package com.example.medicare.data.review

import com.example.medicare.model.DoctorReview
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class DoctorReviewRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance(), private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    fun save(appointmentId: String, doctorId: String, doctorName: String, rating: Int, comment: String, callback: (Result<Unit>) -> Unit) {
        val patientId = auth.currentUser?.uid ?: return callback(Result.failure(IllegalStateException("Vui lòng đăng nhập lại.")))
        val doc = firestore.collection(COLLECTION).document("${appointmentId}_$patientId")
        doc.set(mapOf("id" to doc.id, "appointmentId" to appointmentId, "patientId" to patientId, "doctorId" to doctorId, "doctorName" to doctorName, "rating" to rating, "comment" to comment, "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener { callback(Result.success(Unit)) }.addOnFailureListener { callback(Result.failure(it)) }
    }
    fun getMine(callback: (Result<List<DoctorReview>>) -> Unit) {
        val patientId = auth.currentUser?.uid ?: return callback(Result.failure(IllegalStateException("Vui lòng đăng nhập lại.")))
        firestore.collection(COLLECTION).whereEqualTo("patientId", patientId).get().addOnSuccessListener { snapshot ->
            callback(Result.success(snapshot.documents.map { d -> DoctorReview(d.id, d.getString("appointmentId").orEmpty(), d.getString("doctorId").orEmpty(), d.getString("doctorName").orEmpty(), d.getLong("rating")?.toInt() ?: 0, d.getString("comment").orEmpty(), d.getTimestamp("createdAt")?.toDate()?.time ?: 0L) }))
        }.addOnFailureListener { callback(Result.failure(it)) }
    }
    fun getAll(callback: (Result<List<DoctorReview>>) -> Unit) {
        firestore.collection(COLLECTION).get().addOnSuccessListener { snapshot ->
            callback(Result.success(snapshot.documents.map { d ->
                DoctorReview(d.id, d.getString("appointmentId").orEmpty(), d.getString("doctorId").orEmpty(), d.getString("doctorName").orEmpty(), d.getLong("rating")?.toInt() ?: 0, d.getString("comment").orEmpty(), d.getTimestamp("createdAt")?.toDate()?.time ?: 0L)
            }.sortedByDescending { it.createdAt }))
        }.addOnFailureListener { callback(Result.failure(it)) }
    }
    private companion object { const val COLLECTION = "doctorReviews" }
}
