package com.example.medicare.manager.user

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.medicare.R
import com.example.medicare.databinding.ActivityPatientNotificationBinding
import com.example.medicare.databinding.ActivityNotificationSentBinding

/** Composes a mock in-app/email notification for the selected patient appointment. */
class PatientNotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPatientNotificationBinding
    private var appointmentTime: String = "08:00 - 08:30"
    private var appointmentDate: String = "26/05/2025"

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
        binding.patientNotificationBack.setOnClickListener { finish() }
        binding.patientNotificationTypes.setOnCheckedChangeListener { _, checkedId ->
            binding.patientNotificationContent.setText(contentFor(checkedId))
        }
        binding.patientNotificationSend.setOnClickListener { sendNotification() }
        binding.patientNotificationConfirmed.isChecked = true
        binding.patientNotificationContent.setText(contentFor(R.id.patientNotificationConfirmed))
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
        showNotificationSent(binding.patientNotificationContent.text.toString())
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
        const val EXTRA_APPOINTMENT_TIME = "notification_appointment_time"
        const val EXTRA_APPOINTMENT_DATE = "notification_appointment_date"
        const val EXTRA_SHOW_SENT_STATE = "show_notification_sent_state"
        const val EXTRA_SENT_CONTENT = "notification_sent_content"
    }
}
