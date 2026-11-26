package com.example.medicare.manager.schedule

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.medicare.R
import com.example.medicare.data.doctor.DoctorManagementRepository
import com.example.medicare.data.schedule.ScheduleRepository
import com.example.medicare.databinding.ActivityAssignDoctorScheduleBinding
import com.example.medicare.databinding.ItemOptionPickerRowBinding
import com.example.medicare.databinding.SheetOptionPickerBinding
import com.example.medicare.model.Doctor
import com.example.medicare.model.WorkShift
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
    private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    companion object {
        const val EXTRA_DOCTOR_ID = "schedule_doctor_id"
        const val EXTRA_DATE = "schedule_date"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAssignDoctorScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, (resources.displayMetrics.heightPixels * .92f).toInt())
        window.setGravity(Gravity.BOTTOM)
        selectedDate = runCatching {
            LocalDate.parse(savedInstanceState?.getString(EXTRA_DATE) ?: intent.getStringExtra(EXTRA_DATE))
        }.getOrDefault(LocalDate.now())

        restoredDoctorId = savedInstanceState?.getString(EXTRA_DOCTOR_ID) ?: intent.getStringExtra(EXTRA_DOCTOR_ID)
        restoredShiftId = savedInstanceState?.getString("shift_id")
        if (binding.assignScheduleDuration.text.isNullOrBlank()) binding.assignScheduleDuration.setText("30")
        if (binding.assignScheduleCapacity.text.isNullOrBlank()) binding.assignScheduleCapacity.setText("8")

        renderDate()
        binding.assignScheduleBack.setOnClickListener { finish() }
        binding.assignScheduleCancel.setOnClickListener { finish() }
        binding.assignScheduleDate.setOnClickListener { pickDate() }
        binding.assignScheduleDoctor.setOnClickListener { pickDoctor() }
        binding.assignScheduleShift.setOnClickListener { pickShift() }
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
                selectedDoctor?.let { renderDoctor(it) }
            }.onFailure(::showError)
        }
        doctorRepository.getWorkShifts { result ->
            if (isFinishing || isDestroyed) return@getWorkShifts
            result.onSuccess { loadedShifts ->
                shifts = loadedShifts.filter { it.isActive }
                selectedShift = shifts.firstOrNull { it.id == restoredShiftId }
                selectedShift?.let { renderShift(it) }
            }.onFailure(::showError)
        }
    }

    private fun pickDoctor() {
        if (doctors.isEmpty()) {
            Toast.makeText(this, "Chưa có bác sĩ đang hoạt động.", Toast.LENGTH_SHORT).show()
            return
        }
        showOptionPicker(
            title = "Chọn bác sĩ",
            options = doctors.map { doctor ->
                OptionItem(
                    title = "BS. ${doctor.fullName}",
                    subtitle = doctor.specialty.ifBlank { "Chưa có chuyên khoa" },
                ) {
                    selectedDoctor = doctor
                    renderDoctor(doctor)
                }
            },
        )
    }

    private fun pickShift() {
        if (shifts.isEmpty()) {
            Toast.makeText(
                this,
                "Chưa có ca làm việc đang hoạt động. Hãy tạo ca trong danh mục ca làm việc.",
                Toast.LENGTH_LONG,
            ).show()
            return
        }
        showOptionPicker(
            title = "Chọn ca làm việc",
            options = shifts.map { shift ->
                OptionItem(
                    title = shift.name,
                    subtitle = "${shift.startTime} – ${shift.endTime}",
                ) {
                    selectedShift = shift
                    renderShift(shift)
                }
            },
        )
    }

    private fun showOptionPicker(title: String, options: List<OptionItem>) {
        val sheet = SheetOptionPickerBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(sheet.root)
        sheet.optionPickerTitle.text = title
        options.forEach { option ->
            val row = ItemOptionPickerRowBinding.inflate(LayoutInflater.from(this), sheet.optionPickerList, false)
            row.optionPickerRowTitle.text = option.title
            row.optionPickerRowSubtitle.text = option.subtitle
            row.optionPickerRowSubtitle.visibility =
                if (option.subtitle.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
            row.root.setOnClickListener {
                option.onSelect()
                dialog.dismiss()
            }
            sheet.optionPickerList.addView(row.root)
        }
        dialog.show()
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
    }

    private fun pickDate() {
        val calendar = Calendar.getInstance().apply {
            set(selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth)
        }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate = LocalDate.of(year, month + 1, day)
                renderDate()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    private fun saveSchedule() {
        val doctor = selectedDoctor
        val shift = selectedShift
        val room = binding.assignScheduleRoom.text.toString().trim()
        val duration = binding.assignScheduleDuration.text.toString().toIntOrNull()
        val capacity = binding.assignScheduleCapacity.text.toString().toIntOrNull()

        markSelectError(binding.assignScheduleDoctor, doctor == null)
        markSelectError(binding.assignScheduleShift, shift == null)
        binding.assignScheduleRoom.error = if (room.isBlank()) getString(R.string.manager_required_field) else null
        binding.assignScheduleDuration.error =
            if (duration == null || duration <= 0) getString(R.string.manager_required_field) else null
        binding.assignScheduleCapacity.error =
            if (capacity == null || capacity <= 0) getString(R.string.manager_required_field) else null

        if (doctor == null || shift == null || room.isBlank() || duration == null || duration <= 0 || capacity == null || capacity <= 0) {
            Toast.makeText(this, R.string.manager_required_field, Toast.LENGTH_SHORT).show()
            return
        }

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

    private fun renderDoctor(doctor: Doctor) {
        binding.assignScheduleDoctor.text = "BS. ${doctor.fullName}"
        markSelectError(binding.assignScheduleDoctor, false)
    }

    private fun renderShift(shift: WorkShift) {
        binding.assignScheduleShift.text = "${shift.name} · ${shift.startTime} – ${shift.endTime}"
        markSelectError(binding.assignScheduleShift, false)
    }

    private fun renderDate() {
        binding.assignScheduleDate.text = selectedDate.format(dateFormat)
    }

    private fun markSelectError(view: TextView, error: Boolean) {
        view.setBackgroundResource(
            if (error) R.drawable.assign_schedule_select_error_background
            else R.drawable.assign_schedule_select_background,
        )
        view.setTextColor(ContextCompat.getColor(this, R.color.manager_ink))
    }

    private fun showError(error: Throwable) {
        Toast.makeText(this, error.message ?: getString(R.string.manager_save_failed), Toast.LENGTH_LONG).show()
    }

    private data class OptionItem(
        val title: String,
        val subtitle: String,
        val onSelect: () -> Unit,
    )
}
