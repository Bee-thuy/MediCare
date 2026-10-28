package com.example.medicare.manager.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import com.example.medicare.R
import com.example.medicare.databinding.FragmentManagerPatientsBinding
import com.example.medicare.databinding.ItemManagerAppointmentBinding
import com.example.medicare.manager.user.DetailPatientActivity

class ManagerPatientsFragment : Fragment() {
    private var _binding: FragmentManagerPatientsBinding? = null
    private val binding get() = _binding!!
    private var selectedStatus = AppointmentStatus.ALL
    private var showConflictsOnly = false

    private val mockAppointments = mutableListOf(
        Appointment("booking_01", "Nguyễn Thị Mai", "Nữ · 26 tuổi", "08:00 - 08:30   26/05/2025", "BS. Nguyễn Văn A", "Khám tổng quát", AppointmentStatus.PENDING),
        Appointment("booking_02", "Trần Văn Nam", "Nam · 35 tuổi", "08:15 - 08:45   26/05/2025", "BS. Nguyễn Văn A", "Khám tim mạch", AppointmentStatus.PENDING),
        Appointment("booking_03", "Lê Thị Hương", "Nữ · 42 tuổi", "08:20 - 08:50   26/05/2025", "BS. Nguyễn Văn A", "Khám da liễu", AppointmentStatus.PENDING),
        Appointment("booking_04", "Phạm Minh Đức", "Nam · 50 tuổi", "13:00 - 13:30   26/05/2025", "BS. Nguyễn Thị Mai", "Khám tiêu hóa", AppointmentStatus.PENDING),
        Appointment("booking_05", "Vũ Thu Trang", "Nữ · 31 tuổi", "14:00 - 14:30   26/05/2025", "BS. Trần Thị B", "Khám nội tổng quát", AppointmentStatus.CONFIRMED),
        Appointment("booking_06", "Đỗ Quang Huy", "Nam · 27 tuổi", "15:00 - 15:30   26/05/2025", "BS. Hoàng Nam", "Khám thần kinh", AppointmentStatus.CONFIRMED),
    )

    private val appointmentDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val resultData = result.data ?: return@registerForActivityResult
        val appointmentId = resultData.getStringExtra(DetailPatientActivity.EXTRA_APPOINTMENT_ID) ?: return@registerForActivityResult
        val status = resultData.getStringExtra(DetailPatientActivity.EXTRA_STATUS) ?: return@registerForActivityResult
        val index = mockAppointments.indexOfFirst { it.id == appointmentId }
        if (index >= 0) {
            mockAppointments[index] = mockAppointments[index].copy(status = AppointmentStatus.fromValue(status))
            renderAppointments()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentManagerPatientsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.appointmentAllTab.setOnClickListener { selectStatus(AppointmentStatus.ALL) }
        binding.appointmentPendingTab.setOnClickListener { selectStatus(AppointmentStatus.PENDING) }
        binding.appointmentConfirmedTab.setOnClickListener { selectStatus(AppointmentStatus.CONFIRMED) }
        binding.appointmentRejectedTab.setOnClickListener { selectStatus(AppointmentStatus.REJECTED) }
        binding.appointmentTimeFilter.setOnClickListener { showMockFilterMessage() }
        binding.appointmentDoctorFilter.setOnClickListener { showMockFilterMessage() }
        binding.appointmentConflictFilter.setOnClickListener {
            showConflictsOnly = !showConflictsOnly
            renderAppointments()
        }
        selectStatus(AppointmentStatus.ALL)
    }

    private fun selectStatus(status: AppointmentStatus) {
        selectedStatus = status
        val tabs = mapOf(
            AppointmentStatus.ALL to binding.appointmentAllTab,
            AppointmentStatus.PENDING to binding.appointmentPendingTab,
            AppointmentStatus.CONFIRMED to binding.appointmentConfirmedTab,
            AppointmentStatus.REJECTED to binding.appointmentRejectedTab,
        )
        tabs.forEach { (tabStatus, tab) ->
            val selected = tabStatus == status
            tab.isSelected = selected
            tab.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (selected) R.color.white else R.color.manager_ink,
                ),
            )
        }
        renderAppointments()
    }

    private fun renderAppointments() {
        val conflictIds = conflictingAppointmentIds()
        val visibleAppointments = mockAppointments.filter {
            (selectedStatus == AppointmentStatus.ALL || it.status == selectedStatus) &&
                (!showConflictsOnly || it.id in conflictIds)
        }
        binding.appointmentConflictFilter.isSelected = showConflictsOnly
        binding.appointmentConflictFilter.text = getString(R.string.manager_needs_attention, conflictIds.size)
        binding.appointmentsList.removeAllViews()
        binding.appointmentEmpty.visibility = if (visibleAppointments.isEmpty()) View.VISIBLE else View.GONE
        visibleAppointments.forEach { appointment ->
            val row = ItemManagerAppointmentBinding.inflate(layoutInflater, binding.appointmentsList, false)
            row.appointmentInitials.text = appointment.initials
            row.appointmentPatientName.text = appointment.name
            row.appointmentPatientInfo.text = appointment.patientInfo
            row.appointmentTime.text = "◷ ${appointment.time}"
            row.appointmentDepartment.text = "${appointment.doctor} · ${appointment.department}"
            if (appointment.id in conflictIds) {
                row.appointmentStatus.text = getString(R.string.manager_time_conflict)
                applyConflictStyle(row.appointmentStatus)
            } else {
                row.appointmentStatus.text = when (appointment.status) {
                    AppointmentStatus.PENDING -> getString(R.string.manager_pending)
                    AppointmentStatus.CONFIRMED -> getString(R.string.manager_confirmed)
                    AppointmentStatus.REJECTED -> getString(R.string.manager_reject)
                    AppointmentStatus.ALL -> ""
                }
                applyStatusStyle(row.appointmentStatus, appointment.status)
            }
            row.root.setOnClickListener {
                openAppointmentDetail(appointment)
            }
            binding.appointmentsList.addView(row.root)
        }
    }

    private fun applyStatusStyle(statusView: TextView, status: AppointmentStatus) {
        val pending = status == AppointmentStatus.PENDING
        val rejected = status == AppointmentStatus.REJECTED
        statusView.setBackgroundResource(
            when {
                pending -> R.drawable.manager_appointment_pending_background
                rejected -> R.drawable.manager_status_inactive_background
                else -> R.drawable.manager_status_active_background
            },
        )
        statusView.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (pending) R.color.manager_orange else if (rejected) R.color.manager_red else R.color.manager_green,
            ),
        )
    }

    private fun applyConflictStyle(statusView: TextView) {
        statusView.setBackgroundResource(R.drawable.manager_status_inactive_background)
        statusView.setTextColor(ContextCompat.getColor(requireContext(), R.color.manager_red))
    }

    private fun conflictingAppointmentIds(): Set<String> {
        val confirmedAppointments = mockAppointments.filter { it.status == AppointmentStatus.CONFIRMED }
        return mockAppointments
            .filter { it.status == AppointmentStatus.PENDING }
            .filter { pending ->
                confirmedAppointments.any { confirmed ->
                    pending.doctor == confirmed.doctor &&
                        pending.date == confirmed.date &&
                        pending.overlaps(confirmed)
                }
            }
            .mapTo(mutableSetOf()) { it.id }
    }

    private fun showMockFilterMessage() {
        Toast.makeText(requireContext(), R.string.manager_mock_filter_message, Toast.LENGTH_SHORT).show()
    }

    private fun openAppointmentDetail(appointment: Appointment) {
        val dateTime = appointment.time.split("   ", limit = 2)
        appointmentDetailLauncher.launch(
            Intent(requireContext(), DetailPatientActivity::class.java)
                .putExtra(DetailPatientActivity.EXTRA_APPOINTMENT_ID, appointment.id)
                .putExtra(DetailPatientActivity.EXTRA_PATIENT_NAME, appointment.name)
                .putExtra(DetailPatientActivity.EXTRA_PATIENT_INFO, appointment.patientInfo)
                .putExtra(DetailPatientActivity.EXTRA_TIME, dateTime.firstOrNull().orEmpty())
                .putExtra(DetailPatientActivity.EXTRA_DATE, dateTime.getOrNull(1)?.plus(" (Thứ 2)").orEmpty())
                .putExtra(DetailPatientActivity.EXTRA_DOCTOR, appointment.doctor)
                .putExtra(DetailPatientActivity.EXTRA_SPECIALTY, appointment.department.removePrefix("Khám "))
                .putExtra(DetailPatientActivity.EXTRA_REASON, appointment.department)
                .putExtra(DetailPatientActivity.EXTRA_STATUS, appointment.status.name)
                .putExtra(DetailPatientActivity.EXTRA_HAS_TIME_CONFLICT, appointment.id in conflictingAppointmentIds()),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class Appointment(
        val id: String,
        val name: String,
        val patientInfo: String,
        val time: String,
        val doctor: String,
        val department: String,
        val status: AppointmentStatus,
    ) {
        val initials: String
            get() = name.split(" ").filter { it.isNotBlank() }.takeLast(2).joinToString("") { it.first().toString() }

        val date: String
            get() = time.substringAfter("   ")

        private val timeRange: IntRange
            get() {
                val (start, end) = time.substringBefore("   ").split(" - ")
                return minutes(start)..minutes(end)
            }

        fun overlaps(other: Appointment): Boolean {
            return timeRange.first < other.timeRange.last && other.timeRange.first < timeRange.last
        }

        private fun minutes(value: String): Int {
            val (hours, minutes) = value.split(":").map(String::toInt)
            return hours * 60 + minutes
        }
    }

    private enum class AppointmentStatus {
        ALL,
        PENDING,
        CONFIRMED,
        REJECTED;

        companion object {
            fun fromValue(value: String): AppointmentStatus = entries.firstOrNull { it.name == value } ?: PENDING
        }
    }
}
