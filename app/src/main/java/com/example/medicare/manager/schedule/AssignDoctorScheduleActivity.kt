package com.example.medicare.manager.schedule

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.data.doctor.DoctorManagementRepository
import com.example.medicare.data.schedule.ScheduleRepository
import com.example.medicare.databinding.ActivityAssignDoctorScheduleBinding
import com.example.medicare.model.Doctor
import com.example.medicare.model.WorkShift
import java.time.LocalDate
import java.util.Calendar

class AssignDoctorScheduleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAssignDoctorScheduleBinding
    private val doctorRepository by lazy { DoctorManagementRepository(this) }
    private val scheduleRepository = ScheduleRepository()
    private var doctors: List<Doctor> = emptyList()
    private var shifts: List<WorkShift> = emptyList()
    private var selectedDate = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAssignDoctorScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.assignScheduleBack.setOnClickListener { finish() }
        binding.assignScheduleDate.setText(selectedDate.toString())
        binding.assignScheduleDate.setOnClickListener { pickDate() }
        binding.assignScheduleDoctor.setOnClickListener { binding.assignScheduleDoctor.showDropDown() }
        binding.assignScheduleShift.setOnClickListener { binding.assignScheduleShift.showDropDown() }
        binding.assignScheduleSave.setOnClickListener { saveSchedule() }
        loadOptions()
    }

    private fun loadOptions() {
        doctorRepository.getDoctors { result ->
            result.onSuccess { loadedDoctors ->
                doctors = loadedDoctors.filter { it.isActive }
                binding.assignScheduleDoctor.setAdapter(
                    ArrayAdapter(this, android.R.layout.simple_list_item_1, doctors.map { it.fullName }),
                )
            }.onFailure(::showError)
        }
        doctorRepository.getWorkShifts { result ->
            result.onSuccess { loadedShifts ->
                shifts = loadedShifts.filter { it.isActive }
                binding.assignScheduleShift.setAdapter(
                    ArrayAdapter(this, android.R.layout.simple_list_item_1, shifts.map { "${it.name} (${it.startTime} - ${it.endTime})" }),
                )
            }.onFailure(::showError)
        }
    }

    private fun pickDate() {
        val calendar = Calendar.getInstance().apply {
            set(selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth)
        }
        DatePickerDialog(this, { _, year, month, day ->
            selectedDate = LocalDate.of(year, month + 1, day)
            binding.assignScheduleDate.setText(selectedDate.toString())
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun saveSchedule() {
        val doctor = doctors.firstOrNull { it.fullName == binding.assignScheduleDoctor.text.toString().trim() }
        val shift = shifts.firstOrNull {
            "${it.name} (${it.startTime} - ${it.endTime})" == binding.assignScheduleShift.text.toString().trim()
        }
        val room = binding.assignScheduleRoom.text.toString().trim()
        val duration = binding.assignScheduleDuration.text.toString().toIntOrNull()
        val capacity = binding.assignScheduleCapacity.text.toString().toIntOrNull()

        binding.assignScheduleDoctor.error = if (doctor == null) getString(R.string.manager_required_field) else null
        binding.assignScheduleShift.error = if (shift == null) getString(R.string.manager_required_field) else null
        binding.assignScheduleRoom.error = if (room.isBlank()) getString(R.string.manager_required_field) else null
        binding.assignScheduleDuration.error = if (duration == null || duration <= 0) getString(R.string.manager_required_field) else null
        binding.assignScheduleCapacity.error = if (capacity == null || capacity <= 0) getString(R.string.manager_required_field) else null
        if (doctor == null || shift == null || room.isBlank() || duration == null || duration <= 0 || capacity == null || capacity <= 0) return

        binding.assignScheduleSave.isEnabled = false
        scheduleRepository.createSchedule(
            doctor = doctor,
            shift = shift,
            workDate = selectedDate.toString(),
            room = room,
            appointmentDurationMinutes = duration,
            maximumAppointments = capacity,
            note = binding.assignScheduleNote.text.toString().trim(),
        ) { result ->
            binding.assignScheduleSave.isEnabled = true
            result.onSuccess {
                setResult(RESULT_OK)
                finish()
            }.onFailure(::showError)
        }
    }

    private fun showError(error: Throwable) {
        Toast.makeText(this, error.message ?: getString(R.string.manager_save_failed), Toast.LENGTH_LONG).show()
    }
}
