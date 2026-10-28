package com.example.medicare.manager.user

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.medicare.R
import com.example.medicare.databinding.ActivityDetailDoctorBinding
import com.example.medicare.databinding.ItemDoctorScheduleSlotBinding

/** Shows a mock daily availability board for the doctor selected from a conflicted booking. */
class DetailDoctorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailDoctorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDetailDoctorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bindDoctor()
        binding.detailDoctorBack.setOnClickListener { finish() }
    }

    private fun bindDoctor() {
        val doctorName = intent.getStringExtra(EXTRA_DOCTOR_NAME) ?: "BS. Nguyễn Văn A"
        val specialty = intent.getStringExtra(EXTRA_SPECIALTY) ?: "Tim mạch"
        binding.detailDoctorInitials.text = initialsOf(doctorName)
        binding.detailDoctorName.text = doctorName
        binding.detailDoctorSpecialty.text = getString(R.string.manager_doctor_specialty_format, specialty)
        binding.detailDoctorDate.text = intent.getStringExtra(EXTRA_DATE) ?: "26/05/2025 (Thứ 2)"

        val slots = listOf(
            "08:00 - 08:30" to false,
            "08:30 - 09:00" to true,
            "09:00 - 09:30" to false,
            "09:30 - 10:00" to false,
            "10:00 - 10:30" to false,
            "10:30 - 11:00" to false,
            "11:00 - 11:30" to false,
        )
        slots.forEach { (time, booked) ->
            val row = ItemDoctorScheduleSlotBinding.inflate(layoutInflater, binding.detailDoctorScheduleList, false)
            row.doctorSlotTime.text = time
            row.doctorSlotStatus.text = getString(if (booked) R.string.manager_slot_booked else R.string.manager_slot_available)
            if (booked) {
                row.doctorSlotTime.setBackgroundResource(R.drawable.manager_conflict_alert_background)
                row.doctorSlotStatus.setBackgroundResource(R.drawable.manager_status_inactive_background)
                row.doctorSlotTime.setTextColor(ContextCompat.getColor(this, R.color.manager_red))
                row.doctorSlotStatus.setTextColor(ContextCompat.getColor(this, R.color.manager_red))
            } else {
                row.root.setOnClickListener {
                    Toast.makeText(this, getString(R.string.manager_slot_suggestion, time), Toast.LENGTH_SHORT).show()
                }
            }
            binding.detailDoctorScheduleList.addView(row.root)
        }
    }

    private fun initialsOf(name: String): String {
        return name.removePrefix("BS. ")
            .split(" ")
            .filter { it.isNotBlank() }
            .takeLast(2)
            .joinToString("") { it.first().toString() }
    }

    companion object {
        const val EXTRA_DOCTOR_NAME = "doctor_name"
        const val EXTRA_SPECIALTY = "doctor_specialty"
        const val EXTRA_DATE = "doctor_schedule_date"
    }
}
