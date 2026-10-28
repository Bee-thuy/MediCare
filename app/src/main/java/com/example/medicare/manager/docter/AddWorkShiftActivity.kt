package com.example.medicare.manager.docter

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.databinding.ActivityAddWorkShiftBinding
import java.util.Calendar

class AddWorkShiftActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddWorkShiftBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddWorkShiftBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.addWorkShiftBack.setOnClickListener { finish() }
        binding.addWorkShiftStart.setOnClickListener { pickTime { binding.addWorkShiftStart.setText(it) } }
        binding.addWorkShiftEnd.setOnClickListener { pickTime { binding.addWorkShiftEnd.setText(it) } }
        binding.addWorkShiftSave.setOnClickListener { saveShift() }
    }

    private fun pickTime(onTimePicked: (String) -> Unit) {
        val now = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hour, minute -> onTimePicked("%02d:%02d".format(hour, minute)) },
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE),
            true,
        ).show()
    }

    private fun saveShift() {
        val name = binding.addWorkShiftName.text?.toString().orEmpty().trim()
        val startTime = binding.addWorkShiftStart.text?.toString().orEmpty().trim()
        val endTime = binding.addWorkShiftEnd.text?.toString().orEmpty().trim()
        val description = binding.addWorkShiftDescription.text?.toString().orEmpty().trim()
        binding.addWorkShiftNameLayout.error = if (name.isEmpty()) getString(R.string.manager_required_field) else null
        binding.addWorkShiftStartLayout.error = if (startTime.isEmpty()) getString(R.string.manager_required_field) else null
        binding.addWorkShiftEndLayout.error = if (endTime.isEmpty()) getString(R.string.manager_required_field) else null
        if (listOf(
                binding.addWorkShiftNameLayout.error,
                binding.addWorkShiftStartLayout.error,
                binding.addWorkShiftEndLayout.error,
            ).any { it != null }) return

        binding.addWorkShiftSave.isEnabled = false
        DoctorManagementRepository(this).addWorkShift(
            name = name,
            startTime = startTime,
            endTime = endTime,
            description = description,
            isActive = binding.addWorkShiftActive.isChecked,
        ) { result ->
            binding.addWorkShiftSave.isEnabled = true
            result.onSuccess {
                setResult(RESULT_OK)
                finish()
            }.onFailure {
                Toast.makeText(this, it.message ?: getString(R.string.manager_save_failed), Toast.LENGTH_LONG).show()
            }
        }
    }
}
