package com.example.medicare.manager.doctor

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.appcompat.app.AlertDialog
import com.example.medicare.R
import com.example.medicare.databinding.ActivityWorkShiftBinding
import com.example.medicare.databinding.ItemWorkShiftBinding
import com.example.medicare.data.doctor.DoctorManagementRepository
import com.example.medicare.model.WorkShift
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast

class WorkShiftActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWorkShiftBinding
    private var shifts: List<WorkShift> = emptyList()
    private val addWorkShift = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) loadShifts()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkShiftBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.workShiftsBack.setOnClickListener { finish() }
        binding.workShiftsAdd.setOnClickListener {
            addWorkShift.launch(Intent(this, AddWorkShiftActivity::class.java))
        }
        binding.workShiftsSearch.doAfterTextChanged { showShifts(it?.toString().orEmpty()) }
        loadShifts()
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) loadShifts()
    }

    private fun loadShifts() {
        DoctorManagementRepository(this).getWorkShifts { result ->
            shifts = result.getOrDefault(emptyList())
            showShifts(binding.workShiftsSearch.text?.toString().orEmpty())
        }
    }

    private fun showShifts(query: String) {
        val displayedShifts = shifts.filter { it.name.contains(query, ignoreCase = true) }
        binding.workShiftsList.removeAllViews()
        binding.workShiftsEmpty.visibility = if (displayedShifts.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        displayedShifts.forEach { shift ->
            val itemBinding = ItemWorkShiftBinding.inflate(layoutInflater, binding.workShiftsList, false)
            itemBinding.workShiftItemName.text = shift.name
            itemBinding.workShiftItemTime.text = getString(
                R.string.manager_shift_time_format,
                shift.startTime,
                shift.endTime,
            )
            itemBinding.workShiftItemStatus.apply {
                text = getString(if (shift.isActive) R.string.manager_active else R.string.manager_inactive)
                setTextColor(ContextCompat.getColor(this@WorkShiftActivity, if (shift.isActive) R.color.manager_green else R.color.manager_red))
                setBackgroundResource(if (shift.isActive) R.drawable.manager_status_active_background else R.drawable.manager_status_inactive_background)
            }
            itemBinding.root.setOnClickListener { showActions(shift) }
            binding.workShiftsList.addView(itemBinding.root)
        }
    }

    private fun showActions(shift: WorkShift) {
        val actions = arrayOf("Chỉnh sửa", if (shift.isActive) "Tạm ngưng" else "Kích hoạt", "Xóa có kiểm soát")
        AlertDialog.Builder(this).setTitle(shift.name).setItems(actions) { _, which ->
            when (which) {
                0 -> editShift(shift)
                1 -> DoctorManagementRepository(this).setWorkShiftActive(shift.id, !shift.isActive, ::handleResult)
                2 -> AlertDialog.Builder(this).setTitle("Xóa ca làm?")
                    .setMessage("Chỉ xóa được khi ca chưa được dùng để phân lịch bác sĩ.")
                    .setNegativeButton(R.string.manager_cancel, null)
                    .setPositiveButton("Xóa") { _, _ -> DoctorManagementRepository(this).deleteWorkShiftIfUnused(shift, ::handleResult) }
                    .show()
            }
        }.show()
    }

    private fun editShift(shift: WorkShift) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(52, 16, 52, 0) }
        val name = EditText(this).apply { hint = "Tên ca"; setText(shift.name) }
        val start = EditText(this).apply { hint = "Giờ bắt đầu (HH:mm)"; setText(shift.startTime) }
        val end = EditText(this).apply { hint = "Giờ kết thúc (HH:mm)"; setText(shift.endTime) }
        val description = EditText(this).apply { hint = "Mô tả"; setText(shift.description) }
        listOf(name, start, end, description).forEach(box::addView)
        AlertDialog.Builder(this).setTitle("Chỉnh sửa ca làm").setView(box)
            .setNegativeButton(R.string.manager_cancel, null)
            .setPositiveButton("Lưu") { _, _ ->
                if (name.text.isBlank() || !start.text.matches(Regex("\\d{2}:\\d{2}")) || !end.text.matches(Regex("\\d{2}:\\d{2}"))) {
                    Toast.makeText(this, "Nhập tên và giờ theo dạng HH:mm.", Toast.LENGTH_SHORT).show()
                } else DoctorManagementRepository(this).updateWorkShift(shift, name.text.toString(), start.text.toString(), end.text.toString(), description.text.toString(), ::handleResult)
            }.show()
    }

    private fun handleResult(result: Result<Unit>) {
        result.onSuccess { loadShifts() }.onFailure { Toast.makeText(this, it.message ?: "Không thể lưu thay đổi.", Toast.LENGTH_LONG).show() }
    }
}
