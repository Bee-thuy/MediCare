package com.example.medicare.manager.work_schedule

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.medicare.databinding.ActivityWorkScheduleDetailBinding

class WorkScheduleDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWorkScheduleDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityWorkScheduleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        val doctor = intent.getStringExtra(EXTRA_DOCTOR_NAME) ?: "BS. Nguyễn Văn A"
        val specialty = intent.getStringExtra(EXTRA_SPECIALTY) ?: "Tim mạch"
        binding.detailScheduleInitials.text = initialsOf(doctor)
        binding.detailScheduleDoctorName.text = doctor
        binding.detailScheduleDoctorInfo.text = "Nam · 35 tuổi · 0123 456 789"
        binding.detailScheduleSpecialty.text = specialty
        binding.detailScheduleBack.setOnClickListener { finish() }
        binding.detailScheduleEdit.setOnClickListener {
            startActivity(Intent(this, AssignDoctorScheduleActivity::class.java))
        }
        binding.detailScheduleLock.setOnClickListener {
            startActivity(Intent(this, LeaveLockScheduleActivity::class.java))
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
        const val EXTRA_DOCTOR_NAME = "work_schedule_doctor_name"
        const val EXTRA_SPECIALTY = "work_schedule_specialty"
    }
}
