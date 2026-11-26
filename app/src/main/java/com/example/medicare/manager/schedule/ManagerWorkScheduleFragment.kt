package com.example.medicare.manager.schedule

import android.content.Intent
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.medicare.data.schedule.ScheduleRepository
import com.example.medicare.data.appointment.AppointmentRepository
import com.example.medicare.data.doctor.DoctorManagementRepository
import com.example.medicare.databinding.ActivityWorkScheduleBinding
import java.time.LocalDate
import java.util.Calendar

/** Work-schedule tab hosted directly by ManagerActivity. */
class ManagerWorkScheduleFragment : Fragment() {
    private var _binding: ActivityWorkScheduleBinding? = null
    private val binding get() = _binding!!
    private val repository = ScheduleRepository()
    private val appointments = AppointmentRepository()
    private var selectedDate = LocalDate.now()
    private var selectedDoctorId: String? = null
    private val addSchedule = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) loadSchedules()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = ActivityWorkScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.workScheduleHeader.visibility = View.GONE
        binding.workScheduleWeekly.setOnClickListener {
            startActivity(Intent(requireContext(), WeeklyScheduleActivity::class.java))
        }
        binding.workScheduleWeeklyInline.setOnClickListener {
            startActivity(Intent(requireContext(), WeeklyScheduleActivity::class.java))
        }
        binding.workScheduleAdd.setOnClickListener {
            addSchedule.launch(Intent(requireContext(), AssignDoctorScheduleActivity::class.java).putExtra(AssignDoctorScheduleActivity.EXTRA_DOCTOR_ID, selectedDoctorId).putExtra(AssignDoctorScheduleActivity.EXTRA_DATE, selectedDate.toString()))
        }
        binding.workScheduleAddInline.setOnClickListener {
            ScheduleActionsSheet.show(
                requireContext(),
                onCreateShift = {
                    addSchedule.launch(
                        Intent(requireContext(), AssignDoctorScheduleActivity::class.java)
                            .putExtra(AssignDoctorScheduleActivity.EXTRA_DOCTOR_ID, selectedDoctorId)
                            .putExtra(AssignDoctorScheduleActivity.EXTRA_DATE, selectedDate.toString()),
                    )
                },
                onLeaveRequests = {
                    startActivity(Intent(requireContext(), LeaveRequestsActivity::class.java))
                },
            )
        }
        binding.workScheduleLeaveRequests.setOnClickListener {
            startActivity(Intent(requireContext(), LeaveRequestsActivity::class.java))
        }
        binding.workScheduleDate.setOnClickListener { pickDate() }
        binding.workScheduleDoctorFilter.setOnClickListener { binding.workScheduleDoctorFilter.showDropDown() }
        loadDoctors()
        updateDate()
        loadSchedules()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) loadSchedules()
    }

    private fun pickDate() {
        val calendar = Calendar.getInstance().apply {
            set(selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth)
        }
        DatePickerDialog(requireContext(), { _, year, month, day ->
            selectedDate = LocalDate.of(year, month + 1, day)
            updateDate()
            loadSchedules()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun updateDate() {
        binding.workScheduleDate.setText(WorkScheduleWeekStrip.formatDate(selectedDate))
        binding.workScheduleSection.text = "Các khung giờ ngày ${WorkScheduleWeekStrip.formatDate(selectedDate)}"
        WorkScheduleWeekStrip.render(requireContext(), binding.workScheduleWeekStrip, selectedDate) { date ->
            selectedDate = date
            updateDate()
            loadSchedules()
        }
    }

    private fun loadDoctors() {
        DoctorManagementRepository(requireContext()).getDoctors { result ->
            if (_binding == null) return@getDoctors
            val doctors = result.getOrDefault(emptyList())
            val labels = listOf("Tất cả bác sĩ") + doctors.map { "BS. ${it.fullName}" }
            binding.workScheduleDoctorFilter.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels))
            if (selectedDoctorId == null && doctors.isNotEmpty()) selectedDoctorId = doctors.first().id
            val selected = doctors.indexOfFirst { it.id == selectedDoctorId }
            binding.workScheduleDoctorFilter.setText(labels.getOrElse(selected + 1) { labels.first() }, false)
            binding.workScheduleDoctorFilter.setOnItemClickListener { _, _, position, _ ->
                selectedDoctorId = doctors.getOrNull(position - 1)?.id
                loadSchedules()
            }
            loadSchedules()
        }
    }

    private fun loadSchedules() {
        repository.getSchedulesForDate(selectedDate.toString()) { result ->
            if (_binding != null) {
                result.onSuccess {
                    val visible = it.filter { schedule -> selectedDoctorId == null || schedule.doctorId == selectedDoctorId }
                    appointments.getAppointments { appointmentResult ->
                        if (_binding == null) return@getAppointments
                        appointmentResult.onSuccess { all ->
                            ScheduleListRenderer.renderSlots(requireContext(), binding.workScheduleList,
                                binding.workScheduleEmpty, visible, all.filter { item -> item.appointmentDate == selectedDate.toString() },
                                ::openSchedule) { schedule, time, locked ->
                                repository.setSlotLocked(schedule.id, time, locked) { update ->
                                    if (_binding == null) return@setSlotLocked
                                    update.onSuccess { loadSchedules() }.onFailure { error ->
                                        Toast.makeText(requireContext(), error.message ?: "Không thể cập nhật khung giờ.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }.onFailure { error ->
                            binding.workScheduleEmpty.text = error.message ?: "Không tải được lịch hẹn."
                            binding.workScheduleEmpty.visibility = View.VISIBLE
                        }
                    }
                }.onFailure {
                    binding.workScheduleEmpty.text = getString(com.example.medicare.R.string.manager_no_data)
                    binding.workScheduleEmpty.visibility = View.VISIBLE
                    Toast.makeText(requireContext(), it.message ?: getString(com.example.medicare.R.string.manager_save_failed), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun openSchedule(schedule: com.example.medicare.model.DoctorSchedule) {
        startActivity(Intent(requireContext(), WorkScheduleDetailActivity::class.java)
            .putExtra(WorkScheduleDetailActivity.EXTRA_SCHEDULE_ID, schedule.id)
            .putExtra(WorkScheduleDetailActivity.EXTRA_DOCTOR_NAME, schedule.doctorName)
            .putExtra(WorkScheduleDetailActivity.EXTRA_SPECIALTY, schedule.specialtyName))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
