package com.example.medicare.manager.schedule

import android.os.Bundle
import android.app.AlertDialog
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.medicare.databinding.ActivityWorkScheduleDetailBinding
import com.example.medicare.data.schedule.ScheduleRepository
import com.example.medicare.model.DoctorSchedule

class WorkScheduleDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWorkScheduleDetailBinding
    private val repository = ScheduleRepository()
    private var schedule: DoctorSchedule? = null

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
        val doctor = intent.getStringExtra(EXTRA_DOCTOR_NAME).orEmpty()
        val specialty = intent.getStringExtra(EXTRA_SPECIALTY).orEmpty()
        binding.detailScheduleInitials.text = initialsOf(doctor)
        binding.detailScheduleDoctorName.text = doctor
        binding.detailScheduleDoctorInfo.text = "Đang tải lịch làm việc..."
        binding.detailScheduleSpecialty.text = specialty
        binding.detailScheduleBack.setOnClickListener { finish() }
        binding.detailScheduleEdit.setOnClickListener { showActions() }
        binding.detailScheduleLock.setOnClickListener { toggleLock() }
        intent.getStringExtra(EXTRA_SCHEDULE_ID)?.takeIf { it.isNotBlank() }?.let { loadSchedule(it) }
            ?: run {
                Toast.makeText(this, "Không tìm thấy ca làm việc.", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun loadSchedule(id: String) = repository.getSchedule(id) { result ->
        result.onSuccess {
            schedule = it
            binding.detailScheduleDoctorName.text = it.doctorName
            binding.detailScheduleSpecialty.text = it.specialtyName
            binding.detailScheduleDoctorInfo.text = "${it.workDate} · ${it.startTime} - ${it.endTime} · ${it.room}"
            binding.detailScheduleLock.text = if (it.isLocked) "Mở khóa lịch" else "Khóa lịch"
        }.onFailure { Toast.makeText(this, it.message ?: "Không thể tải lịch.", Toast.LENGTH_LONG).show() }
    }

    private fun toggleLock() {
        val item = schedule ?: return Toast.makeText(this, "Không tìm thấy lịch làm việc.", Toast.LENGTH_SHORT).show()
        repository.setScheduleLocked(item.id, !item.isLocked) { result ->
            result.onSuccess { loadSchedule(item.id) }.onFailure { Toast.makeText(this, it.message ?: "Không thể cập nhật lịch.", Toast.LENGTH_LONG).show() }
        }
    }

    private fun showActions() {
        val item = schedule ?: return Toast.makeText(this, "Không tìm thấy lịch làm việc.", Toast.LENGTH_SHORT).show()
        AlertDialog.Builder(this).setTitle("Quản lý ca làm việc").setItems(arrayOf("Chỉnh sửa thông tin", "Xóa ca làm việc")) { _, selected ->
            if (selected == 0) edit(item) else AlertDialog.Builder(this).setTitle("Xóa ca làm việc?")
                .setMessage("Chỉ xóa được khi không còn lịch hẹn đang chờ hoặc đã xác nhận.")
                .setNegativeButton("Hủy", null).setPositiveButton("Xóa") { _, _ ->
                    repository.deleteScheduleIfNoActiveAppointments(item.id) { result ->
                        result.onSuccess { finish() }.onFailure { Toast.makeText(this, it.message ?: "Không thể xóa lịch.", Toast.LENGTH_LONG).show() }
                    }
                }.show()
        }.show()
    }

    private fun edit(item: DoctorSchedule) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(52, 16, 52, 0) }
        val room = EditText(this).apply { hint = "Phòng khám"; setText(item.room) }
        val duration = EditText(this).apply { hint = "Phút/lượt"; inputType = android.text.InputType.TYPE_CLASS_NUMBER; setText(item.appointmentDurationMinutes.toString()) }
        val capacity = EditText(this).apply { hint = "Số lượt tối đa"; inputType = android.text.InputType.TYPE_CLASS_NUMBER; setText(item.maximumAppointments.toString()) }
        val note = EditText(this).apply { hint = "Ghi chú"; setText(item.note) }
        listOf(room, duration, capacity, note).forEach(box::addView)
        AlertDialog.Builder(this).setTitle("Điều chỉnh lịch").setView(box).setNegativeButton("Hủy", null).setPositiveButton("Lưu") { _, _ ->
            val minutes = duration.text.toString().toIntOrNull(); val max = capacity.text.toString().toIntOrNull()
            if (room.text.isBlank() || minutes == null || max == null || minutes <= 0 || max <= 0) return@setPositiveButton Toast.makeText(this, "Thông tin chưa hợp lệ.", Toast.LENGTH_SHORT).show()
            repository.updateSchedule(item.id, room.text.toString(), minutes, max, note.text.toString()) { result ->
                result.onSuccess { loadSchedule(item.id) }.onFailure { Toast.makeText(this, it.message ?: "Không thể lưu lịch.", Toast.LENGTH_LONG).show() }
            }
        }.show()
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
        const val EXTRA_SCHEDULE_ID = "work_schedule_id"
    }
}
