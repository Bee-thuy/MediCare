package com.example.medicare.manager.appointment

import android.os.Bundle
import android.widget.Toast
import android.widget.ArrayAdapter
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.medicare.R
import com.example.medicare.databinding.ActivityPatientNotificationBinding
import com.example.medicare.databinding.ActivityNotificationSentBinding
import com.example.medicare.data.notification.NotificationRepository

/** Composes a persisted in-app notification for an appointment or selected audience. */
class PatientNotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPatientNotificationBinding
    private var appointmentTime: String = ""
    private var appointmentDate: String = ""
    private var patientId: String = ""
    private var appointmentId: String = ""
    private val notifications = NotificationRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (intent.getBooleanExtra(EXTRA_SHOW_SENT_STATE, false)) {
            showNotificationSent(intent.getStringExtra(EXTRA_SENT_CONTENT).orEmpty())
            return
        }
        binding = ActivityPatientNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        appointmentTime = intent.getStringExtra(EXTRA_APPOINTMENT_TIME) ?: appointmentTime
        appointmentDate = intent.getStringExtra(EXTRA_APPOINTMENT_DATE) ?: appointmentDate
        patientId = intent.getStringExtra(EXTRA_PATIENT_ID).orEmpty()
        appointmentId = intent.getStringExtra(EXTRA_APPOINTMENT_ID).orEmpty()
        binding.patientNotificationBack.setOnClickListener { finish() }
        binding.patientNotificationTypes.setOnCheckedChangeListener { _, checkedId ->
            binding.patientNotificationContent.setText(contentFor(checkedId))
        }
        binding.patientNotificationSend.setOnClickListener { sendNotification() }
        if (intent.getBooleanExtra(EXTRA_SEND_TO_ALL, false)) {
            binding.patientNotificationTypeLabel.visibility = View.GONE
            binding.patientNotificationTypes.visibility = View.GONE
            binding.patientNotificationAudienceLabel.visibility = View.VISIBLE
            binding.patientNotificationAudience.visibility = View.VISIBLE
            binding.patientNotificationAudience.adapter = ArrayAdapter(this,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("Tất cả tài khoản", "Bác sĩ", "Bệnh nhân"))
            binding.patientNotificationContent.setText("")
            binding.patientNotificationContent.hint = "Ví dụ: Hệ thống bảo trì lúc..."
        } else {
            binding.patientNotificationConfirmed.isChecked = true
            binding.patientNotificationContent.setText(contentFor(R.id.patientNotificationConfirmed))
        }
    }

    private fun contentFor(typeId: Int): String = when (typeId) {
        R.id.patientNotificationConfirmed -> getString(
            R.string.manager_notification_confirmed_template,
            appointmentTime,
            appointmentDate,
        )
        R.id.patientNotificationRejected -> getString(R.string.manager_notification_rejected_template)
        R.id.patientNotificationRescheduled -> getString(R.string.manager_notification_rescheduled_template)
        R.id.patientNotificationCancelled -> getString(R.string.manager_notification_cancelled_template)
        else -> ""
    }

    private fun sendNotification() {
        if (binding.patientNotificationContent.text.isNullOrBlank()) {
            binding.patientNotificationContent.error = getString(R.string.manager_required_field)
            return
        }
        val content = binding.patientNotificationContent.text.toString().trim()
        binding.patientNotificationSend.isEnabled = false
        val done: (Result<*>) -> Unit = { result ->
            binding.patientNotificationSend.isEnabled = true
            result.onSuccess { showNotificationSent(content) }
                .onFailure { Toast.makeText(this, it.message ?: "Không thể lưu thông báo.", Toast.LENGTH_LONG).show() }
        }
        if (intent.getBooleanExtra(EXTRA_SEND_TO_ALL, false)) {
            val audience = listOf("ALL", "DOCTOR", "USER")[binding.patientNotificationAudience.selectedItemPosition.coerceIn(0, 2)]
            notifications.createForAudience("Thông báo từ MediCare", content, audience) { done(it) }
        } else {
            notifications.create(
                userId = patientId,
                title = "Thông báo lịch khám",
                body = content,
                type = "ADMIN_MESSAGE",
                appointmentId = appointmentId,
            ) { done(it) }
        }
    }

    private fun showNotificationSent(content: String) {
        val sentBinding = ActivityNotificationSentBinding.inflate(layoutInflater)
        setContentView(sentBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(sentBinding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        sentBinding.notificationSentContent.text = content
        sentBinding.notificationSentBack.setOnClickListener { finish() }
        sentBinding.notificationSentBackArrow.setOnClickListener { finish() }
    }

    companion object {
        const val EXTRA_PATIENT_NAME = "notification_patient_name"
        const val EXTRA_PATIENT_ID = "notification_patient_id"
        const val EXTRA_APPOINTMENT_ID = "notification_appointment_id"
        const val EXTRA_APPOINTMENT_TIME = "notification_appointment_time"
        const val EXTRA_APPOINTMENT_DATE = "notification_appointment_date"
        const val EXTRA_SHOW_SENT_STATE = "show_notification_sent_state"
        const val EXTRA_SENT_CONTENT = "notification_sent_content"
        const val EXTRA_SEND_TO_ALL = "notification_send_to_all"
    }
}
