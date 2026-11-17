package com.example.medicare.notification

import com.example.medicare.data.notification.NotificationRepository
import com.google.firebase.messaging.FirebaseMessaging

object NotificationRegistration {
    fun registerCurrentDevice() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener(NotificationRepository()::saveFcmToken)
    }
}
