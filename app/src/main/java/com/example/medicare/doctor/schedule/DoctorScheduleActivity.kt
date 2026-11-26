package com.example.medicare.doctor.schedule

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.data.schedule.DoctorLeaveRequestRepository
import com.example.medicare.data.schedule.ScheduleRepository
import com.example.medicare.databinding.ActivityDoctorScheduleBinding
import com.example.medicare.model.DoctorSchedule
import com.example.medicare.manager.schedule.WorkScheduleWeekStrip
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

class DoctorScheduleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDoctorScheduleBinding
    private val schedules = ScheduleRepository()
    private val leaveRequests = DoctorLeaveRequestRepository()
    private var allSchedules: List<DoctorSchedule> = emptyList()
    private var selectedDate = LocalDate.now()
    private var mode = Mode.DAY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.doctorWorkBack.setOnClickListener { finish() }
        binding.doctorWorkDate.setOnClickListener { pickDate() }
        binding.doctorWorkDay.setOnClickListener { mode = Mode.DAY; render() }
        binding.doctorWorkWeek.setOnClickListener { mode = Mode.WEEK; render() }
        binding.doctorWorkMonth.setOnClickListener { mode = Mode.MONTH; render() }
        binding.doctorWorkRequestLeave.setOnClickListener { requestLeave() }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return finish()
        schedules.getSchedulesForDoctor(uid) { result ->
            result.onSuccess { allSchedules = it; render() }
                .onFailure { Toast.makeText(this, it.message ?: getString(R.string.doctor_schedule_load_failed), Toast.LENGTH_LONG).show() }
        }
        leaveRequests.getMine { result ->
            result.onSuccess { requests ->
                binding.doctorWorkRequests.text = if (requests.isEmpty()) {
                    getString(R.string.doctor_no_leave_requests)
                } else {
                    requests.take(3).joinToString("\n") { request ->
                        val date = runCatching {
                            java.time.LocalDate.parse(request.date)
                                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        }.getOrDefault(request.date)
                        val status = when (request.status) {
                            "PENDING" -> "Chờ duyệt"
                            "APPROVED" -> "Đã duyệt"
                            "REJECTED" -> "Từ chối"
                            else -> request.status
                        }
                        "$date · $status · ${request.reason}"
                    }
                }
            }
        }
    }

    private fun render() {
        binding.doctorWorkDate.text = "Tháng ${selectedDate.monthValue}/${selectedDate.year}"
        WorkScheduleWeekStrip.render(this, binding.doctorWorkWeekStrip, selectedDate,
            R.drawable.doctor_week_day_background) { date ->
            selectedDate = date
            mode = Mode.DAY
            render()
        }
        binding.doctorWorkDay.isSelected = mode == Mode.DAY
        binding.doctorWorkWeek.isSelected = mode == Mode.WEEK
        binding.doctorWorkMonth.isSelected = mode == Mode.MONTH
        val visible = allSchedules.filter { schedule ->
            val date = runCatching { LocalDate.parse(schedule.workDate) }.getOrNull() ?: return@filter false
            when (mode) {
                Mode.DAY -> date == selectedDate
                Mode.WEEK -> !date.isBefore(selectedDate.minusDays(selectedDate.dayOfWeek.value - 1L)) && !date.isAfter(selectedDate.plusDays(7 - selectedDate.dayOfWeek.value.toLong()))
                Mode.MONTH -> date.year == selectedDate.year && date.month == selectedDate.month
            }
        }
        binding.doctorWorkList.removeAllViews()
        binding.doctorWorkEmpty.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
        visible.forEach { schedule ->
            val row = layoutInflater.inflate(R.layout.item_doctor_work_schedule, binding.doctorWorkList, false)
            row.findViewById<TextView>(R.id.doctorWorkRowDate).text = schedule.shiftName
            row.findViewById<TextView>(R.id.doctorWorkRowInfo).text = "${schedule.startTime} – ${schedule.endTime} · ${schedule.room}"
            row.findViewById<TextView>(R.id.doctorWorkRowStatus).apply {
                text = if (schedule.isLocked) "Đã khóa" else "Đã phân công"
                setTextColor(getColor(if (schedule.isLocked) R.color.manager_red else R.color.manager_green))
                setBackgroundResource(if (schedule.isLocked) R.drawable.manager_status_inactive_background else R.drawable.manager_status_active_background)
            }
            binding.doctorWorkList.addView(row)
        }
    }

    private fun pickDate() {
        val calendar = Calendar.getInstance().apply { set(selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth) }
        DatePickerDialog(this, { _, year, month, day -> selectedDate = LocalDate.of(year, month + 1, day); render() }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun requestLeave() {
        DoctorLeaveRequestSheet.show(this, selectedDate, leaveRequests) { load() }
    }

    private enum class Mode { DAY, WEEK, MONTH }
}
