package com.example.medicare.data.chat

import com.example.medicare.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

class ConsultationRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun observeMessages(
        appointmentId: String,
        callback: (Result<List<ChatMessage>>) -> Unit,
    ): ListenerRegistration {
        return firestore.collection(CONSULTATIONS_COLLECTION).document(appointmentId)
            .collection(MESSAGES_COLLECTION)
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    callback(Result.failure(error))
                    return@addSnapshotListener
                }
                callback(
                    Result.success(
                        snapshot?.documents.orEmpty().mapNotNull { document ->
                            val senderId = document.getString("senderId") ?: return@mapNotNull null
                            val content = document.getString("content") ?: return@mapNotNull null
                            val readBy = (document.get("readBy") as? List<*>)
                                ?.filterIsInstance<String>()
                                .orEmpty()
                            ChatMessage(document.id, senderId, content, readBy)
                        },
                    ),
                )
            }
    }

    fun sendMessage(appointmentId: String, content: String, callback: (Result<Unit>) -> Unit) {
        val senderId = auth.currentUser?.uid ?: run {
            callback(Result.failure(IllegalStateException("Vui lòng đăng nhập lại.")))
            return
        }
        val message = firestore.collection(CONSULTATIONS_COLLECTION).document(appointmentId)
            .collection(MESSAGES_COLLECTION).document()
        message.set(
            hashMapOf(
                "id" to message.id,
                "senderId" to senderId,
                "content" to content.trim(),
                "readBy" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        )
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun markIncomingMessagesRead(appointmentId: String, messages: List<ChatMessage>) {
        val currentUserId = auth.currentUser?.uid ?: return
        val unreadMessages = messages.filter { message ->
            message.senderId != currentUserId && currentUserId !in message.readBy
        }
        if (unreadMessages.isEmpty()) return

        firestore.runBatch { batch ->
            unreadMessages.forEach { message ->
                batch.update(
                    firestore.collection(CONSULTATIONS_COLLECTION).document(appointmentId)
                        .collection(MESSAGES_COLLECTION).document(message.id),
                    "readBy",
                    FieldValue.arrayUnion(currentUserId),
                )
            }
        }
    }

    fun observeTyping(
        appointmentId: String,
        callback: (Result<List<String>>) -> Unit,
    ): ListenerRegistration = firestore.collection(CONSULTATIONS_COLLECTION).document(appointmentId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                callback(Result.failure(error))
                return@addSnapshotListener
            }
            val typingUserIds = (snapshot?.get("typingUserIds") as? List<*>)
                ?.filterIsInstance<String>()
                .orEmpty()
            callback(Result.success(typingUserIds))
        }

    fun setTyping(appointmentId: String, isTyping: Boolean, callback: (Result<Unit>) -> Unit = {}) {
        val currentUserId = auth.currentUser?.uid ?: return
        val conversation = firestore.collection(CONSULTATIONS_COLLECTION).document(appointmentId)
        val typingUpdate = hashMapOf<String, Any>(
            "typingUserIds" to if (isTyping) FieldValue.arrayUnion(currentUserId) else FieldValue.arrayRemove(currentUserId),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (isTyping) {
            conversation.set(typingUpdate, SetOptions.merge())
        } else {
            conversation.update(typingUpdate)
        }
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    private companion object {
        const val CONSULTATIONS_COLLECTION = "consultations"
        const val MESSAGES_COLLECTION = "messages"
    }
}
