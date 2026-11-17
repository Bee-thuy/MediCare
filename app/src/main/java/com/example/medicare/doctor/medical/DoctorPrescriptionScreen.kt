package com.example.medicare.doctor.medical

import android.app.AlertDialog
import android.content.Intent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.data.appointment.AppointmentRepository
import com.example.medicare.data.medical.MedicalRecordRepository
import com.example.medicare.databinding.ActivityDoctorPrescriptionsBinding
import com.example.medicare.databinding.ItemDoctorPrescriptionBinding
import com.example.medicare.model.Appointment
import com.example.medicare.model.AppointmentStatus
import com.example.medicare.model.MedicalRecord
import com.google.firebase.auth.FirebaseAuth

/** Firebase behavior shared by the standalone and shell prescription screens. */
class DoctorPrescriptionScreen(
    private val activity: AppCompatActivity,
    private val binding: ActivityDoctorPrescriptionsBinding,
    private val isActive: () -> Boolean,
) {
    private var currentAppointments: List<Appointment> = emptyList()

    init {
        binding.doctorPrescriptionsNew.setOnClickListener { chooseAppointment() }
        binding.doctorPrescriptionsBottomNew.setOnClickListener { chooseAppointment() }
    }

    fun load() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        AppointmentRepository().getAppointmentsForDoctor(uid) { appointmentsResult ->
            if (!isActive()) return@getAppointmentsForDoctor
            currentAppointments = appointmentsResult.getOrDefault(emptyList())
            val appointments = currentAppointments.associateBy { it.id }
            MedicalRecordRepository().getRecordsForDoctor(uid) { recordsResult ->
                if (!isActive()) return@getRecordsForDoctor
                binding.doctorPrescriptionsList.removeAllViews()
                val records = recordsResult.getOrDefault(emptyList()).filter { it.prescriptionItems.isNotEmpty() }
                binding.doctorPrescriptionsEmpty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
                records.forEach { record -> addPrescription(record, appointments[record.appointmentId]) }
                recordsResult.exceptionOrNull()?.let {
                    binding.doctorPrescriptionsEmpty.text = it.message ?: "Không tải được đơn thuốc."
                }
            }
        }
    }

    private fun addPrescription(record: MedicalRecord, appointment: Appointment?) {
        val row = ItemDoctorPrescriptionBinding.inflate(activity.layoutInflater, binding.doctorPrescriptionsList, false)
        row.doctorPrescriptionPatient.text = "💊 ${appointment?.patientName?.ifBlank { "Bệnh nhân" } ?: "Bệnh nhân"}"
        row.doctorPrescriptionSummary.text = "${record.appointmentId} · ${record.prescriptionItems.size} loại thuốc"
        row.doctorPrescriptionOpen.text = if (appointment?.status == AppointmentStatus.IN_PROGRESS)
            "Xem / chỉnh đơn" else "Xem đơn"
        record.prescriptionItems.forEach { item ->
            val line = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
            val name = TextView(activity).apply {
                text = item.name
                textSize = 10f
                setTextColor(activity.getColor(R.color.manager_muted))
            }
            val dosage = TextView(activity).apply {
                text = item.dosage.ifBlank { item.instructions.ifBlank { "Theo hướng dẫn bác sĩ" } }
                textSize = 10f
                setTextColor(activity.getColor(R.color.manager_ink))
            }
            line.addView(name, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            line.addView(dosage, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT))
            row.doctorPrescriptionMedications.addView(line)
        }
        row.doctorPrescriptionOpen.setOnClickListener {
            if (appointment != null) openRecord(appointment) else AlertDialog.Builder(activity)
                .setMessage("Không tìm thấy lượt khám liên kết với đơn thuốc này.")
                .setPositiveButton("Đã hiểu", null).show()
        }
        binding.doctorPrescriptionsList.addView(row.root)
    }

    private fun chooseAppointment() {
        val ongoing = currentAppointments.filter { it.status == AppointmentStatus.IN_PROGRESS }
        if (ongoing.isEmpty()) {
            AlertDialog.Builder(activity).setTitle("Chưa có lượt khám đang thực hiện")
                .setMessage("Mở Lịch khám cá nhân và bắt đầu khám trước khi kê đơn thuốc cho bệnh nhân.")
                .setPositiveButton("Đã hiểu", null).show()
            return
        }
        val labels = ongoing.map { "${it.patientName.ifBlank { "Bệnh nhân" }} · ${it.appointmentDate} ${it.startTime}" }.toTypedArray()
        AlertDialog.Builder(activity).setTitle("Chọn lượt khám để kê đơn")
            .setItems(labels) { _, index -> openRecord(ongoing[index]) }.show()
    }

    private fun openRecord(a: Appointment) {
        activity.startActivity(Intent(activity, CompleteAppointmentActivity::class.java)
            .putExtra(CompleteAppointmentActivity.EXTRA_ID, a.id)
            .putExtra(CompleteAppointmentActivity.EXTRA_PATIENT_ID, a.patientId)
            .putExtra(CompleteAppointmentActivity.EXTRA_PATIENT_NAME, a.patientName)
            .putExtra(CompleteAppointmentActivity.EXTRA_DOCTOR_ID, a.doctorId)
            .putExtra(CompleteAppointmentActivity.EXTRA_DOCTOR_NAME, a.doctorName)
            .putExtra(CompleteAppointmentActivity.EXTRA_SPECIALTY, a.specialtyName)
            .putExtra(CompleteAppointmentActivity.EXTRA_SCHEDULE_ID, a.scheduleId)
            .putExtra(CompleteAppointmentActivity.EXTRA_DATE, a.appointmentDate)
            .putExtra(CompleteAppointmentActivity.EXTRA_START_TIME, a.startTime)
            .putExtra(CompleteAppointmentActivity.EXTRA_END_TIME, a.endTime)
            .putExtra(CompleteAppointmentActivity.EXTRA_REASON, a.reason)
            .putExtra(CompleteAppointmentActivity.EXTRA_STATUS, a.status.name))
    }
}
