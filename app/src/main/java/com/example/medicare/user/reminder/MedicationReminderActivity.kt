package com.example.medicare.user.reminder

import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.data.reminder.MedicationReminderRepository
import com.example.medicare.databinding.ActivityMedicationReminderBinding
import com.example.medicare.databinding.ItemMedicationReminderBinding
import com.example.medicare.databinding.DialogPatientReminderBinding
import com.example.medicare.model.MedicationReminder
import com.example.medicare.user.PatientScreenChrome
import com.example.medicare.notification.ReminderScheduler
import java.time.LocalDate
import java.time.LocalTime
import com.google.android.material.bottomsheet.BottomSheetDialog

class MedicationReminderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMedicationReminderBinding
    private val repository = MedicationReminderRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicationReminderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.medicationReminderBack.setOnClickListener { finish() }
        binding.medicationReminderAdd.setOnClickListener { showReminderForm() }
        binding.medicationReminderBottomAdd.setOnClickListener { showReminderForm() }
        PatientScreenChrome.bind(this, "Nhắc thuốc & tái khám")
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
            row.reminderMedicationName.text = "💊 ${reminder.medicationName}"
            row.reminderDosage.text = reminder.dosage.ifBlank { reminder.frequency }
            val dp = resources.displayMetrics.density
            reminder.reminderTime.split(',').map(String::trim).filter(String::isNotBlank).forEach { hour ->
                row.reminderTimeChips.addView(TextView(this).apply {
                    text = "⏰ $hour"
                    textSize = 10f
                    setTextColor(getColor(R.color.manager_muted))
                    gravity = android.view.Gravity.CENTER
                    setPadding((12 * dp).toInt(), 0, (12 * dp).toInt(), 0)
                    setBackgroundResource(R.drawable.patient_secondary_button_background)
                }, LinearLayout.LayoutParams(-2, (34 * dp).toInt()).apply { marginEnd = (6 * dp).toInt() })
            }
            row.reminderToggle.isChecked = reminder.isActive
            var syncing = false
            row.reminderToggle.setOnCheckedChangeListener { button, active ->
                if (syncing) return@setOnCheckedChangeListener
                button.isEnabled = false
                repository.setActive(reminder.id, active) { result ->
                    button.isEnabled = true
                    result.onSuccess {
                        if (active) ReminderScheduler.medication(this, reminder.id, reminder.medicationName, reminder.reminderTime)
                        else ReminderScheduler.cancelMedication(this, reminder.id)
                        row.reminderTaken.isEnabled = active && reminder.lastTakenDate != today
                    }.onFailure { error ->
                        syncing = true
                        row.reminderToggle.isChecked = !active
                        syncing = false
                        Toast.makeText(this, error.message ?: getString(R.string.reminder_update_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            val takenToday = reminder.lastTakenDate == today
            row.reminderTaken.text = if (takenToday) "✓ Đã uống" else "☐ Đánh dấu đã uống"
            row.reminderTaken.isEnabled = !takenToday && reminder.isActive
            row.reminderTaken.setOnClickListener {
                repository.markTaken(reminder.id, today) { result ->
                    result.onSuccess { loadReminders() }.onFailure { error ->
                        Toast.makeText(this, error.message ?: getString(R.string.reminder_update_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            row.reminderEdit.setOnClickListener { showReminderForm(reminder) }
            binding.medicationReminderList.addView(row.root)
        }
    }

    private fun showReminderForm(reminder: MedicationReminder? = null) {
        val sheet = BottomSheetDialog(this)
        val form = DialogPatientReminderBinding.inflate(layoutInflater)
        form.reminderDialogTitle.text = if (reminder == null) "Tạo nhắc thuốc" else "Sửa nhắc thuốc"
        form.reminderDialogName.setText(reminder?.medicationName.orEmpty())
        form.reminderDialogDosage.setText(reminder?.dosage.orEmpty())
        form.reminderDialogTimes.setText(reminder?.reminderTime ?: "08:00")
        form.reminderDialogClose.setOnClickListener { sheet.dismiss() }
        form.reminderDialogSave.setOnClickListener {
            val name = form.reminderDialogName.text.toString().trim()
            val dosage = form.reminderDialogDosage.text.toString().trim()
            val times = form.reminderDialogTimes.text.toString().split(',').map(String::trim)
                .filter(String::isNotBlank).distinct()
            form.reminderDialogName.error = if (name.isBlank()) "Nhập tên thuốc" else null
            form.reminderDialogTimes.error = if (times.isEmpty() || times.size > 12 || times.any { runCatching { LocalTime.parse(it) }.isFailure })
                "Nhập giờ dạng HH:mm, ngăn cách bằng dấu phẩy (tối đa 12 giờ)" else null
            if (form.reminderDialogName.error != null || form.reminderDialogTimes.error != null) return@setOnClickListener
            val timeValue = times.joinToString(", ")
            val frequency = "${times.size} lần/ngày"
            form.reminderDialogSave.isEnabled = false
            val callback: (Result<*>) -> Unit = { result ->
                form.reminderDialogSave.isEnabled = true
                result.onSuccess {
                    val id = reminder?.id ?: it as String
                    if (reminder?.isActive != false) ReminderScheduler.medication(this, id, name, timeValue)
                    else ReminderScheduler.cancelMedication(this, id)
                    sheet.dismiss()
                    loadReminders()
                }.onFailure { error ->
                    Toast.makeText(this, error.message ?: "Không lưu được lời nhắc.", Toast.LENGTH_LONG).show()
                }
            }
            if (reminder == null) repository.addReminder(name, dosage, timeValue, frequency, callback)
            else repository.updateReminder(reminder.id, name, dosage, timeValue, frequency, callback)
        }
        sheet.setContentView(form.root)
        form.root.requestFocus()
        sheet.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        sheet.show()
    }
}
