package com.example.medicare.notification

import android.view.View
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.medicare.data.notification.NotificationRepository
import com.google.firebase.firestore.ListenerRegistration

/** Keeps a red numeric badge in sync with unread in-app notifications. */
object NotificationBadge {
    fun bind(owner: LifecycleOwner, badgeView: TextView) {
        var registration: ListenerRegistration? = null
        val repository = NotificationRepository()
        owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                registration?.remove()
                registration = repository.observeMine { result ->
                    result.onSuccess { notifications ->
                        val unread = notifications.count { !it.isRead }
                        if (unread <= 0) {
                            badgeView.visibility = View.GONE
                            badgeView.text = ""
                        } else {
                            badgeView.visibility = View.VISIBLE
                            badgeView.text = if (unread > 99) "99+" else unread.toString()
                        }
                    }.onFailure {
                        badgeView.visibility = View.GONE
                    }
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                registration?.remove()
                registration = null
            }
        })
    }
}
