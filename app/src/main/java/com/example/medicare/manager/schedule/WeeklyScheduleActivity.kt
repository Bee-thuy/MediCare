package com.example.medicare.manager.schedule

import android.os.Bundle
import android.content.Intent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.medicare.R
import com.example.medicare.data.schedule.ScheduleRepository
import com.example.medicare.model.DoctorSchedule
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.medicare.databinding.ActivityWeeklyScheduleBinding

class WeeklyScheduleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWeeklyScheduleBinding
    private val repository = ScheduleRepository()
    private var weekStart = LocalDate.now().with(DayOfWeek.MONDAY)
    private var schedules: List<DoctorSchedule> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityWeeklyScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        binding.weeklyScheduleBack.setOnClickListener { finish() }
        binding.weeklySchedulePrevious.setOnClickListener { weekStart = weekStart.minusWeeks(1); render() }
        binding.weeklyScheduleNext.setOnClickListener { weekStart = weekStart.plusWeeks(1); render() }
        render()
    }

    override fun onResume() {
        super.onResume()
        repository.getAllSchedules { result ->
            if (isFinishing || isDestroyed) return@getAllSchedules
            result.onSuccess { schedules = it; render() }
                .onFailure { binding.weeklyScheduleList.removeAllViews();
                    binding.weeklyScheduleList.addView(label(it.message ?: "Không tải được lịch làm việc.")) }
        }
    }

    private fun render() {
        if (!::binding.isInitialized) return
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        binding.weeklyScheduleRange.text = "${weekStart.format(formatter)} – ${weekStart.plusDays(6).format(formatter)}"
        binding.weeklyScheduleList.removeAllViews()
        (0L..6L).forEach { offset ->
            val day = weekStart.plusDays(offset)
            val daily = schedules.filter { it.workDate == day.toString() }.sortedBy { it.startTime }
            binding.weeklyScheduleList.addView(label("${day.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.forLanguageTag("vi"))} · ${day.format(formatter)}").apply {
                setTextColor(getColor(R.color.manager_ink)); textSize = 14f
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12); bottomMargin = dp(8) })
            if (daily.isEmpty()) binding.weeklyScheduleList.addView(label("Chưa có ca làm việc."))
            else daily.forEach { schedule ->
                binding.weeklyScheduleList.addView(label("🕒  ${schedule.startTime} – ${schedule.endTime}\nBS. ${schedule.doctorName} · ${schedule.shiftName}\n${if (schedule.isLocked) "Đã khóa" else "Đang nhận lịch"}").apply {
                    setTextColor(getColor(R.color.manager_ink))
                    setBackgroundResource(R.drawable.manager_card_background)
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    setOnClickListener { startActivity(Intent(this@WeeklyScheduleActivity, WorkScheduleDetailActivity::class.java)
                        .putExtra(WorkScheduleDetailActivity.EXTRA_SCHEDULE_ID, schedule.id)
                        .putExtra(WorkScheduleDetailActivity.EXTRA_DOCTOR_NAME, schedule.doctorName)
                        .putExtra(WorkScheduleDetailActivity.EXTRA_SPECIALTY, schedule.specialtyName)) }
                }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
            }
        }
    }

    private fun label(value: String) = TextView(this).apply {
        text = value; textSize = 12f; setTextColor(getColor(R.color.manager_muted))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()
}
