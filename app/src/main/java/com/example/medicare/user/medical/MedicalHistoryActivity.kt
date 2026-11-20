package com.example.medicare.user.medical

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.data.medical.MedicalRecordRepository
import com.example.medicare.databinding.ActivityMedicalHistoryBinding
import com.example.medicare.databinding.DialogPatientMedicalRecordBinding
import com.example.medicare.databinding.ItemPatientMedicalHistoryBinding
import com.example.medicare.model.MedicalRecord
import com.example.medicare.user.PatientScreenChrome
import com.example.medicare.user.reminder.MedicationReminderActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MedicalHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMedicalHistoryBinding
    private val recordsRepository = MedicalRecordRepository()

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        binding = ActivityMedicalHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.medicalHistoryBack.setOnClickListener { finish() }
        PatientScreenChrome.bind(this, "Hồ sơ sức khỏe")
    }

    override fun onResume() {
        super.onResume()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        recordsRepository.getRecordsForPatient(uid) { result ->
            if (isFinishing || isDestroyed) return@getRecordsForPatient
            result.onSuccess { records ->
                binding.medicalHistoryList.removeAllViews()
                binding.medicalHistoryEmpty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
                records.forEach(::addRecord)
                val requestedId = intent.getStringExtra(EXTRA_OPEN_APPOINTMENT_ID).orEmpty()
                if (requestedId.isNotBlank()) {
                    val prescription = intent.getBooleanExtra(EXTRA_SHOW_PRESCRIPTION, false)
                    intent.removeExtra(EXTRA_OPEN_APPOINTMENT_ID)
                    intent.removeExtra(EXTRA_SHOW_PRESCRIPTION)
                    records.firstOrNull { it.appointmentId == requestedId }?.let {
                        binding.root.post { if (!isFinishing && !isDestroyed) openRecord(it, prescription) }
                    }
                }
            }.onFailure {
                binding.medicalHistoryEmpty.text = it.message ?: "Không tải được hồ sơ sức khỏe."
                binding.medicalHistoryEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun addRecord(record: MedicalRecord) {
        val row = ItemPatientMedicalHistoryBinding.inflate(layoutInflater, binding.medicalHistoryList, false)
        row.patientHistoryDiagnosis.text = "🩺 ${record.diagnosis.ifBlank { "Kết quả khám" }}"
        row.patientHistoryDoctor.text = "BS. ${record.doctorName}"
        row.patientHistoryId.text = record.appointmentId
        row.patientHistoryDate.text = runCatching { LocalDate.parse(record.appointmentDate)
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrDefault(record.appointmentDate)
        row.patientHistoryResult.setOnClickListener { openRecord(record, false) }
        row.patientHistoryPrescription.setOnClickListener { openRecord(record, true) }
        row.root.setOnClickListener { openRecord(record, false) }
        binding.medicalHistoryList.addView(row.root)
    }

    private fun openRecord(record: MedicalRecord, prescription: Boolean) {
        recordsRepository.getMedicalRecord(record.appointmentId) { result ->
            if (isFinishing || isDestroyed) return@getMedicalRecord
            result.onSuccess { loaded ->
                if (loaded == null) Toast.makeText(this, "Không tìm thấy kết quả khám.", Toast.LENGTH_SHORT).show()
                else showRecordSheet(loaded, prescription)
            }.onFailure {
                Toast.makeText(this, it.message ?: "Không tải được kết quả khám.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showRecordSheet(record: MedicalRecord, prescription: Boolean) {
        val sheet = BottomSheetDialog(this)
        val content = DialogPatientMedicalRecordBinding.inflate(layoutInflater)
        content.patientRecordDialogTitle.text = if (prescription) "Đơn thuốc đã kê" else "Kết quả khám"
        content.patientRecordDialogClose.setOnClickListener { sheet.dismiss() }
        val rows = content.patientRecordDialogContent
        if (prescription) {
            if (record.prescriptionItems.isEmpty()) {
                rows.addView(TextView(this).apply {
                    text = "Lần khám này chưa có đơn thuốc công bố."
                    textSize = 11f
                    setTextColor(getColor(R.color.manager_muted))
                    setPadding(0, dp(8), 0, dp(8))
                })
                content.patientRecordDialogAction.text = "Đóng"
                content.patientRecordDialogAction.setOnClickListener { sheet.dismiss() }
            } else {
                record.prescriptionItems.forEachIndexed { index, item ->
                    if (index > 0) rows.addView(View(this).apply {
                        setBackgroundResource(R.drawable.patient_record_divider)
                    }, LinearLayout.LayoutParams(-1, dp(1)).apply { topMargin = dp(8); bottomMargin = dp(8) })
                    addDetailRow(rows, "Thuốc", item.name)
                    addDetailRow(rows, "Cách dùng", listOf(item.dosage, item.instructions)
                        .filter(String::isNotBlank).joinToString(" · "))
                    addDetailRow(rows, "Số ngày", "${item.durationDays} ngày", last = true)
                }
                content.patientRecordDialogAction.text = "Tạo nhắc uống thuốc"
                content.patientRecordDialogAction.setOnClickListener {
                    sheet.dismiss()
                    startActivity(Intent(this, MedicationReminderActivity::class.java))
                }
            }
        } else {
            val date = runCatching { LocalDate.parse(record.appointmentDate)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrDefault(record.appointmentDate)
            addDetailRow(rows, "Ngày khám", date)
            addDetailRow(rows, "Chẩn đoán", record.diagnosis)
            addDetailRow(rows, "Hướng điều trị", record.treatmentNotes.ifBlank { "Chưa có thông tin" })
            addDetailRow(rows, "Lời dặn", record.instructions.ifBlank { "Chưa có thông tin" })
            addDetailRow(rows, "Bác sĩ", "BS. ${record.doctorName}", last = true)
            content.patientRecordDialogAction.text = "Xem đơn thuốc"
            content.patientRecordDialogAction.setOnClickListener {
                sheet.dismiss()
                showRecordSheet(record, true)
            }
        }
        sheet.setContentView(content.root)
        sheet.show()
    }

    private fun addDetailRow(container: LinearLayout, label: String, value: String, last: Boolean = false) {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(7), 0, dp(7))
        }
        row.addView(TextView(this).apply {
            text = label
            textSize = 11f
            setTextColor(getColor(R.color.manager_muted))
        }, LinearLayout.LayoutParams(dp(105), -2))
        row.addView(TextView(this).apply {
            text = value
            textSize = 11f
            gravity = Gravity.END
            setTextColor(getColor(R.color.manager_ink))
            typeface = resources.getFont(R.font.inter_bold)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        container.addView(row)
        if (!last) container.addView(View(this).apply {
            setBackgroundResource(R.drawable.patient_record_divider)
        }, LinearLayout.LayoutParams(-1, dp(2)))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()

    companion object {
        const val EXTRA_OPEN_APPOINTMENT_ID = "open_medical_appointment_id"
        const val EXTRA_SHOW_PRESCRIPTION = "open_medical_prescription"
    }
}
