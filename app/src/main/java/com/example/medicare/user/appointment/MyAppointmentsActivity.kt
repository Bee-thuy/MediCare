package com.example.medicare.user.appointment

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.medicare.R
import com.example.medicare.data.appointment.AppointmentRepository
import com.example.medicare.databinding.ActivityMyAppointmentsBinding
import com.example.medicare.databinding.ItemUserAppointmentBinding
import com.example.medicare.model.Appointment
import com.example.medicare.model.AppointmentStatus
import com.example.medicare.user.medical.MedicalRecordActivity
import com.example.medicare.ui.chat.ConsultationChatActivity
import com.example.medicare.notification.ReminderScheduler
import com.example.medicare.user.review.ReviewDoctorActivity
import com.google.firebase.auth.FirebaseAuth

class MyAppointmentsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyAppointmentsBinding
    private val appointmentRepository = AppointmentRepository()
    private var appointments: List<Appointment> = emptyList()
    private var selectedStatus: AppointmentStatus? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyAppointmentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.myAppointmentsBack.setOnClickListener { finish() }
        binding.myAppointmentsAllTab.setOnClickListener { selectStatus(null) }
        binding.myAppointmentsPendingTab.setOnClickListener { selectStatus(AppointmentStatus.PENDING) }
        binding.myAppointmentsConfirmedTab.setOnClickListener { selectStatus(AppointmentStatus.CONFIRMED) }
        binding.myAppointmentsCompletedTab.setOnClickListener { selectStatus(AppointmentStatus.COMPLETED) }
        selectStatus(null)
    }

    override fun onResume() {
        super.onResume()
        loadAppointments()
    }

    private fun loadAppointments() {
        val patientId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        appointmentRepository.getAppointmentsForPatient(patientId) { result ->
            result.onSuccess { loadedAppointments ->
                appointments = loadedAppointments
                renderAppointments()
            }.onFailure { error ->
                binding.myAppointmentsEmpty.text = error.message ?: getString(R.string.patient_appointments_load_failed)
                binding.myAppointmentsEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun selectStatus(status: AppointmentStatus?) {
        selectedStatus = status
        mapOf(
            null to binding.myAppointmentsAllTab,
            AppointmentStatus.PENDING to binding.myAppointmentsPendingTab,
            AppointmentStatus.CONFIRMED to binding.myAppointmentsConfirmedTab,
            AppointmentStatus.COMPLETED to binding.myAppointmentsCompletedTab,
        ).forEach { (tabStatus, view) ->
            val selected = tabStatus == status
            view.isSelected = selected
            view.setTextColor(ContextCompat.getColor(this, if (selected) R.color.white else R.color.manager_ink))
        }
        renderAppointments()
    }

    private fun renderAppointments() {
        val visibleAppointments = appointments.filter { selectedStatus == null || it.status == selectedStatus }
        binding.myAppointmentsList.removeAllViews()
        binding.myAppointmentsEmpty.visibility = if (visibleAppointments.isEmpty()) View.VISIBLE else View.GONE
        visibleAppointments.forEach { appointment ->
            if (appointment.status == AppointmentStatus.CONFIRMED) {
                ReminderScheduler.appointment(this, appointment.id, appointment.appointmentDate, appointment.startTime)
            }
            val row = ItemUserAppointmentBinding.inflate(layoutInflater, binding.myAppointmentsList, false)
            row.userAppointmentDoctorName.text = appointment.doctorName
            row.userAppointmentSpecialty.text = appointment.specialtyName
            row.userAppointmentTime.text = getString(
                R.string.patient_appointment_time,
                appointment.appointmentDate,
                appointment.startTime,
                appointment.endTime,
            )
            row.userAppointmentReason.text = appointment.reason.ifBlank { getString(R.string.patient_no_reason) }
            row.userAppointmentStatus.text = statusLabel(appointment.status)
            applyStatusStyle(row.userAppointmentStatus, appointment.status)
            row.root.setOnClickListener { openAppointment(appointment) }
            row.userAppointmentCancel.visibility = if (appointment.status in setOf(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED)) View.VISIBLE else View.GONE
            row.userAppointmentCancel.setOnClickListener { showCancelDialog(appointment) }
            row.userAppointmentChat.visibility = if (appointment.status in setOf(AppointmentStatus.CONFIRMED, AppointmentStatus.ARRIVED, AppointmentStatus.IN_PROGRESS, AppointmentStatus.COMPLETED)) View.VISIBLE else View.GONE
            row.userAppointmentChat.setOnClickListener { openChat(appointment) }
            row.userAppointmentReview.visibility = if (appointment.status == AppointmentStatus.COMPLETED) View.VISIBLE else View.GONE
            row.userAppointmentReview.setOnClickListener { startActivity(Intent(this, ReviewDoctorActivity::class.java).putExtra(ReviewDoctorActivity.EXTRA_APPOINTMENT_ID, appointment.id).putExtra(ReviewDoctorActivity.EXTRA_DOCTOR_ID, appointment.doctorId).putExtra(ReviewDoctorActivity.EXTRA_DOCTOR_NAME, appointment.doctorName)) }
            binding.myAppointmentsList.addView(row.root)
        }
    }

    private fun openAppointment(appointment: Appointment) {
        if (appointment.status != AppointmentStatus.COMPLETED) {
            Toast.makeText(this, R.string.patient_result_not_available, Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            Intent(this, MedicalRecordActivity::class.java)
                .putExtra(MedicalRecordActivity.EXTRA_APPOINTMENT_ID, appointment.id)
                .putExtra(MedicalRecordActivity.EXTRA_DOCTOR_NAME, appointment.doctorName)
                .putExtra(MedicalRecordActivity.EXTRA_SPECIALTY, appointment.specialtyName)
                .putExtra(MedicalRecordActivity.EXTRA_DATE, appointment.appointmentDate),
        )
    }

    private fun applyStatusStyle(view: TextView, status: AppointmentStatus) {
        val background = when (status) {
            AppointmentStatus.PENDING -> R.drawable.manager_appointment_pending_background
            AppointmentStatus.REJECTED, AppointmentStatus.CANCELLED -> R.drawable.manager_status_inactive_background
            AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED -> R.drawable.manager_status_active_background
            AppointmentStatus.ARRIVED, AppointmentStatus.IN_PROGRESS -> R.drawable.manager_appointment_pending_background
        }
        val color = when (status) {
            AppointmentStatus.PENDING -> R.color.manager_orange
            AppointmentStatus.REJECTED, AppointmentStatus.CANCELLED -> R.color.manager_red
            AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED -> R.color.manager_green
            AppointmentStatus.ARRIVED, AppointmentStatus.IN_PROGRESS -> R.color.manager_orange
        }
        view.setBackgroundResource(background)
        view.setTextColor(ContextCompat.getColor(this, color))
    }

    private fun showCancelDialog(appointment: Appointment) {
        val input = EditText(this).apply {
            hint = getString(R.string.patient_cancel_reason_hint)
            setPadding(dp(20), dp(4), dp(20), dp(4))
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.patient_cancel_dialog_title)
            .setMessage(R.string.patient_cancel_dialog_message)
            .setView(input)
            .setNegativeButton(R.string.manager_cancel, null)
            .setPositiveButton(R.string.patient_cancel_appointment) { _, _ ->
                appointmentRepository.cancelAppointmentByPatient(appointment.id, input.text.toString().trim()) { result ->
                    result.onSuccess {
                        Toast.makeText(this, R.string.patient_cancelled_success, Toast.LENGTH_SHORT).show()
                        loadAppointments()
                    }.onFailure { error ->
                        Toast.makeText(this, error.message ?: getString(R.string.patient_cancel_failed), Toast.LENGTH_LONG).show()
                    }
                }
            }
            .show()
    }

    private fun openChat(appointment: Appointment) {
        startActivity(
            Intent(this, ConsultationChatActivity::class.java)
                .putExtra(ConsultationChatActivity.EXTRA_APPOINTMENT_ID, appointment.id)
                .putExtra(ConsultationChatActivity.EXTRA_TITLE, appointment.doctorName),
        )
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun statusLabel(status: AppointmentStatus): String = when (status) {
        AppointmentStatus.PENDING -> getString(R.string.manager_pending)
        AppointmentStatus.CONFIRMED -> getString(R.string.doctor_status_confirmed)
        AppointmentStatus.ARRIVED -> getString(R.string.doctor_status_arrived)
        AppointmentStatus.IN_PROGRESS -> getString(R.string.doctor_status_in_progress)
        AppointmentStatus.COMPLETED -> getString(R.string.doctor_status_completed)
        AppointmentStatus.REJECTED -> getString(R.string.manager_reject)
        AppointmentStatus.CANCELLED -> getString(R.string.doctor_status_cancelled)
    }
}
