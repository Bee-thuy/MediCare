package com.example.medicare.user.appointment

import android.os.Bundle
import android.view.Gravity
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.medicare.R
import com.example.medicare.data.appointment.AppointmentRepository
import com.example.medicare.data.appointment.BookingSlotRepository
import com.example.medicare.data.doctor.DoctorManagementRepository
import com.example.medicare.data.review.DoctorReviewRepository
import com.example.medicare.data.schedule.ScheduleRepository
import com.example.medicare.databinding.FragmentPatientBookingBinding
import com.example.medicare.databinding.ItemPatientBookingDoctorBinding
import com.example.medicare.model.Doctor
import com.example.medicare.model.DoctorSchedule
import com.example.medicare.user.PatientHomeActivity
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import com.google.firebase.firestore.ListenerRegistration
import kotlin.math.max

/** Four booking steps inside the patient shell. Firebase remains the source of doctors and shifts. */
class PatientBookingFragment : Fragment() {
    private var _binding: FragmentPatientBookingBinding? = null
    private val binding get() = _binding!!
    private val scheduleRepository = ScheduleRepository()
    private val appointmentRepository = AppointmentRepository()
    private val bookingSlotRepository = BookingSlotRepository()
    private var availabilityListener: ListenerRegistration? = null
    private var reservedSlots: Set<String> = emptySet()
    private var availabilityLoaded = false
    private var availabilityError = false
    private var step = 1
    private var doctorId = ""
    private var doctorName = ""
    private var specialty = ""
    private var doctors: List<Doctor> = emptyList()
    private var ratings: Map<String, Double> = emptyMap()
    private var schedules: List<DoctorSchedule> = emptyList()
    private var selectedSchedule: DoctorSchedule? = null
    private var pendingScheduleId = ""
    private var selectedStartTime: String? = null
    private var bookingInFlight = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        doctorId = savedInstanceState?.getString(STATE_DOCTOR_ID)
            ?: arguments?.getString(ARG_DOCTOR_ID).orEmpty()
        doctorName = savedInstanceState?.getString(STATE_DOCTOR_NAME)
            ?: arguments?.getString(ARG_DOCTOR_NAME).orEmpty()
        specialty = savedInstanceState?.getString(STATE_SPECIALTY)
            ?: arguments?.getString(ARG_SPECIALTY).orEmpty()
        step = savedInstanceState?.getInt(STATE_STEP) ?: if (doctorId.isBlank()) 1 else 2
        pendingScheduleId = savedInstanceState?.getString(STATE_SCHEDULE_ID).orEmpty()
        selectedStartTime = savedInstanceState?.getString(STATE_START_TIME)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentPatientBookingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        binding.patientBookingReason.setText(state?.getString(STATE_REASON).orEmpty())
        binding.patientBookingBack.setOnClickListener { if (step > 1) { step--; renderStep() } }
        binding.patientBookingNext.setOnClickListener { advance() }
        loadDoctors()
        DoctorReviewRepository().getAll { result ->
            if (_binding == null) return@getAll
            ratings = result.getOrDefault(emptyList()).groupBy { it.doctorId }
                .mapValues { (_, reviews) -> reviews.map { it.rating }.average() }
            renderDoctors()
        }
        if (doctorId.isNotBlank()) loadSchedules(doctorId)
        renderStep()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_STEP, step)
        outState.putString(STATE_DOCTOR_ID, doctorId)
        outState.putString(STATE_DOCTOR_NAME, doctorName)
        outState.putString(STATE_SPECIALTY, specialty)
        outState.putString(STATE_SCHEDULE_ID, selectedSchedule?.id ?: pendingScheduleId)
        outState.putString(STATE_START_TIME, selectedStartTime)
        if (_binding != null) outState.putString(STATE_REASON, binding.patientBookingReason.text.toString())
        super.onSaveInstanceState(outState)
    }

    private fun loadDoctors() {
        DoctorManagementRepository(requireContext()).getActiveDoctors { result ->
            if (_binding == null) return@getActiveDoctors
            result.onSuccess { loaded ->
                doctors = loaded.sortedBy { it.fullName }
                if (doctorId.isBlank() && doctors.isNotEmpty()) {
                    doctorId = doctors.first().id
                    doctorName = doctors.first().fullName
                    specialty = doctors.first().specialty
                    loadSchedules(doctorId)
                }
                renderDoctors()
            }.onFailure {
                binding.patientBookingDoctorsEmpty.visibility = View.VISIBLE
                binding.patientBookingDoctorsEmpty.text = it.message ?: "Không tải được danh sách bác sĩ."
            }
        }
    }

    private fun renderDoctors() {
        if (_binding == null) return
        binding.patientBookingDoctors.removeAllViews()
        binding.patientBookingDoctorsEmpty.visibility = if (doctors.isEmpty()) View.VISIBLE else View.GONE
        binding.patientBookingDoctorsEmpty.text = "Chưa có bác sĩ đang hoạt động."
        doctors.forEach(::addDoctor)
    }

    private fun addDoctor(doctor: Doctor) {
        val row = ItemPatientBookingDoctorBinding.inflate(layoutInflater, binding.patientBookingDoctors, false)
        row.root.isSelected = doctor.id == doctorId
        row.patientBookingDoctorRowName.text = "BS. ${doctor.fullName}"
        val rating = ratings[doctor.id]?.let { " · ⭐ ${"%.1f".format(it)}" }.orEmpty()
        row.patientBookingDoctorRowInfo.text = "${doctor.specialty}$rating"
        row.patientBookingDoctorRowSelected.text = if (row.root.isSelected) "✅" else "○"
        row.root.setOnClickListener { selectDoctor(doctor) }
        binding.patientBookingDoctors.addView(row.root)
    }

    private fun selectDoctor(doctor: Doctor) {
        doctorId = doctor.id
        doctorName = doctor.fullName
        specialty = doctor.specialty
        selectedSchedule = null
        pendingScheduleId = ""
        selectedStartTime = null
        binding.patientBookingSchedule.setText("")
        binding.patientBookingSlots.removeAllViews()
        binding.patientBookingSlotsEmpty.text = "Hãy chọn lịch làm việc để xem giờ trống."
        loadSchedules(doctorId)
        renderDoctors()
    }

    private fun loadSchedules(id: String) {
        availabilityListener?.remove()
        availabilityLoaded = false
        availabilityError = false
        reservedSlots = emptySet()
        availabilityListener = bookingSlotRepository.observeReservedForDoctor(id) { result ->
            if (_binding == null || doctorId != id) return@observeReservedForDoctor
            result.onSuccess {
                reservedSlots = it
                availabilityLoaded = true
                availabilityError = false
                if (selectedSchedule?.let { schedule -> selectedStartTime?.let { time ->
                        BookingSlotRepository.id(schedule.id, time) in reservedSlots } } == true)
                    selectedStartTime = null
                selectedSchedule?.let(::renderSlots)
            }.onFailure {
                availabilityLoaded = false
                availabilityError = true
                selectedSchedule?.let(::renderSlots)
            }
        }
        scheduleRepository.getOpenSchedulesForDoctor(id) { result ->
            if (_binding == null || doctorId != id) return@getOpenSchedulesForDoctor
            result.onSuccess { loaded ->
                schedules = loaded
                val restored = loaded.find { it.id == pendingScheduleId }
                if (restored != null) {
                    selectSchedule(restored, restoreTime = true)
                    renderStep()
                } else if (loaded.isNotEmpty()) selectSchedule(loaded.first(), restoreTime = false)
                else binding.patientBookingSlotsEmpty.text =
                    "Bác sĩ chưa có lịch làm việc đang mở. Hãy chọn bác sĩ khác."
                renderWeek()
            }.onFailure { binding.patientBookingSlotsEmpty.text =
                it.message ?: "Không tải được lịch làm việc của bác sĩ." }
        }
    }

    private fun selectSchedule(schedule: DoctorSchedule, restoreTime: Boolean) {
        selectedSchedule = schedule
        pendingScheduleId = schedule.id
        if (!restoreTime || selectedStartTime !in timeSlots(schedule).map { it.first }) selectedStartTime = null
        val label = "${schedule.workDate} · ${schedule.shiftName} (${schedule.startTime} – ${schedule.endTime})"
        binding.patientBookingSchedule.setText(label, false)
        renderSlots(schedule)
        renderWeek()
    }

    private fun renderWeek() {
        if (_binding == null) return
        binding.patientBookingWeek.removeAllViews()
        val available = schedules.mapNotNull { runCatching { LocalDate.parse(it.workDate) }.getOrNull() }.distinct().sorted()
        val first = available.firstOrNull() ?: LocalDate.now()
        val dates = (0L..6L).map { first.plusDays(it) } + available.filter { it.isAfter(first.plusDays(6)) }
        val formatter = DateTimeFormatter.ofPattern("E", java.util.Locale.forLanguageTag("vi"))
        dates.distinct().forEach { date ->
            val shifts = schedules.filter { it.workDate == date.toString() }
            val active = selectedSchedule?.workDate == date.toString()
            val day = TextView(requireContext()).apply {
                text = "${date.format(formatter).uppercase()}\n${date.dayOfMonth}"
                textSize = 10f
                gravity = Gravity.CENTER
                isSelected = active
                setTextColor(requireContext().getColor(if (active) R.color.white else R.color.manager_muted))
                setBackgroundResource(R.drawable.patient_booking_day_background)
                alpha = if (shifts.isEmpty()) .55f else 1f
                if (shifts.isNotEmpty()) setOnClickListener {
                    selectSchedule(shifts.first(), restoreTime = false)
                }
            }
            binding.patientBookingWeek.addView(day, LinearLayout.LayoutParams(dp(44), dp(54)).apply { marginEnd = dp(4) })
        }
    }

    private fun renderSlots(schedule: DoctorSchedule) {
        binding.patientBookingSlots.removeAllViews()
        if (!availabilityLoaded) {
            binding.patientBookingSlotsEmpty.visibility = View.VISIBLE
            binding.patientBookingSlotsEmpty.text = if (availabilityError)
                "Không kiểm tra được giờ đã đặt. Vui lòng thử lại." else "Đang kiểm tra giờ đã đặt…"
            return
        }
        val shifts = schedules.filter { it.workDate == schedule.workDate }.sortedBy { it.startTime }
        val total = shifts.sumOf { shift -> timeSlots(shift).count { (start, _) ->
            start !in shift.lockedSlots && BookingSlotRepository.id(shift.id, start) !in reservedSlots } }
        val hasSlots = shifts.any { timeSlots(it).isNotEmpty() }
        binding.patientBookingShiftSummary.text = when {
            shifts.isEmpty() -> "Chưa có ca làm việc trong ngày."
            shifts.size == 1 -> "${shifts.first().shiftName} · ${shifts.first().startTime}–${shifts.first().endTime}"
            else -> "${shifts.size} ca làm việc trong ngày · $total khung giờ trống"
        }
        binding.patientBookingSlotsEmpty.visibility = if (total == 0) View.VISIBLE else View.GONE
        if (total == 0) binding.patientBookingSlotsEmpty.text = if (hasSlots)
            "Các giờ trong ngày đã được đặt hoặc khóa." else "Không có khung giờ khả dụng trong ngày này."
        shifts.forEach { shift ->
            val slots = timeSlots(shift)
            if (slots.isEmpty()) return@forEach
            binding.patientBookingSlots.addView(TextView(requireContext()).apply {
                text = "${shift.shiftName}  ·  ${shift.startTime}–${shift.endTime}"
                textSize = 12f
                setTextColor(requireContext().getColor(R.color.manager_ink))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10); bottomMargin = dp(8) })
            slots.chunked(3).forEach { pair ->
            val line = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
            pair.forEach { (start, _) ->
                val booked = BookingSlotRepository.id(shift.id, start) in reservedSlots
                val locked = start in shift.lockedSlots
                val slot = TextView(requireContext()).apply {
                    text = when { locked -> "$start\nĐã khóa"; else -> start }
                    textSize = 11f
                    gravity = Gravity.CENTER
                    isSelected = !booked && !locked && shift.id == selectedSchedule?.id && start == selectedStartTime
                    setTextColor(requireContext().getColor(if (isSelected) R.color.white else if (booked) R.color.manager_red else R.color.manager_ink))
                    setBackgroundResource(when {
                        booked -> R.drawable.patient_booking_slot_booked_background
                        locked -> R.drawable.patient_booking_slot_locked_background
                        else -> R.drawable.patient_booking_slot_background
                    })
                    if (!booked && !locked) setOnClickListener {
                        selectedSchedule = shift
                        pendingScheduleId = shift.id
                        selectedStartTime = start
                        renderSlots(shift)
                    }
                }
                line.addView(slot, LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                    marginEnd = dp(6)
                    bottomMargin = dp(7)
                })
            }
            repeat(3 - pair.size) {
                line.addView(View(requireContext()), LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                    marginEnd = dp(6)
                    bottomMargin = dp(7)
                })
            }
            binding.patientBookingSlots.addView(line)
            }
        }
    }

    private fun advance() {
        when (step) {
            1 -> {
                if (doctorId.isBlank()) Toast.makeText(requireContext(), "Vui lòng chọn bác sĩ.", Toast.LENGTH_SHORT).show()
                else { step = 2; renderStep() }
            }
            2 -> {
                if (!availabilityLoaded || selectedSchedule == null || selectedStartTime == null ||
                    selectedStartTime in selectedSchedule?.lockedSlots.orEmpty() ||
                    selectedSchedule?.let { BookingSlotRepository.id(it.id, selectedStartTime.orEmpty()) in reservedSlots } == true) {
                    Toast.makeText(requireContext(), "Vui lòng chọn ngày và khung giờ khám.", Toast.LENGTH_SHORT).show()
                } else { step = 3; renderStep() }
            }
            3 -> {
                if (binding.patientBookingReason.text.toString().trim().isBlank()) {
                    binding.patientBookingReason.error = "Vui lòng nhập lý do khám."
                } else { step = 4; renderStep() }
            }
            4 -> submit()
        }
    }

    private fun renderStep() {
        if (_binding == null) return
        val labels = listOf("Bác sĩ", "Ngày & giờ", "Lý do", "Xác nhận")
        binding.patientBookingStepLabel.text = "Bước $step/4 · ${labels[step - 1]}"
        listOf(binding.patientBookingCircle1, binding.patientBookingCircle2,
            binding.patientBookingCircle3, binding.patientBookingCircle4).forEachIndexed { index, circle ->
            circle.isSelected = index + 1 <= step
            circle.text = if (index + 1 < step) "✓" else "${index + 1}"
            circle.setTextColor(requireContext().getColor(if (circle.isSelected) R.color.white else R.color.manager_muted))
        }
        binding.patientBookingDoctorStep.visibility = if (step == 1) View.VISIBLE else View.GONE
        binding.patientBookingDateStep.visibility = if (step == 2) View.VISIBLE else View.GONE
        binding.patientBookingReasonStep.visibility = if (step == 3) View.VISIBLE else View.GONE
        binding.patientBookingConfirmStep.visibility = if (step == 4) View.VISIBLE else View.GONE
        binding.patientBookingBack.visibility = if (step == 1) View.GONE else View.VISIBLE
        binding.patientBookingNext.text = if (step == 4) "📨 Gửi yêu cầu" else "Tiếp tục →"
        binding.patientBookingDoctorName.text = "BS. $doctorName"
        binding.patientBookingDoctorSpecialty.text = specialty
        val schedule = selectedSchedule
        if (schedule != null) {
            val time = selectedStartTime.orEmpty()
            val date = runCatching { LocalDate.parse(schedule.workDate)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrDefault(schedule.workDate)
            binding.patientBookingReasonSummary.removeAllViews()
            addKeyValue(binding.patientBookingReasonSummary, "Bác sĩ", "BS. $doctorName")
            addKeyValue(binding.patientBookingReasonSummary, "Ngày khám", date)
            addKeyValue(binding.patientBookingReasonSummary, "Giờ khám", time, last = true)
            binding.patientBookingConfirmDoctorName.text = "BS. $doctorName"
            binding.patientBookingConfirmSpecialty.text = specialty
            binding.patientBookingConfirmation.removeAllViews()
            addKeyValue(binding.patientBookingConfirmation, "Ngày khám", date)
            addKeyValue(binding.patientBookingConfirmation, "Giờ khám", time)
            addKeyValue(binding.patientBookingConfirmation, "Lý do", binding.patientBookingReason.text.toString().trim())
            addKeyValue(binding.patientBookingConfirmation, "Trạng thái sau gửi", "Chờ duyệt", last = true, status = true)
        }
        binding.patientBookingScroll.post { _binding?.patientBookingScroll?.scrollTo(0, 0) }
    }

    private fun addKeyValue(container: LinearLayout, label: String, value: String,
                            last: Boolean = false, status: Boolean = false) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(TextView(requireContext()).apply {
            text = label
            textSize = 11f
            setTextColor(requireContext().getColor(R.color.manager_muted))
        }, LinearLayout.LayoutParams(0, dp(32), 1f).apply { gravity = Gravity.CENTER_VERTICAL })
        row.addView(TextView(requireContext()).apply {
            text = value
            textSize = 11f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setTextColor(requireContext().getColor(if (status) R.color.manager_orange else R.color.manager_ink))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            if (status) {
                setBackgroundResource(R.drawable.manager_appointment_pending_background)
                setPadding(dp(7), dp(4), dp(7), dp(4))
            }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT))
        container.addView(row)
        if (!last) container.addView(View(requireContext()).apply {
            setBackgroundColor(Color.rgb(237, 240, 246))
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)))
    }

    private fun submit() {
        if (bookingInFlight) return
        val schedule = selectedSchedule ?: return
        val start = selectedStartTime ?: return
        val reason = binding.patientBookingReason.text.toString().trim()
        val end = timeSlots(schedule).firstOrNull { it.first == start }?.second ?: return
        bookingInFlight = true
        binding.patientBookingNext.isEnabled = false
        appointmentRepository.createAppointment(schedule, start, end, reason) { result ->
            if (_binding == null) return@createAppointment
            bookingInFlight = false
            binding.patientBookingNext.isEnabled = true
            result.onSuccess {
                Toast.makeText(requireContext(), "Đã gửi yêu cầu đặt lịch. Chờ Admin xác nhận.", Toast.LENGTH_LONG).show()
                (activity as? PatientHomeActivity)?.showTab(PatientHomeActivity.TAB_HOME)
                startActivity(android.content.Intent(requireContext(), MyAppointmentsActivity::class.java))
            }.onFailure {
                Toast.makeText(requireContext(), it.message ?: "Không gửi được yêu cầu đặt lịch.", Toast.LENGTH_LONG).show()
                loadSchedules(doctorId)
            }
        }
    }

    private fun timeSlots(schedule: DoctorSchedule): List<Pair<String, String>> = runCatching {
        val (startHour, startMinute) = schedule.startTime.split(":").map(String::toInt)
        val (endHour, endMinute) = schedule.endTime.split(":").map(String::toInt)
        val end = endHour * 60 + endMinute
        val duration = max(schedule.appointmentDurationMinutes, 1)
        buildList {
            var current = startHour * 60 + startMinute
            while (current + duration <= end) {
                val start = formatTime(current)
                val finish = formatTime(current + duration)
                if (schedule.workDate != LocalDate.now().toString() ||
                    LocalTime.parse(finish).isAfter(LocalTime.now())) add(start to finish)
                current += duration
            }
        }
    }.getOrDefault(emptyList())

    private fun formatTime(totalMinutes: Int) = "%02d:%02d".format(totalMinutes / 60, totalMinutes % 60)
    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()

    override fun onDestroyView() {
        availabilityListener?.remove()
        availabilityListener = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_DOCTOR_ID = "doctor_id"
        private const val ARG_DOCTOR_NAME = "doctor_name"
        private const val ARG_SPECIALTY = "specialty"
        private const val STATE_STEP = "step"
        private const val STATE_DOCTOR_ID = "selected_doctor_id"
        private const val STATE_DOCTOR_NAME = "selected_doctor_name"
        private const val STATE_SPECIALTY = "selected_specialty"
        private const val STATE_SCHEDULE_ID = "selected_schedule_id"
        private const val STATE_START_TIME = "selected_start_time"
        private const val STATE_REASON = "reason"

        fun newInstance(doctor: Doctor? = null) = PatientBookingFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_DOCTOR_ID, doctor?.id.orEmpty())
                putString(ARG_DOCTOR_NAME, doctor?.fullName.orEmpty())
                putString(ARG_SPECIALTY, doctor?.specialty.orEmpty())
            }
        }
    }
}
