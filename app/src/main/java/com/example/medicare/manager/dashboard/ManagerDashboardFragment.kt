package com.example.medicare.manager.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.medicare.R
import com.example.medicare.data.appointment.AppointmentRepository
import com.example.medicare.data.doctor.DoctorManagementRepository
import com.example.medicare.databinding.FragmentManagerDashboardBinding
import com.example.medicare.databinding.ItemDashboardRequestBinding
import com.example.medicare.manager.ManagerActivity
import com.example.medicare.manager.ManagerDetailSheets
import com.example.medicare.manager.appointment.DetailPatientActivity
import com.example.medicare.model.Appointment
import com.example.medicare.model.AppointmentStatus
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

/** Admin landing page: real operating metrics, weekly chart and latest booking requests. */
class ManagerDashboardFragment : Fragment() {
    private var _binding: FragmentManagerDashboardBinding? = null
    private val binding get() = _binding!!
    private val doctors by lazy { DoctorManagementRepository(requireContext()) }
    private val appointments = AppointmentRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentManagerDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.dashboardDoctorsCard.root.setOnClickListener {
            (activity as? ManagerActivity)?.showTab(ManagerActivity.TAB_DOCTORS)
        }
        binding.dashboardPatientsCard.root.setOnClickListener {
            (activity as? ManagerActivity)?.showTab(ManagerActivity.TAB_PATIENTS)
        }
        binding.dashboardSchedulesCard.root.setOnClickListener { openAppointments() }
        binding.dashboardPendingCard.root.setOnClickListener { openAppointments() }
        binding.dashboardHandleRequests.setOnClickListener { openAppointments() }
        binding.dashboardAllRequests.setOnClickListener { openAppointments() }
        binding.dashboardViewReport.setOnClickListener {
            startActivity(Intent(requireContext(), ManagerReportActivity::class.java))
        }
        loadSummary()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) loadSummary()
    }

    private fun openAppointments() {
        (activity as? ManagerActivity)?.showTab(ManagerActivity.TAB_APPOINTMENTS)
    }

    private fun loadSummary() {
        doctors.getDoctors { result ->
            val current = _binding ?: return@getDoctors
            current.dashboardDoctorsCard.dashboardDoctorsValue.text =
                result.getOrNull()?.size?.let(::formatCount) ?: getString(R.string.manager_no_data)
        }
        doctors.getPatientCount { result ->
            val current = _binding ?: return@getPatientCount
            current.dashboardPatientsCard.dashboardPatientsValue.text =
                result.getOrNull()?.let(::formatCount) ?: getString(R.string.manager_no_data)
        }
        appointments.getAppointments { result ->
            val current = _binding ?: return@getAppointments
            val all = result.getOrDefault(emptyList())
            renderAppointmentMetrics(current, all)
        }
    }

    private fun renderAppointmentMetrics(current: FragmentManagerDashboardBinding, all: List<Appointment>) {
        val today = LocalDate.now()
        val todayValue = today.toString()
        current.dashboardSchedulesCard.dashboardSchedulesValue.text =
            formatCount(all.count { it.appointmentDate == todayValue })
        current.dashboardPendingCard.dashboardPendingValue.text =
            formatCount(all.count { it.status == AppointmentStatus.PENDING })

        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val days = (0..6).map { monday.plusDays(it.toLong()) }
        val counts = days.map { date -> all.count { it.appointmentDate == date.toString() } }
        current.dashboardWeeklyChart.submitData(
            listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN"),
            counts,
        )
        current.dashboardWeekCaption.text =
            "Dữ liệu tuần " + monday.dayOfMonth + "/" + monday.monthValue + " – " +
                days.last().dayOfMonth + "/" + days.last().monthValue
        renderLatestRequests(current, all)
    }

    private fun renderLatestRequests(current: FragmentManagerDashboardBinding, all: List<Appointment>) {
        val requests = all.filter { it.status == AppointmentStatus.PENDING }
            .sortedWith(compareBy<Appointment> { it.appointmentDate }.thenBy { it.startTime })
            .take(2)
        current.dashboardRequestsList.removeAllViews()
        current.dashboardRequestsEmpty.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
        requests.forEach { appointment ->
            val row = ItemDashboardRequestBinding.inflate(layoutInflater, current.dashboardRequestsList, false)
            row.dashboardRequestInitials.text = "👤"
            row.dashboardRequestName.text = appointment.patientName.ifBlank { "Bệnh nhân" }
            row.dashboardRequestInfo.text =
                appointment.appointmentDate + " · " + appointment.startTime + " · " +
                    appointment.specialtyName.ifBlank { appointment.doctorName }
            row.dashboardRequestStatus.text = getString(R.string.manager_pending)
            row.root.setOnClickListener { openRequest(appointment, all) }
            current.dashboardRequestsList.addView(row.root)
        }
    }

    private fun openRequest(appointment: Appointment, all: List<Appointment>) {
        ManagerDetailSheets.appointment(requireActivity(), appointment, all.any {
                it.id != appointment.id && it.doctorId == appointment.doctorId &&
                    it.appointmentDate == appointment.appointmentDate && it.startTime == appointment.startTime &&
                    it.status in setOf(AppointmentStatus.CONFIRMED, AppointmentStatus.ARRIVED,
                        AppointmentStatus.IN_PROGRESS)
            }, ::loadSummary)
    }

    private fun formatCount(value: Int): String =
        NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(value)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
