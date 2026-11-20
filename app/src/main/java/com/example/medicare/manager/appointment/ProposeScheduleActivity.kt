package com.example.medicare.manager.appointment

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.GridLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.medicare.R
import com.example.medicare.data.appointment.AppointmentRepository
import com.example.medicare.data.schedule.ScheduleRepository
import com.example.medicare.model.AppointmentStatus
import java.time.LocalTime
import com.example.medicare.databinding.ActivityProposeScheduleBinding

/** Lets a manager select an available replacement time and notify the patient. */
class ProposeScheduleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProposeScheduleBinding
    private var selectedTime: String? = null
    private var appointmentDate: String = ""
    private val appointments = AppointmentRepository()
    private var availableTimes: Set<String> = emptySet()
    private var durationMinutes = 30

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProposeScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        appointmentDate = intent.getStringExtra(EXTRA_DATE).orEmpty()
        binding.proposeScheduleInfo.text = "Đang tải khung giờ còn trống..."
        binding.proposeScheduleBack.setOnClickListener { finish() }
        binding.proposeScheduleCancel.setOnClickListener { finish() }
        binding.proposeSlotGrid.removeAllViews()
        binding.proposeScheduleSend.isEnabled = false
        loadAvailableTimes()
        binding.proposeScheduleSend.setOnClickListener { sendProposal() }
    }

    private fun loadAvailableTimes() {
        val scheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID).orEmpty()
        if (scheduleId.isBlank()) {
            binding.proposeScheduleInfo.text = "Không tìm thấy ca làm việc của lịch khám này."
            return
        }
        ScheduleRepository().getSchedule(scheduleId) { scheduleResult ->
            if (isFinishing || isDestroyed) return@getSchedule
            scheduleResult.onSuccess { schedule ->
                if (schedule.isLocked) {
                    binding.proposeScheduleInfo.text = "Ca làm việc đang bị khóa."
                    return@onSuccess
                }
                appointmentDate = schedule.workDate
                durationMinutes = schedule.appointmentDurationMinutes.coerceAtLeast(1)
                appointments.getAppointmentsForDoctor(schedule.doctorId) { result ->
                    if (isFinishing || isDestroyed) return@getAppointmentsForDoctor
                    result.onSuccess { booked ->
                        val occupied = booked.filter { it.id != intent.getStringExtra(EXTRA_APPOINTMENT_ID) &&
                            it.appointmentDate == schedule.workDate && it.status in setOf(
                                AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED,
                                AppointmentStatus.ARRIVED, AppointmentStatus.IN_PROGRESS)
                        }.map { it.startTime }.toSet()
                        val start = runCatching { LocalTime.parse(schedule.startTime) }.getOrNull()
                        val end = runCatching { LocalTime.parse(schedule.endTime) }.getOrNull()
                        if (start == null || end == null) {
                            binding.proposeScheduleInfo.text = "Giờ ca làm việc không hợp lệ."
                            return@onSuccess
                        }
                        val times = generateSequence(start) { it.plusMinutes(durationMinutes.toLong()) }
                            .takeWhile { it.plusMinutes(durationMinutes.toLong()) <= end }
                            .map(LocalTime::toString)
                            .filter { it !in occupied && it !in schedule.lockedSlots &&
                                it != intent.getStringExtra(EXTRA_CURRENT_TIME)?.substringBefore(" - ") }
                            .take(24).toList()
                        availableTimes = times.toSet()
                        renderTimes(times)
                    }.onFailure { binding.proposeScheduleInfo.text = it.message ?: "Không tải được lịch đã đặt." }
                }
            }.onFailure { binding.proposeScheduleInfo.text = it.message ?: "Không tải được ca làm việc." }
        }
    }

    private fun renderTimes(times: List<String>) {
        binding.proposeSlotGrid.removeAllViews()
        binding.proposeScheduleInfo.text = if (times.isEmpty()) "Không còn khung giờ trống trong ca này."
            else "Các giờ còn trống ngày $appointmentDate"
        binding.proposeScheduleSend.isEnabled = times.isNotEmpty()
        times.forEach { time ->
            val slot = TextView(this).apply {
                text = time
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                setTextColor(getColor(R.color.manager_ink))
                setBackgroundResource(R.drawable.manager_time_option_background)
                setOnClickListener { selectTime(this) }
            }
            binding.proposeSlotGrid.addView(slot, GridLayout.LayoutParams().apply {
                width = 0
                height = dp(48)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(3), dp(4), dp(3), dp(4))
            })
        }
    }

    private fun selectTime(slot: TextView) {
        selectedTime = slot.text.toString()
        (0 until binding.proposeSlotGrid.childCount).map(binding.proposeSlotGrid::getChildAt)
            .filterIsInstance<TextView>().forEach { option ->
            val selected = option == slot
            option.isSelected = selected
            option.setTextColor(ContextCompat.getColor(this, if (selected) R.color.white else R.color.manager_ink))
        }
    }

    private fun sendProposal() {
        val time = selectedTime
        if (time == null) {
            Toast.makeText(this, R.string.manager_select_available_time, Toast.LENGTH_SHORT).show()
            return
        }
        if (time !in availableTimes) {
            Toast.makeText(this, "Giờ đã chọn không còn khả dụng. Hãy tải lại lịch.", Toast.LENGTH_SHORT).show()
            return
        }
        val note = binding.proposeScheduleNote.text?.toString()?.trim().orEmpty()
        val appointmentId = intent.getStringExtra(EXTRA_APPOINTMENT_ID).orEmpty()
        if (appointmentId.isBlank()) {
            Toast.makeText(this, "Không tìm thấy lịch hẹn cần đổi.", Toast.LENGTH_SHORT).show()
            return
        }
        val endTime = addMinutes(time, durationMinutes)
        binding.proposeScheduleSend.isEnabled = false
        appointments.rescheduleAppointment(appointmentId, time, endTime, note) { result ->
            binding.proposeScheduleSend.isEnabled = true
            result.onSuccess {
                val message = "Đã tạo lịch mới $appointmentDate · $time - $endTime và gửi thông báo trong ứng dụng."
                startActivity(Intent(this, PatientNotificationActivity::class.java)
                    .putExtra(PatientNotificationActivity.EXTRA_SHOW_SENT_STATE, true)
                    .putExtra(PatientNotificationActivity.EXTRA_SENT_CONTENT, message))
                finish()
            }.onFailure { error ->
                Toast.makeText(this, error.message ?: "Không thể đổi lịch hẹn.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun addMinutes(start: String, duration: Int): String {
        val parts = start.split(":"); val total = parts[0].toInt() * 60 + parts[1].toInt() + duration
        return "%02d:%02d".format(total / 60, total % 60)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()

    companion object {
        const val EXTRA_SCHEDULE_ID = "propose_schedule_schedule_id"
        const val EXTRA_DATE = "propose_schedule_date"
        const val EXTRA_APPOINTMENT_ID = "propose_schedule_appointment_id"
        const val EXTRA_PATIENT_ID = "propose_schedule_patient_id"
        const val EXTRA_CURRENT_TIME = "propose_schedule_current_time"
    }
}
