package com.example.medicare.user.medical

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.data.medical.MedicalRecordRepository
import com.example.medicare.databinding.ActivityMedicalRecordBinding
import com.example.medicare.user.PatientScreenChrome

class MedicalRecordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMedicalRecordBinding
    private val medicalRecordRepository = MedicalRecordRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicalRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        PatientScreenChrome.bind(this, "Hồ sơ sức khỏe")

        binding.medicalRecordBack.setOnClickListener { finish() }
        binding.medicalRecordDoctor.text = intent.getStringExtra(EXTRA_DOCTOR_NAME).orEmpty()
        binding.medicalRecordSpecialty.text = intent.getStringExtra(EXTRA_SPECIALTY).orEmpty()
        binding.medicalRecordDate.text = intent.getStringExtra(EXTRA_DATE).orEmpty()
        loadRecord(intent.getStringExtra(EXTRA_APPOINTMENT_ID).orEmpty())
    }

    private fun loadRecord(appointmentId: String) {
        if (appointmentId.isBlank()) {
            showError(getString(R.string.patient_record_load_failed))
            return
        }
        medicalRecordRepository.getMedicalRecord(appointmentId) { result ->
            result.onSuccess { record ->
                if (record == null) {
                    showError(getString(R.string.patient_record_not_found))
                    return@onSuccess
                }
                binding.medicalRecordSymptoms.text = record.symptoms
                binding.medicalRecordDiagnosis.text = record.diagnosis
                binding.medicalRecordTreatment.text = record.treatmentNotes.ifBlank { getString(R.string.patient_no_information) }
                binding.medicalRecordInstructions.text = record.instructions.ifBlank { getString(R.string.patient_no_information) }
                if (record.prescriptionItems.isEmpty()) {
                    binding.medicalRecordPrescriptionSection.visibility = View.GONE
                    if (intent.getBooleanExtra(EXTRA_SHOW_PRESCRIPTION, false))
                        Toast.makeText(this, "Lượt khám này chưa có đơn thuốc.", Toast.LENGTH_SHORT).show()
                } else {
                    binding.medicalRecordPrescription.text = record.prescriptionItems.joinToString("\n\n") { item ->
                        getString(
                            R.string.patient_prescription_item,
                            item.name,
                            item.dosage,
                            item.instructions,
                            item.durationDays,
                        )
                    }
                    if (intent.getBooleanExtra(EXTRA_SHOW_PRESCRIPTION, false))
                        binding.medicalRecordScroll.post {
                            binding.medicalRecordScroll.smoothScrollTo(
                                0, binding.medicalRecordContent.top + binding.medicalRecordPrescriptionSection.top)
                        }
                }
            }.onFailure { error -> showError(error.message ?: getString(R.string.patient_record_load_failed)) }
        }
    }

    private fun showError(message: String) {
        binding.medicalRecordContent.visibility = View.GONE
        binding.medicalRecordEmpty.visibility = View.VISIBLE
        binding.medicalRecordEmpty.text = message
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_APPOINTMENT_ID = "appointment_id"
        const val EXTRA_DOCTOR_NAME = "doctor_name"
        const val EXTRA_SPECIALTY = "specialty"
        const val EXTRA_DATE = "appointment_date"
        const val EXTRA_SHOW_PRESCRIPTION = "show_prescription"
    }
}
