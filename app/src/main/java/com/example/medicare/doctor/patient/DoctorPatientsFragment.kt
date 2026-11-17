package com.example.medicare.doctor.patient

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.example.medicare.R
import com.example.medicare.data.appointment.AppointmentRepository
import com.example.medicare.data.medical.MedicalRecordRepository
import com.example.medicare.databinding.ActivityDoctorPatientsBinding
import com.example.medicare.model.Appointment
import com.google.firebase.auth.FirebaseAuth

class DoctorPatientsFragment : Fragment() {
    private var _binding: ActivityDoctorPatientsBinding? = null
    private val binding get() = _binding!!
    private val appointments = AppointmentRepository()
    private val records = MedicalRecordRepository()
    private var patients: List<Appointment> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = ActivityDoctorPatientsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        binding.doctorPatientsBack.visibility = View.GONE
        binding.doctorPatientsTitle.visibility = View.VISIBLE
        binding.doctorPatientsSearch.doAfterTextChanged { render() }
    }

    override fun onResume() { super.onResume(); load() }

    private fun load() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        appointments.getAppointmentsForDoctor(uid) { result ->
            if (!isAdded) return@getAppointmentsForDoctor
            result.onSuccess { list ->
                patients = list.groupBy { it.patientId }.values.mapNotNull { entries -> entries.maxByOrNull { it.appointmentDate + it.startTime } }
                render()
            }.onFailure { Toast.makeText(requireContext(), it.message ?: getString(R.string.doctor_patients_load_failed), Toast.LENGTH_LONG).show() }
        }
    }

    private fun render() {
        val query = binding.doctorPatientsSearch.text.toString()
        val visible = patients.filter { it.patientName.contains(query, true) || it.reason.contains(query, true) }
        binding.doctorPatientsList.removeAllViews()
        binding.doctorPatientsEmpty.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
        visible.forEach { patient ->
            val row = layoutInflater.inflate(R.layout.item_doctor_patient, binding.doctorPatientsList, false)
            row.findViewById<TextView>(R.id.doctorPatientName).text = patient.patientName.ifBlank { getString(R.string.doctor_patient) }
            row.findViewById<TextView>(R.id.doctorPatientInfo).text = getString(R.string.doctor_patient_last_appointment, patient.appointmentDate, patient.reason.ifBlank { getString(R.string.doctor_no_reason) })
            row.setOnClickListener { showHistory(patient) }
            binding.doctorPatientsList.addView(row)
        }
    }

    private fun showHistory(patient: Appointment) {
        records.getRecordsForPatient(patient.patientId) { result ->
            if (!isAdded) return@getRecordsForPatient
            result.onSuccess { history ->
                val content = if (history.isEmpty()) getString(R.string.doctor_no_medical_history) else history.joinToString("\n\n") { "${it.appointmentDate} · ${it.specialtyName}\n${it.diagnosis.ifBlank { getString(R.string.doctor_no_diagnosis) }}" }
                AlertDialog.Builder(requireContext()).setTitle(patient.patientName).setMessage(content).setPositiveButton(android.R.string.ok, null).show()
            }.onFailure { Toast.makeText(requireContext(), it.message ?: getString(R.string.doctor_history_load_failed), Toast.LENGTH_LONG).show() }
        }
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
