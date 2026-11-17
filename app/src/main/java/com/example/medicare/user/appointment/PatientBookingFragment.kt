package com.example.medicare.user.appointment

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.medicare.R
import com.example.medicare.data.appointment.AppointmentRepository
import com.example.medicare.data.doctor.DoctorManagementRepository
import com.example.medicare.data.schedule.ScheduleRepository
import com.example.medicare.databinding.FragmentPatientBookingBinding
import com.example.medicare.databinding.ItemUserDoctorBinding
import com.example.medicare.model.Doctor
import com.example.medicare.model.DoctorSchedule
import com.example.medicare.user.PatientHomeActivity
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.max

/** Four booking steps inside the patient shell. Firebase remains the source of doctors and shifts. */
class PatientBookingFragment : Fragment() {
    private var _binding: FragmentPatientBookingBinding? = null
    private val binding get() = _binding!!
    private val scheduleRepository = ScheduleRepository()
    private val appointmentRepository = AppointmentRepository()
    private var step = 1
    private var doctorId = ""
    private var doctorName = ""
    private var specialty = ""
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
        binding.patientBookingSchedule.setOnClickListener { binding.patientBookingSchedule.showDropDown() }
        binding.patientBookingSchedule.setOnItemClickListener { _, _, position, _ ->
            selectSchedule(schedules[position], restoreTime = false)
        }
        loadDoctors()
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
            binding.patientBookingDoctors.removeAllViews()
            result.onSuccess { doctors ->
                binding.patientBookingDoctorsEmpty.visibility = if (doctors.isEmpty()) View.VISIBLE else View.GONE
                binding.patientBookingDoctorsEmpty.text = "Chưa có bác sĩ đang hoạt động."
                doctors.sortedBy { it.fullName }.forEach(::addDoctor)
            }.onFailure {
                binding.patientBookingDoctorsEmpty.visibility = View.VISIBLE
                binding.patientBookingDoctorsEmpty.text = it.message ?: "Không tải được danh sách bác sĩ."
            }
        }
    }

    private fun addDoctor(doctor: Doctor) {
        val row = ItemUserDoctorBinding.inflate(layoutInflater, binding.patientBookingDoctors, false)
        row.userDoctorInitials.text = "👨‍⚕️"
        row.userDoctorName.text = "BS. ${doctor.fullName}"
        row.userDoctorSpecialty.text = doctor.specialty
        row.userDoctorExperience.text = doctor.experienceYears.takeIf(String::isNotBlank)
            ?.let { "$it năm kinh nghiệm" }.orEmpty()
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
        step = 2
        renderStep()
    }

    private fun loadSchedules(id: String) {
        scheduleRepository.getOpenSchedulesForDoctor(id) { result ->
            if (_binding == null || doctorId != id) return@getOpenSchedulesForDoctor
            result.onSuccess { loaded ->
                schedules = loaded
                binding.patientBookingSchedule.setAdapter(ArrayAdapter(requireContext(),
                    android.R.layout.simple_list_item_1,
                    loaded.map { "${it.workDate} · ${it.shiftName} (${it.startTime} – ${it.endTime})" }))
                val restored = loaded.find { it.id == pendingScheduleId }
                if (restored != null) {
                    selectSchedule(restored, restoreTime = true)
                    renderStep()
                }
                else if (loaded.isEmpty()) binding.patientBookingSlotsEmpty.text =
                    "Bác sĩ chưa có lịch làm việc đang mở. Hãy chọn bác sĩ khác."
            }.onFailure { binding.patientBookingSlotsEmpty.text =
                it.message ?: "Không tải được lịch làm việc của bác sĩ." }
        }
    }

    private fun selectSchedule(schedule: DoctorSchedule, restoreTime: Boolean) {
        selectedSchedule = schedule
        pendingScheduleId = schedule.id
        if (!restoreTime) selectedStartTime = null
        val label = "${schedule.workDate} · ${schedule.shiftName} (${schedule.startTime} – ${schedule.endTime})"
        binding.patientBookingSchedule.setText(label, false)
        renderSlots(schedule)
    }

    private fun renderSlots(schedule: DoctorSchedule) {
        binding.patientBookingSlots.removeAllViews()
        val slots = timeSlots(schedule)
        binding.patientBookingSlotsEmpty.visibility = if (slots.isEmpty()) View.VISIBLE else View.GONE
        if (slots.isEmpty()) binding.patientBookingSlotsEmpty.text = "Không có khung giờ khả dụng trong lịch này."
        slots.chunked(2).forEach { pair ->
            val line = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
            pair.forEach { (start, end) ->
                val slot = TextView(requireContext()).apply {
                    text = "$start – $end"
                    textSize = 11f
                    gravity = Gravity.CENTER
                    isSelected = start == selectedStartTime
                    setTextColor(requireContext().getColor(if (isSelected) R.color.white else R.color.patient_purple))
                    setBackgroundResource(R.drawable.patient_appointment_tab_background)
                    setOnClickListener {
                        selectedStartTime = start
                        renderSlots(schedule)
                    }
                }
                line.addView(slot, LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                    marginEnd = dp(6)
                    bottomMargin = dp(7)
                })
            }
            binding.patientBookingSlots.addView(line)
        }
    }

    private fun advance() {
        when (step) {
            1 -> {
                Toast.makeText(requireContext(), "Vui lòng chọn bác sĩ.", Toast.LENGTH_SHORT).show()
            }
            2 -> {
                if (selectedSchedule == null || selectedStartTime == null) {
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
            binding.patientBookingReasonSummary.text = "BS. $doctorName\n${schedule.workDate} · $time"
            binding.patientBookingConfirmation.text =
                "👨‍⚕️ BS. $doctorName\n$specialty\n\nNgày khám: ${schedule.workDate}\nGiờ khám: $time\nLý do: ${binding.patientBookingReason.text.toString().trim()}\nTrạng thái sau gửi: Chờ duyệt"
        }
        binding.patientBookingScroll.post { _binding?.patientBookingScroll?.scrollTo(0, 0) }
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
