package com.example.medicare.doctor.medical

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.data.medical.MedicalRecordRepository
import com.example.medicare.databinding.ActivityCompleteAppointmentBinding
import com.example.medicare.databinding.ItemPrescriptionInputBinding
import com.example.medicare.model.Appointment
import com.example.medicare.model.AppointmentStatus
import com.example.medicare.model.PrescriptionItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CompleteAppointmentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCompleteAppointmentBinding
    private val medicalRecordRepository = MedicalRecordRepository()
    private lateinit var appointment: Appointment
    private val prescriptionBindings = mutableListOf<ItemPrescriptionInputBinding>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompleteAppointmentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * .88f).toInt())
        window.setGravity(Gravity.BOTTOM)

        appointment = appointmentFromIntent()
        bindAppointment()
        binding.completeAppointmentBack.setOnClickListener { finish() }
        binding.completeAppointmentSave.setOnClickListener { saveResult() }
        binding.completeAppointmentDraft.setOnClickListener { saveDraft() }
        restoreDraft()
        binding.completeAppointmentAddMedication.setOnClickListener { addMedication() }
        if (appointment.status == AppointmentStatus.COMPLETED) showSavedRecord()
        else if (appointment.status != AppointmentStatus.IN_PROGRESS) setInputsEnabled(false)
    }

    private fun bindAppointment() {
        binding.completeAppointmentPatientName.text = appointment.patientName.ifBlank { getString(R.string.doctor_patient) }
        val date = runCatching { LocalDate.parse(appointment.appointmentDate)
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrDefault(appointment.appointmentDate)
        binding.completeAppointmentInfo.text = "$date · ${appointment.startTime}"
        binding.completeAppointmentReason.text = appointment.reason.ifBlank { getString(R.string.doctor_no_reason) }
        val editable = appointment.status == AppointmentStatus.IN_PROGRESS
        binding.completeAppointmentSave.visibility = if (editable) View.VISIBLE else View.GONE
        binding.completeAppointmentDraft.visibility = if (editable) View.VISIBLE else View.GONE
    }

    private fun showSavedRecord() {
        setInputsEnabled(false)
        binding.completeAppointmentTitle.setText(R.string.doctor_record_title)
        medicalRecordRepository.getMedicalRecord(appointment.id) { result ->
            if (isFinishing || isDestroyed) return@getMedicalRecord
            result.onSuccess { record ->
                binding.completeAppointmentPublished.visibility = if (record != null) View.VISIBLE else View.GONE
                binding.completeAppointmentSymptoms.setText(record?.symptoms.orEmpty())
                binding.completeAppointmentDiagnosis.setText(record?.diagnosis.orEmpty())
                binding.completeAppointmentTreatment.setText(record?.treatmentNotes.orEmpty())
                binding.completeAppointmentInstructions.setText(record?.instructions.orEmpty())
                record?.prescriptionItems?.forEach(::addMedication)
                setInputsEnabled(false)
            }.onFailure { error ->
                Toast.makeText(this, error.message ?: getString(R.string.doctor_record_load_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveResult() {
        val symptoms = binding.completeAppointmentSymptoms.text.toString().trim()
        val diagnosis = binding.completeAppointmentDiagnosis.text.toString().trim()
        val treatment = binding.completeAppointmentTreatment.text.toString().trim()
        val instructions = binding.completeAppointmentInstructions.text.toString().trim()
        val prescriptionItems = prescriptionItems() ?: return
        binding.completeAppointmentSymptoms.error = if (symptoms.isBlank()) getString(R.string.manager_required_field) else null
        binding.completeAppointmentDiagnosis.error = if (diagnosis.isBlank()) getString(R.string.manager_required_field) else null
        if (symptoms.isBlank() || diagnosis.isBlank()) return

        binding.completeAppointmentSave.isEnabled = false
        medicalRecordRepository.completeAppointment(
            appointment = appointment,
            symptoms = symptoms,
            diagnosis = diagnosis,
            treatmentNotes = treatment,
            instructions = instructions,
            prescriptionItems = prescriptionItems,
        ) { result ->
            binding.completeAppointmentSave.isEnabled = true
            result.onSuccess {
                Toast.makeText(this, R.string.doctor_record_saved, Toast.LENGTH_LONG).show()
                getPreferences(MODE_PRIVATE).edit().remove("draft_${appointment.id}").apply()
                setResult(RESULT_OK)
                finish()
            }.onFailure { error ->
                Toast.makeText(this, error.message ?: getString(R.string.doctor_record_save_failed), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveDraft() {
        val fields = listOf(binding.completeAppointmentSymptoms, binding.completeAppointmentDiagnosis,
            binding.completeAppointmentTreatment, binding.completeAppointmentInstructions)
        val values = org.json.JSONArray()
        fields.forEach { values.put(it.text.toString()) }
        val medications = org.json.JSONArray()
        prescriptionBindings.forEach { item ->
            medications.put(org.json.JSONArray().apply {
                put(item.prescriptionName.text.toString())
                put(item.prescriptionDosage.text.toString())
                put(item.prescriptionInstructions.text.toString())
                put(item.prescriptionDurationDays.text.toString())
            })
        }
        values.put(medications)
        getPreferences(MODE_PRIVATE).edit().putString("draft_${appointment.id}", values.toString()).apply()
        Toast.makeText(this, "Đã lưu nháp trên thiết bị này.", Toast.LENGTH_SHORT).show()
    }

    private fun restoreDraft() {
        if (appointment.status != AppointmentStatus.IN_PROGRESS) return
        val stored = getPreferences(MODE_PRIVATE).getString("draft_${appointment.id}", null) ?: return
        runCatching { org.json.JSONArray(stored) }.onSuccess { values ->
            listOf(binding.completeAppointmentSymptoms, binding.completeAppointmentDiagnosis,
                binding.completeAppointmentTreatment, binding.completeAppointmentInstructions)
                .forEachIndexed { index, field -> field.setText(values.optString(index)) }
            values.optJSONArray(4)?.let { medications ->
                repeat(medications.length()) { index ->
                    val row = medications.optJSONArray(index) ?: return@repeat
                    addMedication(PrescriptionItem(row.optString(0), row.optString(1),
                        row.optString(2), row.optString(3)))
                }
            }
        }
    }

    private fun setInputsEnabled(enabled: Boolean) {
        binding.completeAppointmentSymptoms.isEnabled = enabled
        binding.completeAppointmentDiagnosis.isEnabled = enabled
        binding.completeAppointmentTreatment.isEnabled = enabled
        binding.completeAppointmentInstructions.isEnabled = enabled
        binding.completeAppointmentAddMedication.isEnabled = enabled
        prescriptionBindings.forEach { item ->
            item.prescriptionName.isEnabled = enabled
            item.prescriptionDosage.isEnabled = enabled
            item.prescriptionInstructions.isEnabled = enabled
            item.prescriptionDurationDays.isEnabled = enabled
            item.prescriptionRemove.visibility = if (enabled) View.VISIBLE else View.GONE
        }
    }

    private fun addMedication(item: PrescriptionItem? = null) {
        val itemBinding = ItemPrescriptionInputBinding.inflate(layoutInflater, binding.completeAppointmentItems, false)
        itemBinding.prescriptionName.setText(item?.name.orEmpty())
        itemBinding.prescriptionDosage.setText(item?.dosage.orEmpty())
        itemBinding.prescriptionInstructions.setText(item?.instructions.orEmpty())
        itemBinding.prescriptionDurationDays.setText(item?.durationDays.orEmpty())
        itemBinding.prescriptionRemove.setOnClickListener {
            prescriptionBindings.remove(itemBinding)
            binding.completeAppointmentItems.removeView(itemBinding.root)
        }
        prescriptionBindings += itemBinding
        binding.completeAppointmentItems.addView(itemBinding.root)
    }

    private fun prescriptionItems(): List<PrescriptionItem>? {
        return prescriptionBindings.mapNotNull { item ->
            val name = item.prescriptionName.text.toString().trim()
            val dosage = item.prescriptionDosage.text.toString().trim()
            val instructions = item.prescriptionInstructions.text.toString().trim()
            val durationDays = item.prescriptionDurationDays.text.toString().trim()
            if (name.isBlank() && dosage.isBlank() && instructions.isBlank() && durationDays.isBlank()) {
                null
            } else {
                item.prescriptionName.error = if (name.isBlank()) getString(R.string.manager_required_field) else null
                item.prescriptionDosage.error = if (dosage.isBlank()) getString(R.string.manager_required_field) else null
                item.prescriptionInstructions.error = if (instructions.isBlank()) getString(R.string.manager_required_field) else null
                item.prescriptionDurationDays.error = if (durationDays.isBlank()) getString(R.string.manager_required_field) else null
                if (name.isBlank() || dosage.isBlank() || instructions.isBlank() || durationDays.isBlank()) {
                    return null
                }
                PrescriptionItem(name, dosage, instructions, durationDays)
            }
        }
    }

    private fun appointmentFromIntent(): Appointment = Appointment(
        id = intent.getStringExtra(EXTRA_ID).orEmpty(),
        patientId = intent.getStringExtra(EXTRA_PATIENT_ID).orEmpty(),
        patientName = intent.getStringExtra(EXTRA_PATIENT_NAME).orEmpty(),
        doctorId = intent.getStringExtra(EXTRA_DOCTOR_ID).orEmpty(),
        doctorName = intent.getStringExtra(EXTRA_DOCTOR_NAME).orEmpty(),
        specialtyName = intent.getStringExtra(EXTRA_SPECIALTY).orEmpty(),
        scheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID).orEmpty(),
        appointmentDate = intent.getStringExtra(EXTRA_DATE).orEmpty(),
        startTime = intent.getStringExtra(EXTRA_START_TIME).orEmpty(),
        endTime = intent.getStringExtra(EXTRA_END_TIME).orEmpty(),
        reason = intent.getStringExtra(EXTRA_REASON).orEmpty(),
        status = AppointmentStatus.entries.firstOrNull {
            it.name == intent.getStringExtra(EXTRA_STATUS)
        } ?: AppointmentStatus.CONFIRMED,
    )

    companion object {
        const val EXTRA_ID = "appointment_id"
        const val EXTRA_PATIENT_ID = "patient_id"
        const val EXTRA_PATIENT_NAME = "patient_name"
        const val EXTRA_DOCTOR_ID = "doctor_id"
        const val EXTRA_DOCTOR_NAME = "doctor_name"
        const val EXTRA_SPECIALTY = "specialty_name"
        const val EXTRA_SCHEDULE_ID = "schedule_id"
        const val EXTRA_DATE = "appointment_date"
        const val EXTRA_START_TIME = "start_time"
        const val EXTRA_END_TIME = "end_time"
        const val EXTRA_REASON = "reason"
        const val EXTRA_STATUS = "status"
    }
}
