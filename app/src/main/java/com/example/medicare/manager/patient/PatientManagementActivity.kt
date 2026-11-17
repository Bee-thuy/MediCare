package com.example.medicare.manager.patient

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.example.medicare.R
import com.example.medicare.data.patient.PatientManagementRepository
import com.example.medicare.databinding.ActivityPatientManagementBinding
import com.example.medicare.databinding.ItemManagedPatientBinding
import com.example.medicare.model.AccountStatus
import com.example.medicare.model.Patient
import com.example.medicare.data.appointment.AppointmentRepository

class PatientManagementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPatientManagementBinding
    private val repository = PatientManagementRepository()
    private var patients: List<Patient> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.patientManagementBack.setOnClickListener { finish() }
        binding.patientManagementSearch.doAfterTextChanged { renderPatients(it?.toString().orEmpty()) }
        loadPatients()
    }

    override fun onResume() {
        super.onResume()
        loadPatients()
    }

    private fun loadPatients() {
        repository.getPatients { result ->
            result.onSuccess { loadedPatients ->
                patients = loadedPatients
                renderPatients(binding.patientManagementSearch.text?.toString().orEmpty())
            }.onFailure { error ->
                binding.patientManagementEmpty.text = error.message ?: getString(R.string.manager_patients_load_failed)
                binding.patientManagementEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun renderPatients(query: String) {
        val visiblePatients = patients.filter { patient ->
            patient.fullName.contains(query, true) ||
                patient.email.contains(query, true) ||
                patient.phone.contains(query, true)
        }
        binding.patientManagementList.removeAllViews()
        binding.patientManagementEmpty.visibility = if (visiblePatients.isEmpty()) View.VISIBLE else View.GONE
        visiblePatients.forEach { patient ->
            val row = ItemManagedPatientBinding.inflate(layoutInflater, binding.patientManagementList, false)
            row.managedPatientInitials.text = initialsOf(patient.fullName)
            row.managedPatientName.text = patient.fullName.ifBlank { getString(R.string.manager_patient) }
            row.managedPatientContact.text = listOf(patient.email, patient.phone).filter { it.isNotBlank() }.joinToString(" · ")
            row.managedPatientStatus.text = if (patient.accountStatus == AccountStatus.ACTIVE) {
                getString(R.string.manager_active)
            } else {
                getString(R.string.manager_inactive)
            }
            applyStatusStyle(row.managedPatientStatus, patient.accountStatus)
            row.root.setOnClickListener { showPatientActions(patient) }
            binding.patientManagementList.addView(row.root)
        }
    }

    private fun showPatientActions(patient: Patient) {
        AlertDialog.Builder(this).setTitle(patient.fullName).setItems(arrayOf("Xem lịch sử đặt khám", "Khóa / mở khóa tài khoản")) { _, index ->
            if (index == 0) showAppointmentHistory(patient) else showStatusDialog(patient)
        }.show()
    }

    private fun showAppointmentHistory(patient: Patient) {
        AppointmentRepository().getAppointmentsForPatient(patient.id) { result ->
            result.onSuccess { appointments ->
                val content = if (appointments.isEmpty()) "Bệnh nhân chưa có lịch khám." else appointments.joinToString("\n\n") {
                    "${it.appointmentDate} · ${it.startTime} - ${it.endTime}\n${it.doctorName} · ${it.specialtyName}\n${it.status.name}${if (it.reason.isBlank()) "" else "\nLý do: ${it.reason}"}"
                }
                AlertDialog.Builder(this).setTitle("Lịch sử: ${patient.fullName}").setMessage(content).setPositiveButton("Đóng", null).show()
            }.onFailure { Toast.makeText(this, it.message ?: "Không thể tải lịch sử.", Toast.LENGTH_LONG).show() }
        }
    }

    private fun showStatusDialog(patient: Patient) {
        val active = patient.accountStatus == AccountStatus.ACTIVE
        AlertDialog.Builder(this)
            .setTitle(if (active) R.string.manager_disable_patient_title else R.string.manager_enable_patient_title)
            .setMessage(
                getString(
                    if (active) R.string.manager_disable_patient_message else R.string.manager_enable_patient_message,
                    patient.fullName,
                ),
            )
            .setNegativeButton(R.string.manager_cancel, null)
            .setPositiveButton(if (active) R.string.manager_lock_patient else R.string.manager_unlock_patient) { _, _ ->
                val nextStatus = if (active) AccountStatus.DISABLED else AccountStatus.ACTIVE
                repository.updateAccountStatus(patient.id, nextStatus) { result ->
                    result.onSuccess {
                        Toast.makeText(this, R.string.manager_patient_status_updated, Toast.LENGTH_SHORT).show()
                        loadPatients()
                    }.onFailure { error ->
                        Toast.makeText(this, error.message ?: getString(R.string.manager_save_failed), Toast.LENGTH_LONG).show()
                    }
                }
            }
            .show()
    }

    private fun applyStatusStyle(view: TextView, status: AccountStatus) {
        val active = status == AccountStatus.ACTIVE
        view.setBackgroundResource(if (active) R.drawable.manager_status_active_background else R.drawable.manager_status_inactive_background)
        view.setTextColor(ContextCompat.getColor(this, if (active) R.color.manager_green else R.color.manager_red))
    }

    private fun initialsOf(name: String): String = name.split(" ")
        .filter { it.isNotBlank() }
        .takeLast(2)
        .joinToString("") { it.first().toString() }
}
