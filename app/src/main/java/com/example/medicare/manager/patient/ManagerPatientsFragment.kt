package com.example.medicare.manager.patient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.example.medicare.R
import com.example.medicare.manager.ManagerDetailSheets
import com.example.medicare.data.appointment.AppointmentRepository
import com.example.medicare.data.patient.PatientManagementRepository
import com.example.medicare.databinding.FragmentManagerPatientsDirectoryBinding
import com.example.medicare.databinding.ItemManagedPatientBinding
import com.example.medicare.model.AccountStatus
import com.example.medicare.model.Patient

/** Patient directory displayed directly in the manager's five-tab mobile shell. */
class ManagerPatientsFragment : Fragment() {
    private var _binding: FragmentManagerPatientsDirectoryBinding? = null
    private val binding get() = _binding!!
    private val repository = PatientManagementRepository()
    private var patients: List<Patient> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentManagerPatientsDirectoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.managerPatientsSearch.doAfterTextChanged { renderPatients(it?.toString().orEmpty()) }
        loadPatients()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) loadPatients()
    }

    private fun loadPatients() {
        repository.getPatients { result ->
            if (!isAdded || _binding == null) return@getPatients
            result.onSuccess {
                patients = it
                renderPatients(binding.managerPatientsSearch.text?.toString().orEmpty())
            }.onFailure {
                binding.managerPatientsEmpty.text = it.message ?: getString(R.string.manager_patients_load_failed)
                binding.managerPatientsEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun renderPatients(query: String) {
        val visible = patients.filter {
            it.fullName.contains(query, true) || it.email.contains(query, true) || it.phone.contains(query, true)
        }
        binding.managerPatientsList.removeAllViews()
        binding.managerPatientsEmpty.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
        visible.forEach { patient ->
            val row = ItemManagedPatientBinding.inflate(layoutInflater, binding.managerPatientsList, false)
            row.managedPatientInitials.text = "👤"
            row.managedPatientName.text = patient.fullName.ifBlank { getString(R.string.manager_patient) }
            row.managedPatientContact.text = patient.phone.ifBlank { patient.email }
            row.managedPatientStatus.text = if (patient.accountStatus == AccountStatus.ACTIVE) "Đang dùng" else "Đã khóa"
            applyStatusStyle(row.managedPatientStatus, patient.accountStatus)
            row.root.setOnClickListener { showActions(patient) }
            binding.managerPatientsList.addView(row.root)
        }
    }

    private fun showActions(patient: Patient) {
        ManagerDetailSheets.patient(requireActivity(), patient) { togglePatientStatus(patient) }
    }

    private fun showAppointmentHistory(patient: Patient) {
        AppointmentRepository().getAppointmentsForPatient(patient.id) { result ->
            if (!isAdded) return@getAppointmentsForPatient
            result.onSuccess { appointments ->
                val detail = if (appointments.isEmpty()) {
                    "Bệnh nhân chưa có lịch khám."
                } else {
                    appointments.joinToString("\n\n") {
                        "${it.appointmentDate} · ${it.startTime} - ${it.endTime}\n${it.doctorName} · ${it.specialtyName}\n${it.status.name}"
                    }
                }
                AlertDialog.Builder(requireContext())
                    .setTitle("Lịch sử: ${patient.fullName}")
                    .setMessage(detail)
                    .setPositiveButton("Đóng", null)
                    .show()
            }.onFailure {
                Toast.makeText(requireContext(), it.message ?: "Không thể tải lịch sử.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun togglePatientStatus(patient: Patient) {
        val active = patient.accountStatus == AccountStatus.ACTIVE
        AlertDialog.Builder(requireContext())
            .setTitle(if (active) R.string.manager_disable_patient_title else R.string.manager_enable_patient_title)
            .setMessage(getString(if (active) R.string.manager_disable_patient_message else R.string.manager_enable_patient_message, patient.fullName))
            .setNegativeButton(R.string.manager_cancel, null)
            .setPositiveButton(if (active) R.string.manager_lock_patient else R.string.manager_unlock_patient) { _, _ ->
                val next = if (active) AccountStatus.DISABLED else AccountStatus.ACTIVE
                repository.updateAccountStatus(patient.id, next) { result ->
                    if (!isAdded) return@updateAccountStatus
                    result.onSuccess { loadPatients() }
                        .onFailure {
                            Toast.makeText(requireContext(), it.message ?: getString(R.string.manager_save_failed), Toast.LENGTH_LONG).show()
                        }
                }
            }
            .show()
    }

    private fun applyStatusStyle(view: TextView, status: AccountStatus) {
        val active = status == AccountStatus.ACTIVE
        view.setBackgroundResource(if (active) R.drawable.manager_status_active_background else R.drawable.manager_status_inactive_background)
        view.setTextColor(ContextCompat.getColor(requireContext(), if (active) R.color.manager_green else R.color.manager_red))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
