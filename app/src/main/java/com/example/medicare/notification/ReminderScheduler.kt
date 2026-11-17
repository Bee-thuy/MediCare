package com.example.medicare.notification

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    fun medication(context: Context, id: String, name: String, time: String) {
        val localTime = runCatching { LocalTime.parse(time) }.getOrNull() ?: return
        var trigger = LocalDateTime.of(LocalDate.now(), localTime)
        if (!trigger.isAfter(LocalDateTime.now())) trigger = trigger.plusDays(1)
        enqueue(context, "medication_$id", trigger, "Đến giờ uống thuốc", "$name · $time", workDataOf("repeatDaily" to true, "id" to id, "title" to "Đến giờ uống thuốc", "body" to "$name · $time"))
    }

    fun appointment(context: Context, id: String, date: String, time: String) {
        val appointmentTime = runCatching { LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time)) }.getOrNull() ?: return
        enqueue(context, "appointment_$id", appointmentTime.minusHours(24), "Nhắc lịch khám", "Bạn có lịch khám vào $date lúc $time.", workDataOf("repeatDaily" to false, "title" to "Nhắc lịch khám", "body" to "Bạn có lịch khám vào $date lúc $time."))
    }

    private fun enqueue(context: Context, name: String, trigger: LocalDateTime, title: String, body: String, data: Data) {
        val delay = Duration.between(LocalDateTime.now(), trigger).toMillis()
        if (delay <= 0) return
        val request = OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).setInputData(data).build()
        WorkManager.getInstance(context).enqueueUniqueWork(name, androidx.work.ExistingWorkPolicy.REPLACE, request)
    }
}
