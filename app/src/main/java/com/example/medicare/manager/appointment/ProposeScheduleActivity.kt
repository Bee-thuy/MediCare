package com.example.medicare.manager.appointment

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.medicare.R
import com.example.medicare.data.appointment.AppointmentRepository
import com.example.medicare.databinding.ActivityProposeScheduleBinding

/** Lets a manager select an available replacement time and notify the patient. */
class ProposeScheduleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProposeScheduleBinding
    private var selectedTime: String? = null
    private var appointmentDate: String = "26/05/2025"
    private val appointments = AppointmentRepository()

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

        appointmentDate = intent.getStringExtra(EXTRA_DATE) ?: appointmentDate
        binding.proposeScheduleInfo.text = getString(R.string.manager_available_slots_info, appointmentDate)
        binding.proposeScheduleBack.setOnClickListener { finish() }
        binding.proposeScheduleCancel.setOnClickListener { finish() }
        listOf(
            binding.proposeSlot0830,
            binding.proposeSlot0900,
            binding.proposeSlot0930,
            binding.proposeSlot1000,
            binding.proposeSlot1030,
            binding.proposeSlot1100,
            binding.proposeSlot1130,
        ).forEach { slot ->
            slot.setOnClickListener { selectTime(slot) }
        }
        binding.proposeScheduleSend.setOnClickListener { sendProposal() }
    }

    private fun selectTime(slot: TextView) {
        selectedTime = slot.text.toString()
        val slots = listOf(
            binding.proposeSlot0830,
            binding.proposeSlot0900,
            binding.proposeSlot0930,
            binding.proposeSlot1000,
            binding.proposeSlot1030,
            binding.proposeSlot1100,
            binding.proposeSlot1130,
        )
        slots.forEach { option ->
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
        val note = binding.proposeScheduleNote.text?.toString()?.trim().orEmpty()
        val appointmentId = intent.getStringExtra(EXTRA_APPOINTMENT_ID).orEmpty()
        if (appointmentId.isBlank()) {
            Toast.makeText(this, "Không tìm thấy lịch hẹn cần đổi.", Toast.LENGTH_SHORT).show()
            return
        }
        val endTime = addMinutes(time, durationFrom(intent.getStringExtra(EXTRA_CURRENT_TIME).orEmpty()))
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

    private fun durationFrom(value: String): Int {
        val parts = value.split("-").map { it.trim() }
        if (parts.size != 2) return 30
        fun minutes(time: String): Int = time.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        return runCatching { (minutes(parts[1]) - minutes(parts[0])).coerceAtLeast(1) }.getOrDefault(30)
    }

    private fun addMinutes(start: String, duration: Int): String {
        val parts = start.split(":"); val total = parts[0].toInt() * 60 + parts[1].toInt() + duration
        return "%02d:%02d".format(total / 60, total % 60)
    }

    companion object {
        const val EXTRA_DATE = "propose_schedule_date"
        const val EXTRA_APPOINTMENT_ID = "propose_schedule_appointment_id"
        const val EXTRA_PATIENT_ID = "propose_schedule_patient_id"
        const val EXTRA_CURRENT_TIME = "propose_schedule_current_time"
    }
}
