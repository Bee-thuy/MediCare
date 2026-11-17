package com.example.medicare.user.reminder

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.data.reminder.MedicationReminderRepository
import com.example.medicare.databinding.ActivityAddMedicationReminderBinding
import java.util.Calendar
import com.example.medicare.notification.ReminderScheduler

class AddMedicationReminderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddMedicationReminderBinding
    private val repository = MedicationReminderRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMedicationReminderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.addReminderBack.setOnClickListener { finish() }
        binding.addReminderTime.setOnClickListener { pickTime() }
        binding.addReminderSave.setOnClickListener { saveReminder() }
    }

    private fun pickTime() {
        val calendar = Calendar.getInstance()
        TimePickerDialog(this, { _, hour, minute ->
            binding.addReminderTime.setText("%02d:%02d".format(hour, minute))
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
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
        repository.addReminder(name, dosage, time, frequency) { result ->
            binding.addReminderSave.isEnabled = true
            result.onSuccess { id ->
                ReminderScheduler.medication(this, id, name, time)
                finish()
            }.onFailure { error ->
                Toast.makeText(this, error.message ?: getString(R.string.reminder_save_failed), Toast.LENGTH_LONG).show()
            }
        }
    }
}
