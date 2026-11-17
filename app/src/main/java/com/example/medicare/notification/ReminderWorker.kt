package com.example.medicare.notification

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val title = inputData.getString("title") ?: return Result.failure()
        val body = inputData.getString("body") ?: return Result.failure()
        AppNotificationManager.show(applicationContext, title, body, (title + body).hashCode())
        if (inputData.getBoolean("repeatDaily", false)) {
            val id = inputData.getString("id").orEmpty()
            val time = body.substringAfter("· ", "")
            val name = body.substringBefore(" ·")
            if (id.isNotBlank() && time.isNotBlank()) ReminderScheduler.medication(applicationContext, id, name, time)
        }
        return Result.success()
    }
}
