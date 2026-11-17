package com.example.medicare.doctor.patient

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.example.medicare.R
import com.example.medicare.data.appointment.AppointmentRepository
import com.example.medicare.data.medical.MedicalRecordRepository
import com.example.medicare.databinding.ActivityDoctorPatientsBinding
import com.example.medicare.model.Appointment
import com.google.firebase.auth.FirebaseAuth

/** Patients are derived from appointments assigned to the signed-in doctor; no directory-wide patient access. */
class DoctorPatientsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDoctorPatientsBinding
    private val appointments = AppointmentRepository()
    private val records = MedicalRecordRepository()
    private var assignedPatients: List<Appointment> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorPatientsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.doctorPatientsBack.setOnClickListener { finish() }
        binding.doctorPatientsSearch.doAfterTextChanged { render() }
    }

    override fun onResume() {
        super.onResume()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return finish()
        appointments.getAppointmentsForDoctor(uid) { result ->
            result.onSuccess { list ->
                assignedPatients = list.groupBy { it.patientId }.values.map { items ->
                    items.maxByOrNull { it.appointmentDate + it.startTime }!!
                }
                render()
            }.onFailure { Toast.makeText(this, it.message ?: getString(R.string.doctor_patients_load_failed), Toast.LENGTH_LONG).show() }
        }
    }

    private fun render() {
        val query = binding.doctorPatientsSearch.text.toString().trim()
        val visible = assignedPatients.filter { it.patientName.contains(query, true) || it.reason.contains(query, true) }
        binding.doctorPatientsList.removeAllViews()
        binding.doctorPatientsEmpty.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
        visible.forEach { appointment ->
            val row = layoutInflater.inflate(R.layout.item_doctor_patient, binding.doctorPatientsList, false)
            row.findViewById<TextView>(R.id.doctorPatientName).text = appointment.patientName.ifBlank { getString(R.string.doctor_patient) }
            row.findViewById<TextView>(R.id.doctorPatientInfo).text = getString(R.string.doctor_patient_last_appointment, appointment.appointmentDate, appointment.reason.ifBlank { getString(R.string.doctor_no_reason) })
            row.setOnClickListener { showHistory(appointment) }
            binding.doctorPatientsList.addView(row)
        }
    }

    private fun showHistory(appointment: Appointment) {
        records.getRecordsForPatient(appointment.patientId) { result ->
            result.onSuccess { items ->
                val message = if (items.isEmpty()) getString(R.string.doctor_no_medical_history) else items.joinToString("\n\n") {
                    "${it.appointmentDate} · ${it.specialtyName}\n${it.diagnosis.ifBlank { getString(R.string.doctor_no_diagnosis) }}"
                }
                AlertDialog.Builder(this)
                    .setTitle(appointment.patientName.ifBlank { getString(R.string.doctor_patient) })
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }.onFailure { Toast.makeText(this, it.message ?: getString(R.string.doctor_history_load_failed), Toast.LENGTH_LONG).show() }
        }
    }
}
