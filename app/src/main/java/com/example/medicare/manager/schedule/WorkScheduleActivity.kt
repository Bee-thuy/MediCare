package com.example.medicare.manager.schedule

import android.content.Intent
import android.app.DatePickerDialog
import android.app.AlertDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.data.schedule.ScheduleRepository
import com.example.medicare.data.doctor.DoctorManagementRepository
import com.example.medicare.databinding.ActivityWorkScheduleBinding
import java.time.LocalDate
import java.util.Calendar

class WorkScheduleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWorkScheduleBinding
    private val repository = ScheduleRepository()
    private var selectedDate = LocalDate.now()
    private var selectedDoctorId: String? = null
    private val addSchedule = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) loadSchedules()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.workScheduleBack.setOnClickListener { finish() }
        binding.workScheduleWeekly.setOnClickListener {
            startActivity(Intent(this, WeeklyScheduleActivity::class.java))
        }
        binding.workScheduleWeeklyInline.setOnClickListener {
            startActivity(Intent(this, WeeklyScheduleActivity::class.java))
        }
        binding.workScheduleAdd.setOnClickListener {
            addSchedule.launch(Intent(this, AssignDoctorScheduleActivity::class.java))
        }
        binding.workScheduleAddInline.setOnClickListener {
            AlertDialog.Builder(this).setItems(arrayOf("Tạo ca làm việc", "Xem yêu cầu nghỉ / đổi ca")) { _, index ->
                if (index == 0) addSchedule.launch(Intent(this, AssignDoctorScheduleActivity::class.java))
                else startActivity(Intent(this, LeaveRequestsActivity::class.java))
            }.show()
        }
        binding.workScheduleLeaveRequests.setOnClickListener {
            startActivity(Intent(this, LeaveRequestsActivity::class.java))
        }
        binding.workScheduleDate.setOnClickListener { pickDate() }
        binding.workScheduleDoctorFilter.setOnClickListener { binding.workScheduleDoctorFilter.showDropDown() }
        loadDoctors()
        updateDate()
        loadSchedules()
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) loadSchedules()
    }

    private fun pickDate() {
        val calendar = Calendar.getInstance().apply {
            set(selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth)
        }
        DatePickerDialog(this, { _, year, month, day ->
            selectedDate = LocalDate.of(year, month + 1, day)
            updateDate()
            loadSchedules()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun updateDate() {
        binding.workScheduleDate.setText(WorkScheduleWeekStrip.formatDate(selectedDate))
        binding.workScheduleSection.text = "Các ca ngày ${WorkScheduleWeekStrip.formatDate(selectedDate)}"
        WorkScheduleWeekStrip.render(this, binding.workScheduleWeekStrip, selectedDate) { date ->
            selectedDate = date
            updateDate()
            loadSchedules()
        }
    }

    private fun loadDoctors() {
        DoctorManagementRepository(this).getDoctors { result ->
            if (isFinishing || isDestroyed) return@getDoctors
            val doctors = result.getOrDefault(emptyList())
            val labels = listOf("Tất cả bác sĩ") + doctors.map { "BS. ${it.fullName}" }
            binding.workScheduleDoctorFilter.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, labels))
            binding.workScheduleDoctorFilter.setText(labels.first(), false)
            binding.workScheduleDoctorFilter.setOnItemClickListener { _, _, position, _ ->
                selectedDoctorId = doctors.getOrNull(position - 1)?.id
                loadSchedules()
            }
        }
    }

    private fun loadSchedules() {
        repository.getSchedulesForDate(selectedDate.toString()) { result ->
            result.onSuccess {
                val visible = it.filter { schedule -> selectedDoctorId == null || schedule.doctorId == selectedDoctorId }
                ScheduleListRenderer.render(this, binding.workScheduleList, binding.workScheduleEmpty, visible) { schedule ->
                    startActivity(Intent(this, WorkScheduleDetailActivity::class.java)
                        .putExtra(WorkScheduleDetailActivity.EXTRA_SCHEDULE_ID, schedule.id)
                        .putExtra(WorkScheduleDetailActivity.EXTRA_DOCTOR_NAME, schedule.doctorName)
                        .putExtra(WorkScheduleDetailActivity.EXTRA_SPECIALTY, schedule.specialtyName))
                }
            }.onFailure {
                binding.workScheduleEmpty.text = getString(R.string.manager_no_data)
                binding.workScheduleEmpty.visibility = android.view.View.VISIBLE
                Toast.makeText(this, it.message ?: getString(R.string.manager_save_failed), Toast.LENGTH_LONG).show()
            }
        }
    }
}
