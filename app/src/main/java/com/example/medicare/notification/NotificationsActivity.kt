package com.example.medicare.notification

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.data.notification.NotificationRepository
import com.example.medicare.databinding.ActivityNotificationsBinding
import com.example.medicare.databinding.ItemAppNotificationBinding
import com.google.firebase.firestore.ListenerRegistration
import java.text.DateFormat
import java.util.Date

class NotificationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationsBinding
    private val repository = NotificationRepository()
    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.notificationsBack.setOnClickListener { finish() }
        listener = repository.observeMine { result ->
            result.onSuccess { notifications ->
                binding.notificationsList.removeAllViews()
                binding.notificationsEmpty.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
                notifications.forEach { item ->
                    val row = ItemAppNotificationBinding.inflate(layoutInflater, binding.notificationsList, false)
                    row.appNotificationTitle.text = item.title
                    row.appNotificationBody.text = item.body
                    row.appNotificationTime.text = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(item.createdAt))
                    row.root.alpha = if (item.isRead) .62f else 1f
                    row.root.setOnClickListener { repository.markRead(item.id); row.root.alpha = .62f }
                    binding.notificationsList.addView(row.root)
                }
            }
        }
    }

    override fun onDestroy() { listener?.remove(); super.onDestroy() }
}
