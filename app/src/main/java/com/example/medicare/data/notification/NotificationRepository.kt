package com.example.medicare.data.notification

import com.example.medicare.model.AppNotification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class NotificationRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    /** Stores an in-app notification. FCM, when configured, is only an extra delivery channel. */
    fun create(
        userId: String,
        title: String,
        body: String,
        type: String,
        appointmentId: String = "",
        callback: (Result<Unit>) -> Unit = {},
    ) {
        if (userId.isBlank()) {
            callback(Result.failure(IllegalArgumentException("Không tìm thấy người nhận thông báo.")))
            return
        }
        val document = firestore.collection(COLLECTION).document()
        document.set(
            mapOf(
                "id" to document.id,
                "userId" to userId,
                "title" to title.trim(),
                "body" to body.trim(),
                "type" to type,
                "appointmentId" to appointmentId,
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    /** Sends one persisted notification to every patient account in batches below Firestore's limit. */
    fun createForAllPatients(
        title: String,
        body: String,
        callback: (Result<Int>) -> Unit,
    ) = createForAudience(title, body, "USER", callback)

    /** Audience is ALL, DOCTOR, or USER; each recipient receives its own in-app record. */
    fun createForAudience(title: String, body: String, audience: String, callback: (Result<Int>) -> Unit) {
        if (audience !in setOf("ALL", "DOCTOR", "USER")) {
            callback(Result.failure(IllegalArgumentException("Đối tượng nhận không hợp lệ.")))
            return
        }
        firestore.collection(USERS_COLLECTION).get()
            .addOnSuccessListener { users ->
                val ids = users.documents.filter { document ->
                    val type = document.getString("type")
                    type in setOf("USER", "DOCTOR") && (audience == "ALL" || type == audience)
                }.map { it.id }
                if (ids.isEmpty()) return@addOnSuccessListener callback(Result.success(0))
                val batches = ids.chunked(400)
                fun commitBatch(index: Int) {
                    if (index == batches.size) return callback(Result.success(ids.size))
                    val batch = firestore.batch()
                    batches[index].forEach { userId ->
                        val document = firestore.collection(COLLECTION).document()
                        batch.set(document, mapOf(
                            "id" to document.id,
                            "userId" to userId,
                            "title" to title.trim(),
                            "body" to body.trim(),
                            "type" to "GENERAL",
                            "audience" to audience,
                            "appointmentId" to "",
                            "isRead" to false,
                            "createdAt" to FieldValue.serverTimestamp(),
                        ))
                    }
                    batch.commit().addOnSuccessListener { commitBatch(index + 1) }
                        .addOnFailureListener { callback(Result.failure(it)) }
                }
                commitBatch(0)
            }.addOnFailureListener { callback(Result.failure(it)) }
    }

    fun observeMine(callback: (Result<List<AppNotification>>) -> Unit): ListenerRegistration? {
        val userId = auth.currentUser?.uid ?: run {
            callback(Result.failure(IllegalStateException("Vui lòng đăng nhập lại.")))
            return null
        }
        return firestore.collection(COLLECTION).whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    callback(Result.failure(error))
                    return@addSnapshotListener
                }
                callback(Result.success(snapshot?.documents.orEmpty().map { document ->
                    AppNotification(
                        id = document.id,
                        userId = userId,
                        title = document.getString("title").orEmpty(),
                        body = document.getString("body").orEmpty(),
                        type = document.getString("type").orEmpty(),
                        appointmentId = document.getString("appointmentId").orEmpty(),
                        isRead = document.getBoolean("isRead") ?: false,
                        createdAt = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                    )
                }.sortedByDescending(AppNotification::createdAt)))
            }
    }

    fun getBroadcastHistory(callback: (Result<List<AppNotification>>) -> Unit) {
        firestore.collection(COLLECTION).whereEqualTo("type", "GENERAL").limit(300).get()
            .addOnSuccessListener { snapshot ->
                callback(Result.success(snapshot.documents.map { document ->
                    AppNotification(
                        id = document.id,
                        userId = document.getString("userId").orEmpty(),
                        title = document.getString("title").orEmpty(),
                        body = document.getString("body").orEmpty(),
                        type = "GENERAL",
                        createdAt = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                    )
                }.distinctBy { "${it.body}|${it.createdAt / 60_000}" }
                    .sortedByDescending(AppNotification::createdAt)))
            }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    /** Admin history includes automatic appointment notices and messages sent to one patient. */
    fun getAdminSentHistory(callback: (Result<List<AppNotification>>) -> Unit) {
        firestore.collection(COLLECTION).orderBy("createdAt", Query.Direction.DESCENDING).limit(300).get()
            .addOnSuccessListener { snapshot ->
                val sentTypes = setOf("GENERAL", "ADMIN_MESSAGE", "APPOINTMENT_CONFIRMED",
                    "APPOINTMENT_REJECTED", "APPOINTMENT_RESCHEDULED")
                callback(Result.success(snapshot.documents.mapNotNull { document ->
                    val type = document.getString("type").orEmpty()
                    if (type !in sentTypes) return@mapNotNull null
                    AppNotification(
                        id = document.id,
                        userId = document.getString("userId").orEmpty(),
                        title = document.getString("title").orEmpty(),
                        body = document.getString("body").orEmpty(),
                        type = type,
                        audience = document.getString("audience").orEmpty(),
                        appointmentId = document.getString("appointmentId").orEmpty(),
                        createdAt = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                    )
                }.distinctBy { if (it.type == "GENERAL") "${it.body}|${it.audience}|${it.createdAt / 60_000}" else it.id }))
            }.addOnFailureListener { callback(Result.failure(it)) }
    }

    fun markRead(notificationId: String) {
        firestore.collection(COLLECTION).document(notificationId).update(
            mapOf("isRead" to true, "readAt" to FieldValue.serverTimestamp()),
        )
    }

    fun saveFcmToken(token: String) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection(USERS_COLLECTION).document(userId).update(
            mapOf("fcmToken" to token, "updatedAt" to FieldValue.serverTimestamp()),
        )
    }

    private companion object {
        const val COLLECTION = "notifications"
        const val USERS_COLLECTION = "users"
    }
}
