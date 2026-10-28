package com.example.medicare.manager.user

import android.content.Intent
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.medicare.R
import com.example.medicare.databinding.ActivityDetailPatientBinding
import com.example.medicare.databinding.DialogConfirmAppointmentBinding
import com.example.medicare.databinding.DialogResolveAppointmentConflictBinding

/** Detail screen for one mock appointment request in the manager appointment list. */
class DetailPatientActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailPatientBinding
    private lateinit var appointmentId: String
    private var patientName: String = ""
    private var appointmentStatus: String = STATUS_PENDING
    private var hasTimeConflict: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDetailPatientBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        appointmentId = intent.getStringExtra(EXTRA_APPOINTMENT_ID).orEmpty()
        appointmentStatus = intent.getStringExtra(EXTRA_STATUS) ?: STATUS_PENDING
        hasTimeConflict = intent.getBooleanExtra(EXTRA_HAS_TIME_CONFLICT, false)
        bindAppointment()
        binding.detailPatientBack.setOnClickListener { finish() }
        binding.detailConfirmButton.setOnClickListener {
            if (hasTimeConflict) showConflictResolutionDialog() else showConfirmationDialog()
        }
        binding.detailRejectButton.setOnClickListener { updateAppointmentStatus(STATUS_REJECTED) }
        binding.detailSendNotificationButton.setOnClickListener { sendPatientNotification() }
    }

    private fun bindAppointment() {
        patientName = intent.getStringExtra(EXTRA_PATIENT_NAME) ?: "Nguyễn Thị Mai"
        binding.detailPatientInitials.text = initialsOf(patientName)
        binding.detailPatientName.text = patientName
        binding.detailPatientInfo.text = intent.getStringExtra(EXTRA_PATIENT_INFO) ?: "Nữ · 28 tuổi · 0123 456 789"
        binding.detailAppointmentDate.text = intent.getStringExtra(EXTRA_DATE) ?: "26/05/2025 (Thứ 2)"
        binding.detailAppointmentTime.text = intent.getStringExtra(EXTRA_TIME) ?: "08:00 - 08:30"
        binding.detailAppointmentDoctor.text = intent.getStringExtra(EXTRA_DOCTOR) ?: "BS. Nguyễn Văn A\nChuyên khoa Tim mạch"
        binding.detailAppointmentSpecialty.text = intent.getStringExtra(EXTRA_SPECIALTY) ?: "Tim mạch"
        binding.detailAppointmentReason.text = intent.getStringExtra(EXTRA_REASON) ?: "Khám tổng quát"
        binding.detailAppointmentNote.text = intent.getStringExtra(EXTRA_NOTE) ?: getString(R.string.manager_no_note)
        renderStatus()
    }

    private fun showConfirmationDialog() {
        val dialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
        }
        val dialogBinding = DialogConfirmAppointmentBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialogBinding.confirmAppointmentMessage.text = getString(
            R.string.manager_confirm_appointment_message,
            patientName,
        )
        dialogBinding.confirmAppointmentCancel.setOnClickListener { dialog.dismiss() }
        dialogBinding.confirmAppointmentAccept.setOnClickListener {
            dialog.dismiss()
            updateAppointmentStatus(STATUS_CONFIRMED)
        }
        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout((resources.displayMetrics.widthPixels * 0.84).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            setDimAmount(0.55f)
        }
    }

    private fun showConflictResolutionDialog() {
        val dialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
        }
        val dialogBinding = DialogResolveAppointmentConflictBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialogBinding.resolveConflictMessage.text = getString(
            R.string.manager_conflict_dialog_message,
            binding.detailAppointmentDoctor.text.toString().substringBefore("\n"),
            binding.detailAppointmentTime.text.toString(),
        )
        dialogBinding.resolveConflictClose.setOnClickListener { dialog.dismiss() }
        dialogBinding.resolveConflictSuggestTime.setOnClickListener {
            dialog.dismiss()
            startActivity(
                Intent(this, ProposeScheduleActivity::class.java)
                    .putExtra(ProposeScheduleActivity.EXTRA_DATE, binding.detailAppointmentDate.text.toString()),
            )
        }
        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout((resources.displayMetrics.widthPixels * 0.88).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
            setDimAmount(0.55f)
        }
    }

    private fun renderStatus() {
        val pending = appointmentStatus == STATUS_PENDING
        val rejected = appointmentStatus == STATUS_REJECTED
        val conflictNeedsResolution = pending && hasTimeConflict
        binding.detailAppointmentStatus.text = when {
            conflictNeedsResolution -> getString(R.string.manager_time_conflict)
            appointmentStatus == STATUS_CONFIRMED -> getString(R.string.manager_confirmed)
            appointmentStatus == STATUS_REJECTED -> getString(R.string.manager_reject)
            else -> getString(R.string.manager_pending)
        }
        binding.detailAppointmentStatus.setBackgroundResource(
            when {
                conflictNeedsResolution -> R.drawable.manager_status_inactive_background
                pending -> R.drawable.manager_appointment_pending_background
                rejected -> R.drawable.manager_status_inactive_background
                else -> R.drawable.manager_status_active_background
            },
        )
        binding.detailAppointmentStatus.setTextColor(
            ContextCompat.getColor(
                this,
                when {
                    conflictNeedsResolution -> R.color.manager_red
                    pending -> R.color.manager_orange
                    rejected -> R.color.manager_red
                    else -> R.color.manager_green
                },
            ),
        )
        binding.detailConflictAlert.visibility = if (conflictNeedsResolution) View.VISIBLE else View.GONE
        binding.detailConflictMessage.text = getString(
            R.string.manager_conflict_detail_message,
            binding.detailAppointmentTime.text.toString(),
            binding.detailAppointmentDoctor.text.toString().substringBefore("\n"),
        )
        binding.detailAppointmentDecisionActions.visibility = if (pending) View.VISIBLE else View.GONE
        binding.detailConfirmButton.text = getString(
            if (conflictNeedsResolution) R.string.manager_resolve_conflict else R.string.manager_accept,
        )
        binding.detailSendNotificationButton.visibility = if (appointmentStatus == STATUS_CONFIRMED) View.VISIBLE else View.GONE
        binding.detailAppointmentActions.visibility = if (rejected) View.GONE else View.VISIBLE
    }

    private fun updateAppointmentStatus(status: String) {
        appointmentStatus = status
        setResult(
            RESULT_OK,
            Intent()
                .putExtra(EXTRA_APPOINTMENT_ID, appointmentId)
                .putExtra(EXTRA_STATUS, appointmentStatus),
        )
        renderStatus()
        Toast.makeText(
            this,
            if (status == STATUS_CONFIRMED) R.string.manager_appointment_accepted else R.string.manager_appointment_rejected,
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun sendPatientNotification() {
        startActivity(
            Intent(this, PatientNotificationActivity::class.java)
                .putExtra(PatientNotificationActivity.EXTRA_PATIENT_NAME, patientName)
                .putExtra(PatientNotificationActivity.EXTRA_APPOINTMENT_TIME, binding.detailAppointmentTime.text.toString())
                .putExtra(PatientNotificationActivity.EXTRA_APPOINTMENT_DATE, binding.detailAppointmentDate.text.toString()),
        )
    }

    private fun initialsOf(name: String): String {
        return name.split(" ")
            .filter { it.isNotBlank() }
            .takeLast(2)
            .joinToString("") { it.first().toString() }
    }

    companion object {
        const val EXTRA_APPOINTMENT_ID = "appointment_id"
        const val EXTRA_PATIENT_NAME = "patient_name"
        const val EXTRA_PATIENT_INFO = "patient_info"
        const val EXTRA_DATE = "appointment_date"
        const val EXTRA_TIME = "appointment_time"
        const val EXTRA_DOCTOR = "appointment_doctor"
        const val EXTRA_SPECIALTY = "appointment_specialty"
        const val EXTRA_REASON = "appointment_reason"
        const val EXTRA_NOTE = "appointment_note"
        const val EXTRA_STATUS = "appointment_status"
        const val EXTRA_HAS_TIME_CONFLICT = "appointment_has_time_conflict"

        const val STATUS_PENDING = "PENDING"
        const val STATUS_CONFIRMED = "CONFIRMED"
        const val STATUS_REJECTED = "REJECTED"
    }
}
