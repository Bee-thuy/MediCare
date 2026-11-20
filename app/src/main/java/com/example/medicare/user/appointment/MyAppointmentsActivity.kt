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
import com.example.medicare.user.medical.MedicalHistoryActivity
import com.example.medicare.user.chat.PatientChatsActivity
import com.example.medicare.notification.ReminderScheduler
import com.example.medicare.user.review.MyReviewsActivity
import com.example.medicare.user.PatientDetailSheets
import com.example.medicare.user.PatientHomeActivity
import com.example.medicare.user.PatientScreenChrome
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MyAppointmentsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyAppointmentsBinding
    private val appointmentRepository = AppointmentRepository()
    private var appointments: List<Appointment> = emptyList()
    private var selectedStatus: AppointmentStatus? = null
    private var proposedOnly = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyAppointmentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.myAppointmentsBack.setOnClickListener { finish() }
        binding.myAppointmentsAllTab.setOnClickListener { selectStatus(null) }
        binding.myAppointmentsPendingTab.setOnClickListener { selectStatus(AppointmentStatus.PENDING) }
        binding.myAppointmentsConfirmedTab.setOnClickListener { selectStatus(AppointmentStatus.CONFIRMED) }
        binding.myAppointmentsProposedTab.setOnClickListener { proposedOnly = true; selectedStatus = null; updateTabs(); renderAppointments() }
        binding.myAppointmentsCompletedTab.setOnClickListener { selectStatus(AppointmentStatus.COMPLETED) }
        binding.myAppointmentsCancelledTab.setOnClickListener { selectStatus(AppointmentStatus.CANCELLED) }
        binding.myAppointmentsNew.setOnClickListener {
            startActivity(Intent(this, PatientHomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(PatientHomeActivity.EXTRA_TAB, PatientHomeActivity.TAB_BOOKING))
        }
        PatientScreenChrome.bind(this, "Quản lý lịch khám")
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
        proposedOnly = false
        selectedStatus = status
        updateTabs()
        renderAppointments()
    }

    private fun updateTabs() {
        listOf(
            binding.myAppointmentsAllTab to (selectedStatus == null && !proposedOnly),
            binding.myAppointmentsPendingTab to (selectedStatus == AppointmentStatus.PENDING),
            binding.myAppointmentsConfirmedTab to (selectedStatus == AppointmentStatus.CONFIRMED),
            binding.myAppointmentsProposedTab to proposedOnly,
            binding.myAppointmentsCompletedTab to (selectedStatus == AppointmentStatus.COMPLETED),
            binding.myAppointmentsCancelledTab to (selectedStatus == AppointmentStatus.CANCELLED),
        ).forEach { (view, selected) ->
            view.isSelected = selected
            view.setTextColor(ContextCompat.getColor(this, if (selected) R.color.white else R.color.manager_muted))
        }
    }

    private fun renderAppointments() {
        val visibleAppointments = appointments.filter {
            if (proposedOnly) it.rescheduledFromId.isNotBlank() && it.status == AppointmentStatus.PENDING
            else selectedStatus == null || it.status == selectedStatus
        }
        binding.myAppointmentsList.removeAllViews()
        binding.myAppointmentsEmpty.visibility = if (visibleAppointments.isEmpty()) View.VISIBLE else View.GONE
        visibleAppointments.forEach { appointment ->
            if (appointment.status == AppointmentStatus.CONFIRMED) {
                ReminderScheduler.appointment(this, appointment.id, appointment.appointmentDate, appointment.startTime)
            }
            val row = ItemUserAppointmentBinding.inflate(layoutInflater, binding.myAppointmentsList, false)
            row.userAppointmentDoctorName.text = "BS. ${appointment.doctorName}"
            row.userAppointmentSpecialty.text = appointment.specialtyName
            val date = runCatching { LocalDate.parse(appointment.appointmentDate)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrDefault(appointment.appointmentDate)
            row.userAppointmentTime.text = "$date · ${appointment.startTime} · ${appointment.specialtyName}"
            row.userAppointmentReason.text = appointment.reason.ifBlank { getString(R.string.patient_no_reason) }
            row.userAppointmentStatus.text = if (appointment.rescheduledFromId.isNotBlank() && appointment.status == AppointmentStatus.PENDING)
                "Đề xuất đổi giờ" else statusLabel(appointment.status)
            applyStatusStyle(row.userAppointmentStatus, appointment.status)
            row.root.setOnClickListener { PatientDetailSheets.appointment(this, appointment, ::loadAppointments) }
            row.root.setOnLongClickListener { showAppointmentActions(appointment); true }
            binding.myAppointmentsList.addView(row.root)
        }
    }

    private fun showAppointmentActions(appointment: Appointment) {
        val actions = mutableListOf<Pair<String, () -> Unit>>()
        actions += "Thông tin lịch khám" to {
            AlertDialog.Builder(this).setTitle("BS. ${appointment.doctorName}")
                .setMessage("${appointment.appointmentDate} · ${appointment.startTime}–${appointment.endTime}\n${appointment.specialtyName}\nLý do: ${appointment.reason.ifBlank { "Chưa nhập" }}\nTrạng thái: ${statusLabel(appointment.status)}")
                .setPositiveButton("Đóng", null).show()
        }
        if (appointment.status == AppointmentStatus.COMPLETED) {
            actions += "Xem kết quả khám" to { openAppointment(appointment) }
            actions += "Đánh giá bác sĩ" to {
                startActivity(Intent(this, MyReviewsActivity::class.java)
                    .putExtra(MyReviewsActivity.EXTRA_OPEN_APPOINTMENT_ID, appointment.id))
            }
        }
        if (appointment.status in setOf(AppointmentStatus.CONFIRMED, AppointmentStatus.ARRIVED,
                AppointmentStatus.IN_PROGRESS, AppointmentStatus.COMPLETED))
            actions += "Chat bác sĩ" to { openChat(appointment) }
        if (appointment.status in setOf(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED))
            actions += "Hủy lịch khám" to { showCancelDialog(appointment) }
        AlertDialog.Builder(this).setTitle("Lịch khám")
            .setItems(actions.map { it.first }.toTypedArray()) { _, index -> actions[index].second() }
            .show()
    }

    private fun openAppointment(appointment: Appointment) {
        if (appointment.status != AppointmentStatus.COMPLETED) {
            Toast.makeText(this, R.string.patient_result_not_available, Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            Intent(this, MedicalHistoryActivity::class.java)
                .putExtra(MedicalHistoryActivity.EXTRA_OPEN_APPOINTMENT_ID, appointment.id),
        )
    }

    private fun applyStatusStyle(view: TextView, status: AppointmentStatus) {
        val background = when (status) {
            AppointmentStatus.PENDING -> R.drawable.manager_appointment_pending_background
            AppointmentStatus.REJECTED, AppointmentStatus.CANCELLED -> R.drawable.manager_status_inactive_background
            AppointmentStatus.CONFIRMED -> R.drawable.manager_status_active_background
            AppointmentStatus.COMPLETED -> R.drawable.patient_completed_status_background
            AppointmentStatus.ARRIVED, AppointmentStatus.IN_PROGRESS -> R.drawable.manager_appointment_pending_background
        }
        val color = when (status) {
            AppointmentStatus.PENDING -> R.color.manager_orange
            AppointmentStatus.REJECTED, AppointmentStatus.CANCELLED -> R.color.manager_red
            AppointmentStatus.CONFIRMED -> R.color.manager_green
            AppointmentStatus.COMPLETED -> R.color.manager_blue_dark
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
            Intent(this, PatientChatsActivity::class.java)
                .putExtra(PatientChatsActivity.EXTRA_DOCTOR_ID, appointment.doctorId),
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
