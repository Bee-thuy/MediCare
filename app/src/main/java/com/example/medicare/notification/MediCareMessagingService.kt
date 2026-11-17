package com.example.medicare.notification

import com.example.medicare.data.notification.NotificationRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MediCareMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        NotificationRepository().saveFcmToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification
        val title = notification?.title ?: message.data["title"] ?: getString(com.example.medicare.R.string.app_name)
        val body = notification?.body ?: message.data["body"] ?: "Bạn có một cập nhật mới."
        AppNotificationManager.show(this, title, body, message.messageId?.hashCode() ?: title.hashCode())
    }
}
