package com.example.medicare.manager.work_schedule

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.medicare.R
import com.example.medicare.databinding.ActivityWorkScheduleSimpleBinding
import com.example.medicare.manager.docter.WorkShiftActivity

open class ScheduleBaseActivity : AppCompatActivity() {
    protected lateinit var binding: ActivityWorkScheduleSimpleBinding

    protected fun createScreen(title: String, body: String, secondary: String, primary: String) {
        enableEdgeToEdge()
        binding = ActivityWorkScheduleSimpleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        binding.simpleScheduleTitle.text = title
        binding.simpleScheduleBody.text = body
        binding.simpleScheduleSecondary.text = secondary
        binding.simpleSchedulePrimary.text = primary
        binding.simpleScheduleBack.setOnClickListener { finish() }
    }
}

class AssignDoctorScheduleActivity : ScheduleBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createScreen(
            getString(R.string.work_schedule_assign),
            "Bác sĩ: BS. Nguyễn Văn A - Tim mạch\nNgày làm việc: 26/05/2025\nCa làm việc: Ca sáng (08:00 - 12:00)\nPhòng khám: Phòng 201\nThời gian khám mỗi lượt: 30 phút\nSố lượng lịch hẹn tối đa: 8 lượt\n\nGhi chú: Khám bệnh theo lịch hẹn.",
            getString(R.string.manager_cancel),
            getString(R.string.work_schedule_assign),
        )
        binding.simpleSchedulePrimary.setOnClickListener { startActivity(Intent(this, ScheduleUpdateResultActivity::class.java)) }
        binding.simpleScheduleSecondary.setOnClickListener { finish() }
    }
}

class WorkShiftListActivity : ScheduleBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createScreen(
            getString(R.string.work_schedule_shifts),
            "Ca sáng  •  08:00 - 12:00  •  Đang hoạt động\nCa chiều  •  13:00 - 17:00  •  Đang hoạt động\nCa tối  •  17:00 - 21:00  •  Đang hoạt động\nCa khám nhanh  •  08:00 - 12:00  •  Đang hoạt động",
            getString(R.string.manager_back),
            getString(R.string.manager_add_work_shift),
        )
        binding.simpleSchedulePrimary.setOnClickListener { startActivity(Intent(this, WorkShiftActivity::class.java)) }
        binding.simpleScheduleSecondary.setOnClickListener { finish() }
    }
}

class LeaveLockScheduleActivity : ScheduleBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createScreen(
            getString(R.string.work_schedule_leave),
            "Bác sĩ: BS. Nguyễn Văn A\nNgày áp dụng: 26/05/2025\n\nLoại: Nghỉ làm việc / Khóa lịch\nLý do: Nghỉ phép\nGhi chú: Bác sĩ nghỉ phép cá nhân.\n\nCác khung giờ trong ngày sẽ không thể đặt lịch.",
            getString(R.string.manager_cancel),
            getString(R.string.manager_accept),
        )
        binding.simpleSchedulePrimary.setOnClickListener { startActivity(Intent(this, ScheduleUpdateResultActivity::class.java)) }
        binding.simpleScheduleSecondary.setOnClickListener { finish() }
    }
}

class ScheduleUpdateResultActivity : ScheduleBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createScreen(
            getString(R.string.work_schedule_updated),
            "Thông tin làm việc đã được lưu vào hệ thống.\n\nBác sĩ: BS. Nguyễn Văn A\nNgày làm việc: 26/05/2025 (Thứ 2)\nCa làm việc: Ca sáng\nGiờ làm việc: 08:00 - 12:00\nPhòng khám: Phòng 201\nTrạng thái: Đang làm việc",
            getString(R.string.manager_back),
            getString(R.string.work_schedule_title),
        )
        binding.simpleScheduleSecondary.setOnClickListener { finish() }
        binding.simpleSchedulePrimary.setOnClickListener {
            startActivity(Intent(this, WorkScheduleActivity::class.java))
            finish()
        }
    }
}
