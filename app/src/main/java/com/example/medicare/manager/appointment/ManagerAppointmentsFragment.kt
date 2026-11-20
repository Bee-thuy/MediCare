package com.example.medicare.manager.appointment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.DialogInterface
import android.graphics.drawable.GradientDrawable
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import com.example.medicare.R
import com.example.medicare.manager.ManagerDetailSheets
import com.example.medicare.data.appointment.AppointmentRepository
import com.example.medicare.databinding.FragmentManagerPatientsBinding
import com.example.medicare.databinding.ItemManagerAppointmentBinding
import com.example.medicare.model.Appointment
import com.example.medicare.model.AppointmentStatus
import java.time.LocalDate
import java.util.Calendar

class ManagerAppointmentsFragment : Fragment() {
    private var _binding: FragmentManagerPatientsBinding? = null
    private val binding get() = _binding!!
    private val appointmentRepository = AppointmentRepository()
    private val appointments = mutableListOf<Appointment>()
    private var selectedStatus: AppointmentStatus? = null
    private var selectedDate: String? = null
    private var selectedDoctorId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentManagerPatientsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.appointmentAllTab.setOnClickListener { selectStatus(null) }
        binding.appointmentPendingTab.setOnClickListener { selectStatus(AppointmentStatus.PENDING) }
        binding.appointmentConfirmedTab.setOnClickListener { selectStatus(AppointmentStatus.CONFIRMED) }
        binding.appointmentRejectedTab.setOnClickListener { selectStatus(AppointmentStatus.REJECTED) }
        binding.appointmentTimeFilter.setOnClickListener { chooseDate() }
        binding.appointmentDoctorFilter.setOnClickListener { chooseDoctor() }
        binding.appointmentFiltersToggle.setOnClickListener {
            val expanded = binding.appointmentFiltersPanel.visibility != View.VISIBLE
            binding.appointmentFiltersPanel.visibility = if (expanded) View.VISIBLE else View.GONE
            binding.appointmentFiltersToggle.text = if (expanded) "Ẩn bộ lọc  ▴" else "Lọc ngày, bác sĩ  ▾"
        }
        binding.appointmentConflictFilter.setOnClickListener { showConflicts() }
        binding.appointmentConflictWarning.setOnClickListener { showConflicts() }
        selectStatus(null)
        loadAppointments()
    }

    override fun onResume() {
        super.onResume()
        loadAppointments()
    }

    private fun selectStatus(status: AppointmentStatus?) {
        selectedStatus = status
        val tabs = mapOf(
            null to binding.appointmentAllTab,
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

    private fun loadAppointments() {
        appointmentRepository.getAppointments { result ->
            if (!isAdded) return@getAppointments
            result.onSuccess { loadedAppointments ->
                appointments.clear()
                appointments.addAll(loadedAppointments)
                renderAppointments()
            }.onFailure { error ->
                Toast.makeText(requireContext(), error.message ?: "Không thể tải lịch khám.", Toast.LENGTH_SHORT).show()
                renderAppointments()
            }
        }
    }

    private fun renderAppointments() {
        val conflicts = conflictingAppointments()
        val conflictIds = conflicts.flatten().map { it.id }.toSet()
        val visibleAppointments = appointments.filter {
            (selectedStatus == null || it.status == selectedStatus) &&
                (selectedDate == null || it.appointmentDate == selectedDate) &&
                (selectedDoctorId == null || it.doctorId == selectedDoctorId)
        }
        binding.appointmentCount.text = "${visibleAppointments.size} lịch"
        binding.appointmentConflictWarning.visibility = if (conflicts.isEmpty()) View.GONE else View.VISIBLE
        binding.appointmentConflictWarning.text = "⚠️ Có ${conflicts.size} khung giờ có nhiều lịch đặt. Chạm để kiểm tra."
        binding.appointmentsList.removeAllViews()
        binding.appointmentEmpty.visibility = if (visibleAppointments.isEmpty()) View.VISIBLE else View.GONE
        visibleAppointments.forEach { appointment ->
            val row = ItemManagerAppointmentBinding.inflate(layoutInflater, binding.appointmentsList, false)
            row.appointmentInitials.text = "👤"
            row.appointmentPatientName.text = appointment.patientName.ifBlank { "Bệnh nhân" }
            row.appointmentPatientInfo.text = buildString {
                append(formatDate(appointment.appointmentDate))
                append(" · ")
                append(appointment.startTime)
                append(" · ")
                append(appointment.specialtyName.ifBlank { appointment.doctorName })
                if (appointment.id in conflictIds) append(" · ⚠️ Trùng giờ")
            }
            row.appointmentStatus.text = when (appointment.status) {
                AppointmentStatus.PENDING -> "Chờ duyệt"
                AppointmentStatus.CONFIRMED -> "Đã xác nhận"
                AppointmentStatus.ARRIVED -> getString(R.string.doctor_status_arrived)
                AppointmentStatus.IN_PROGRESS -> getString(R.string.doctor_status_in_progress)
                AppointmentStatus.REJECTED -> "Từ chối"
                AppointmentStatus.CANCELLED -> "Đã hủy"
                AppointmentStatus.COMPLETED -> "Hoàn thành"
            }
            applyStatusStyle(row.appointmentStatus, appointment.status)
            row.root.setOnClickListener {
                openAppointmentDetail(appointment)
            }
            binding.appointmentsList.addView(row.root)
        }
    }

    private fun applyStatusStyle(statusView: TextView, status: AppointmentStatus) {
        val (background, foreground) = when (status) {
            AppointmentStatus.PENDING -> 0xFFFFF2DD.toInt() to 0xFFC17D12.toInt()
            AppointmentStatus.CONFIRMED, AppointmentStatus.ARRIVED -> 0xFFE5F8EF.toInt() to 0xFF15865E.toInt()
            AppointmentStatus.IN_PROGRESS -> 0xFFE8F7FB.toInt() to 0xFF0282A8.toInt()
            AppointmentStatus.COMPLETED -> 0xFFEEF2FB.toInt() to 0xFF4E6E9E.toInt()
            AppointmentStatus.REJECTED, AppointmentStatus.CANCELLED -> 0xFFFFF0F0.toInt() to 0xFFD33F48.toInt()
        }
        statusView.background = GradientDrawable().apply {
            setColor(background)
            cornerRadius = resources.displayMetrics.density * 8f
        }
        statusView.setTextColor(foreground)
    }

    private fun conflictingAppointments(): List<List<Appointment>> = appointments
        .filter { it.status in setOf(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED,
            AppointmentStatus.ARRIVED, AppointmentStatus.IN_PROGRESS) }
        .groupBy { "${it.doctorId}|${it.appointmentDate}|${it.startTime}" }
        .values.filter { it.size > 1 }

    private fun showConflicts() {
        val conflicts = conflictingAppointments()
        if (conflicts.isEmpty()) {
            AlertDialog.Builder(requireContext()).setTitle("Kiểm tra lịch trùng")
                .setMessage("Chưa tìm thấy nhiều lịch đặt cùng bác sĩ và khung giờ.")
                .setPositiveButton(android.R.string.ok, null).show()
            return
        }
        val conflicting = conflicts.flatten()
        val labels = conflicting.map {
            "${it.patientName.ifBlank { "Bệnh nhân" }} · ${formatDate(it.appointmentDate)} ${it.startTime} · ${it.doctorName}"
        }.toTypedArray()
        AlertDialog.Builder(requireContext()).setTitle("Các lịch cùng khung giờ")
            .setItems(labels) { _, index -> openAppointmentDetail(conflicting[index]) }
            .setNegativeButton("Đóng", null).show()
    }

    private fun chooseDate() {
        val current = selectedDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
        val calendar = Calendar.getInstance().apply { set(current.year, current.monthValue - 1, current.dayOfMonth) }
        DatePickerDialog(requireContext(), { _, year, month, day ->
            selectedDate = LocalDate.of(year, month + 1, day).toString()
            binding.appointmentTimeFilter.text = "${formatDate(selectedDate.orEmpty())}  ▾"
            renderAppointments()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).apply {
            setButton(DialogInterface.BUTTON_NEUTRAL, "Bỏ lọc ngày") { _, _ ->
                selectedDate = null
                binding.appointmentTimeFilter.text = "Ngày khám ▾"
                renderAppointments()
            }
        }.show()
    }

    private fun chooseDoctor() {
        val doctors = appointments.map { it.doctorId to it.doctorName.ifBlank { getString(R.string.manager_doctors) } }.distinct()
        val labels = listOf(getString(R.string.manager_all_doctors)) + doctors.map { it.second }
        AlertDialog.Builder(requireContext())
            .setItems(labels.toTypedArray()) { _, index ->
                selectedDoctorId = if (index == 0) null else doctors[index - 1].first
                binding.appointmentDoctorFilter.text = labels[index] + " ▾"
                renderAppointments()
            }.show()
    }

    private fun openAppointmentDetail(appointment: Appointment) {
        val occupied = appointments.any {
            it.id != appointment.id && it.doctorId == appointment.doctorId &&
                it.appointmentDate == appointment.appointmentDate && it.startTime == appointment.startTime &&
                it.status in setOf(AppointmentStatus.CONFIRMED, AppointmentStatus.ARRIVED,
                    AppointmentStatus.IN_PROGRESS)
        }
        ManagerDetailSheets.appointment(requireActivity(), appointment, occupied, ::loadAppointments)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun formatDate(raw: String): String = runCatching {
        val date = LocalDate.parse(raw)
        "%02d/%02d/%04d".format(date.dayOfMonth, date.monthValue, date.year)
    }.getOrDefault(raw)
}
