package com.example.medicare.manager.schedule

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
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
    private var selectedDoctor: Doctor? = null
    private var selectedShift: WorkShift? = null
    private var restoredDoctorId: String? = null
    private var restoredShiftId: String? = null

    companion object {
        const val EXTRA_DOCTOR_ID = "schedule_doctor_id"
        const val EXTRA_DATE = "schedule_date"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAssignDoctorScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, (resources.displayMetrics.heightPixels * .88f).toInt())
        window.setGravity(Gravity.BOTTOM)
        selectedDate = runCatching { LocalDate.parse(savedInstanceState?.getString(EXTRA_DATE)
            ?: intent.getStringExtra(EXTRA_DATE)) }.getOrDefault(LocalDate.now())

        restoredDoctorId = savedInstanceState?.getString(EXTRA_DOCTOR_ID) ?: intent.getStringExtra(EXTRA_DOCTOR_ID)
        restoredShiftId = savedInstanceState?.getString("shift_id")
        binding.assignScheduleBack.setOnClickListener { finish() }
        binding.assignScheduleDate.setText(selectedDate.toString())
        binding.assignScheduleDate.setOnClickListener { pickDate() }
        binding.assignScheduleDoctor.setOnClickListener {
            if (doctors.isEmpty()) { Toast.makeText(this, "Chưa có bác sĩ đang hoạt động.", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            AlertDialog.Builder(this).setTitle("Chọn bác sĩ")
                .setItems(doctors.map { "BS. ${it.fullName} · ${it.specialty}" }.toTypedArray()) { _, index ->
                    selectedDoctor = doctors[index]
                    binding.assignScheduleDoctor.setText("BS. ${doctors[index].fullName}")
                    binding.assignScheduleDoctor.error = null
                }.show()
        }
        binding.assignScheduleShift.setOnClickListener {
            if (shifts.isEmpty()) { Toast.makeText(this, "Chưa có ca làm việc đang hoạt động. Hãy tạo ca trong danh mục ca làm việc.", Toast.LENGTH_LONG).show(); return@setOnClickListener }
            AlertDialog.Builder(this).setTitle("Chọn ca làm việc")
                .setItems(shifts.map { "${it.name} (${it.startTime} – ${it.endTime})" }.toTypedArray()) { _, index ->
                    selectedShift = shifts[index]
                    binding.assignScheduleShift.setText("${shifts[index].name} (${shifts[index].startTime} – ${shifts[index].endTime})")
                    binding.assignScheduleShift.error = null
                }.show()
        }
        binding.assignScheduleSave.setOnClickListener { saveSchedule() }
        loadOptions()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(EXTRA_DATE, selectedDate.toString())
        outState.putString(EXTRA_DOCTOR_ID, selectedDoctor?.id)
        outState.putString("shift_id", selectedShift?.id)
        super.onSaveInstanceState(outState)
    }

    private fun loadOptions() {
        doctorRepository.getDoctors { result ->
            if (isFinishing || isDestroyed) return@getDoctors
            result.onSuccess { loadedDoctors ->
                doctors = loadedDoctors.filter { it.isActive }
                selectedDoctor = doctors.firstOrNull { it.id == restoredDoctorId }
                selectedDoctor?.let { binding.assignScheduleDoctor.setText("BS. ${it.fullName}") }
            }.onFailure(::showError)
        }
        doctorRepository.getWorkShifts { result ->
            if (isFinishing || isDestroyed) return@getWorkShifts
            result.onSuccess { loadedShifts ->
                shifts = loadedShifts.filter { it.isActive }
                selectedShift = shifts.firstOrNull { it.id == restoredShiftId }
                selectedShift?.let { binding.assignScheduleShift.setText("${it.name} (${it.startTime} – ${it.endTime})") }

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
        val doctor = selectedDoctor
        val shift = selectedShift
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
            if (isFinishing || isDestroyed) return@createSchedule
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
