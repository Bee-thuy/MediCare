package com.example.medicare.manager.work_schedule

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.medicare.databinding.ActivityWorkScheduleBinding

/** Mock manager work-schedule list, opened from the replacement bottom-navigation tab. */
class WorkScheduleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWorkScheduleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityWorkScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        binding.workScheduleBack.setOnClickListener { finish() }
        binding.workScheduleWeekly.setOnClickListener {
            startActivity(Intent(this, WeeklyScheduleActivity::class.java))
        }
        binding.workScheduleAssign.setOnClickListener {
            startActivity(Intent(this, AssignDoctorScheduleActivity::class.java))
        }
        binding.workScheduleDoctorA.setOnClickListener { openDetail("BS. Nguyễn Văn A", "Tim mạch") }
        binding.workScheduleDoctorB.setOnClickListener { openDetail("BS. Trần Thị B", "Nội khoa") }
    }

    private fun openDetail(doctorName: String, specialty: String) {
        startActivity(
            Intent(this, WorkScheduleDetailActivity::class.java)
                .putExtra(WorkScheduleDetailActivity.EXTRA_DOCTOR_NAME, doctorName)
                .putExtra(WorkScheduleDetailActivity.EXTRA_SPECIALTY, specialty),
        )
    }
}
