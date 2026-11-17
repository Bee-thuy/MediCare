package com.example.medicare.user.reminder

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.data.reminder.MedicationReminderRepository
import com.example.medicare.databinding.ActivityMedicationReminderBinding
import com.example.medicare.databinding.ItemMedicationReminderBinding
import com.example.medicare.model.MedicationReminder
import java.time.LocalDate

class MedicationReminderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMedicationReminderBinding
    private val repository = MedicationReminderRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicationReminderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.medicationReminderBack.setOnClickListener { finish() }
        binding.medicationReminderAdd.setOnClickListener {
            startActivity(Intent(this, AddMedicationReminderActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadReminders()
    }

    private fun loadReminders() {
        repository.getReminders { result ->
            result.onSuccess(::renderReminders).onFailure { error ->
                binding.medicationReminderEmpty.text = error.message ?: getString(R.string.reminder_load_failed)
                binding.medicationReminderEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun renderReminders(reminders: List<MedicationReminder>) {
        val today = LocalDate.now().toString()
        binding.medicationReminderList.removeAllViews()
        binding.medicationReminderEmpty.visibility = if (reminders.isEmpty()) View.VISIBLE else View.GONE
        reminders.forEach { reminder ->
            val row = ItemMedicationReminderBinding.inflate(layoutInflater, binding.medicationReminderList, false)
            row.reminderMedicationName.text = reminder.medicationName
            row.reminderDosage.text = reminder.dosage
            row.reminderSchedule.text = getString(R.string.reminder_schedule, reminder.reminderTime, reminder.frequency)
            val takenToday = reminder.lastTakenDate == today
            row.reminderTaken.text = getString(if (takenToday) R.string.reminder_taken else R.string.reminder_mark_taken)
            row.reminderTaken.isEnabled = !takenToday
            row.reminderTaken.setOnClickListener {
                repository.markTaken(reminder.id, today) { result ->
                    result.onSuccess { loadReminders() }.onFailure { error ->
                        Toast.makeText(this, error.message ?: getString(R.string.reminder_update_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            binding.medicationReminderList.addView(row.root)
        }
    }
}
