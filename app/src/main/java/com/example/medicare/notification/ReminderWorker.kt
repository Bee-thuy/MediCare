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
            val workName = inputData.getString("workName")
            val time = inputData.getString("reminderTime") ?: body.substringAfter("· ", "")
            val name = inputData.getString("medicineName") ?: body.substringBefore(" ·")
            if (!workName.isNullOrBlank() && time.isNotBlank())
                ReminderScheduler.rescheduleMedication(applicationContext, workName, name, time)
            else {
                val legacyId = inputData.getString("id").orEmpty()
                if (legacyId.isNotBlank() && time.isNotBlank()) ReminderScheduler.medication(applicationContext, legacyId, name, time)
            }
        }
        return Result.success()
    }
}
