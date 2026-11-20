package com.example.medicare.user.reminder

import androidx.appcompat.app.AlertDialog
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.view.Gravity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.data.reminder.MedicationReminderRepository
import com.example.medicare.databinding.ActivityAddMedicationReminderBinding
import com.example.medicare.notification.ReminderScheduler
import com.example.medicare.user.PatientScreenChrome

class AddMedicationReminderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddMedicationReminderBinding
    private val repository = MedicationReminderRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMedicationReminderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        PatientScreenChrome.bind(this, if (intent.hasExtra(EXTRA_ID)) "Sửa lời nhắc thuốc" else "Thêm lời nhắc thuốc")
        binding.addReminderBack.setOnClickListener { finish() }
        binding.addReminderTime.setOnClickListener { pickTime() }
        binding.addReminderSave.setOnClickListener { saveReminder() }
        binding.addReminderTime.setText("08:00")
        if (intent.hasExtra(EXTRA_ID)) {
            binding.addReminderMedicationName.setText(intent.getStringExtra(EXTRA_NAME).orEmpty())
            binding.addReminderDosage.setText(intent.getStringExtra(EXTRA_DOSAGE).orEmpty())
            binding.addReminderTime.setText(intent.getStringExtra(EXTRA_TIME).orEmpty())
            binding.addReminderFrequency.setText(intent.getStringExtra(EXTRA_FREQUENCY).orEmpty())
            binding.addReminderSave.text = "Lưu thay đổi"
        }
    }

    private fun pickTime() {
        val current = binding.addReminderTime.text.toString().split(":")
        val hour = NumberPicker(this).apply {
            minValue = 0; maxValue = 23; wrapSelectorWheel = true
            setFormatter { "%02d".format(it) }
            value = current.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 8
            contentDescription = "Giờ nhắc thuốc"
        }
        val minute = NumberPicker(this).apply {
            minValue = 0; maxValue = 59; wrapSelectorWheel = true
            setFormatter { "%02d".format(it) }
            value = current.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
            contentDescription = "Phút nhắc thuốc"
        }
        val wheels = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(24, 12, 24, 12)
            addView(hour, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(minute, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        AlertDialog.Builder(this).setTitle("Chọn giờ nhắc thuốc")
            .setView(wheels).setNegativeButton("Hủy", null)
            .setPositiveButton("Chọn") { _, _ ->
                binding.addReminderTime.setText("%02d:%02d".format(hour.value, minute.value))
            }.show()
    }

    private fun saveReminder() {
        val name = binding.addReminderMedicationName.text.toString().trim()
        val dosage = binding.addReminderDosage.text.toString().trim()
        val time = binding.addReminderTime.text.toString().trim()
        val frequency = binding.addReminderFrequency.text.toString().trim()
        binding.addReminderMedicationName.error = if (name.isBlank()) getString(R.string.manager_required_field) else null
        binding.addReminderTime.error = if (time.isBlank()) getString(R.string.manager_required_field) else null
        binding.addReminderFrequency.error = if (frequency.isBlank()) getString(R.string.manager_required_field) else null
        if (name.isBlank() || time.isBlank() || frequency.isBlank()) return

        binding.addReminderSave.isEnabled = false
        val reminderId = intent.getStringExtra(EXTRA_ID)
        if (reminderId != null) {
            repository.updateReminder(reminderId, name, dosage, time, frequency) { result ->
                binding.addReminderSave.isEnabled = true
                result.onSuccess {
                    if (intent.getBooleanExtra(EXTRA_ACTIVE, true)) ReminderScheduler.medication(this, reminderId, name, time)
                    else ReminderScheduler.cancelMedication(this, reminderId)
                    finish()
                }.onFailure { error ->
                    Toast.makeText(this, error.message ?: getString(R.string.reminder_save_failed), Toast.LENGTH_LONG).show()
                }
            }
        } else repository.addReminder(name, dosage, time, frequency) { result ->
            binding.addReminderSave.isEnabled = true
            result.onSuccess { id ->
                ReminderScheduler.medication(this, id, name, time)
                finish()
            }.onFailure { error ->
                Toast.makeText(this, error.message ?: getString(R.string.reminder_save_failed), Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_NAME = "reminder_name"
        const val EXTRA_DOSAGE = "reminder_dosage"
        const val EXTRA_TIME = "reminder_time"
        const val EXTRA_FREQUENCY = "reminder_frequency"
        const val EXTRA_ACTIVE = "reminder_active"
    }
}
