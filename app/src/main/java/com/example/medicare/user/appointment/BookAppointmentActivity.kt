package com.example.medicare.user.appointment

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.view.isVisible
import com.example.medicare.R
import com.example.medicare.data.appointment.AppointmentRepository
import com.example.medicare.data.appointment.BookingSlotRepository
import com.example.medicare.data.schedule.ScheduleRepository
import com.example.medicare.databinding.ActivityBookAppointmentBinding
import com.example.medicare.model.DoctorSchedule
import com.google.firebase.firestore.ListenerRegistration
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.max

class BookAppointmentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBookAppointmentBinding
    private val scheduleRepository = ScheduleRepository()
    private val appointmentRepository = AppointmentRepository()
    private val bookingSlotRepository = BookingSlotRepository()
    private var availabilityListener: ListenerRegistration? = null
    private var reservedSlots: Set<String> = emptySet()
    private var availabilityLoaded = false
    private var schedules: List<DoctorSchedule> = emptyList()
    private var selectedSchedule: DoctorSchedule? = null
    private var selectedStartTime: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookAppointmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val doctorId = intent.getStringExtra(EXTRA_DOCTOR_ID).orEmpty()
        binding.bookAppointmentDoctorName.text = intent.getStringExtra(EXTRA_DOCTOR_NAME).orEmpty()
        binding.bookAppointmentSpecialty.text = intent.getStringExtra(EXTRA_SPECIALTY).orEmpty()
        binding.bookAppointmentBack.setOnClickListener { finish() }
        binding.bookAppointmentSchedule.setOnClickListener { binding.bookAppointmentSchedule.showDropDown() }
        binding.bookAppointmentSchedule.setOnItemClickListener { _, _, position, _ -> selectSchedule(schedules[position]) }
        binding.bookAppointmentConfirm.setOnClickListener { bookAppointment() }
        availabilityListener = bookingSlotRepository.observeReservedForDoctor(doctorId) { result ->
            result.onSuccess {
                reservedSlots = it
                availabilityLoaded = true
                if (selectedSchedule?.let { schedule -> selectedStartTime?.let { time ->
                        BookingSlotRepository.id(schedule.id, time) in reservedSlots } } == true)
                    selectedStartTime = null
                selectedSchedule?.let(::renderSlots)
            }.onFailure {
                availabilityLoaded = false
                binding.bookAppointmentSlotsEmpty.text = it.message ?: "Không kiểm tra được giờ đã đặt."
                binding.bookAppointmentSlots.removeAllViews()
            }
        }
        loadSchedules(doctorId)
    }

    private fun loadSchedules(doctorId: String) {
        scheduleRepository.getOpenSchedulesForDoctor(doctorId) { result ->
            result.onSuccess { loadedSchedules ->
                schedules = loadedSchedules
                binding.bookAppointmentSchedule.setAdapter(
                    ArrayAdapter(
                        this,
                        android.R.layout.simple_list_item_1,
                        schedules.map { "${it.workDate} · ${it.shiftName} (${it.startTime} - ${it.endTime})" },
                    ),
                )
                if (schedules.isEmpty()) {
                    binding.bookAppointmentSlotsEmpty.text = "Bác sĩ chưa có lịch làm việc đang mở."
                }
            }.onFailure { showError(it) }
        }
    }

    private fun selectSchedule(schedule: DoctorSchedule) {
        selectedSchedule = schedule
        selectedStartTime = null
        renderSlots(schedule)
    }

    private fun renderSlots(schedule: DoctorSchedule) {
        binding.bookAppointmentSlots.removeAllViews()
        if (!availabilityLoaded) {
            binding.bookAppointmentSlotsEmpty.isVisible = true
            binding.bookAppointmentSlotsEmpty.text = "Đang kiểm tra giờ đã đặt…"
            return
        }
        val slots = timeSlots(schedule.startTime, schedule.endTime, schedule.appointmentDurationMinutes)
            .filter { schedule.workDate != LocalDate.now().toString() || LocalTime.parse(it.second).isAfter(LocalTime.now()) }
        binding.bookAppointmentSlotsEmpty.isVisible = slots.isEmpty()
        binding.bookAppointmentSlotsEmpty.text = if (slots.isEmpty()) "Không có khung giờ khả dụng trong lịch này." else ""
        slots.forEach { (start, end) ->
            val booked = BookingSlotRepository.id(schedule.id, start) in reservedSlots
            val locked = start in schedule.lockedSlots
            val slot = TextView(this).apply {
                text = "$start - $end" + if (locked) " · Đã khóa" else ""
                setTextColor(getColor(if (booked) R.color.manager_red else if (locked) R.color.manager_muted else R.color.manager_blue))
                textSize = 15f
                gravity = android.view.Gravity.CENTER
                setPadding(0, dp(12), 0, dp(12))
                setBackgroundResource(when {
                    booked -> R.drawable.patient_booking_slot_booked_background
                    locked -> R.drawable.patient_booking_slot_locked_background
                    else -> R.drawable.manager_card_background
                })
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { bottomMargin = dp(8) }
                if (!booked && !locked) setOnClickListener {
                    selectedStartTime = start
                    binding.bookAppointmentSlots.children.forEach { child -> child.isSelected = false }
                    isSelected = true
                    setBackgroundResource(R.drawable.manager_button_background)
                    setTextColor(getColor(R.color.white))
                }
            }
            binding.bookAppointmentSlots.addView(slot)
        }
    }

    private fun bookAppointment() {
        val schedule = selectedSchedule
        val start = selectedStartTime
        val reason = binding.bookAppointmentReason.text.toString().trim()
        binding.bookAppointmentSchedule.error = if (schedule == null) getString(R.string.manager_required_field) else null
        binding.bookAppointmentReason.error = if (reason.isBlank()) "Vui lòng nhập lý do khám." else null
        if (!availabilityLoaded || schedule == null || start == null || reason.isBlank() ||
            start in schedule?.lockedSlots.orEmpty() ||
            schedule?.let { BookingSlotRepository.id(it.id, start.orEmpty()) in reservedSlots } == true) {
            if (start == null && schedule != null) showError(IllegalStateException("Vui lòng chọn khung giờ khám."))
            return
        }
        val end = timeSlots(schedule.startTime, schedule.endTime, schedule.appointmentDurationMinutes)
            .firstOrNull { it.first == start }
            ?.second
            ?: return
        binding.bookAppointmentConfirm.isEnabled = false
        appointmentRepository.createAppointment(schedule, start, end, reason) { result ->
            binding.bookAppointmentConfirm.isEnabled = true
            result.onSuccess {
                Toast.makeText(this, "Đã gửi yêu cầu đặt lịch. Chờ Admin xác nhận.", Toast.LENGTH_LONG).show()
                finish()
            }.onFailure { showError(it); loadSchedules(schedule.doctorId) }
        }
    }

    override fun onDestroy() {
        availabilityListener?.remove()
        super.onDestroy()
    }

    private fun timeSlots(start: String, end: String, duration: Int): List<Pair<String, String>> {
        val safeDuration = max(duration, 1)
        val startMinute = minutes(start)
        val endMinute = minutes(end)
        return buildList {
            var current = startMinute
            while (current + safeDuration <= endMinute) {
                add(formatTime(current) to formatTime(current + safeDuration))
                current += safeDuration
            }
        }
    }

    private fun minutes(value: String): Int {
        val (hour, minute) = value.split(":").map(String::toInt)
        return hour * 60 + minute
    }

    private fun formatTime(totalMinutes: Int): String = "%02d:%02d".format(totalMinutes / 60, totalMinutes % 60)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun showError(error: Throwable) {
        Toast.makeText(this, error.message ?: getString(R.string.manager_save_failed), Toast.LENGTH_LONG).show()
    }

    companion object {
        const val EXTRA_DOCTOR_ID = "doctor_id"
        const val EXTRA_DOCTOR_NAME = "doctor_name"
        const val EXTRA_SPECIALTY = "doctor_specialty"
    }
}
