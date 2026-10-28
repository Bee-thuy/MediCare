package com.example.medicare.manager.docter

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.example.medicare.R
import com.example.medicare.databinding.ActivityWorkShiftBinding
import com.example.medicare.databinding.ItemWorkShiftBinding

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
            binding.workShiftsList.addView(itemBinding.root)
        }
    }
}
