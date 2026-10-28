package com.example.medicare.manager.user

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.medicare.R
import com.example.medicare.databinding.ActivityProposeScheduleBinding

/** Lets a manager select an available replacement time and notify the patient. */
class ProposeScheduleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProposeScheduleBinding
    private var selectedTime: String? = null
    private var appointmentDate: String = "26/05/2025"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProposeScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        appointmentDate = intent.getStringExtra(EXTRA_DATE) ?: appointmentDate
        binding.proposeScheduleInfo.text = getString(R.string.manager_available_slots_info, appointmentDate)
        binding.proposeScheduleBack.setOnClickListener { finish() }
        binding.proposeScheduleCancel.setOnClickListener { finish() }
        listOf(
            binding.proposeSlot0830,
            binding.proposeSlot0900,
            binding.proposeSlot0930,
            binding.proposeSlot1000,
            binding.proposeSlot1030,
            binding.proposeSlot1100,
            binding.proposeSlot1130,
        ).forEach { slot ->
            slot.setOnClickListener { selectTime(slot) }
        }
        binding.proposeScheduleSend.setOnClickListener { sendProposal() }
    }

    private fun selectTime(slot: TextView) {
        selectedTime = slot.text.toString()
        val slots = listOf(
            binding.proposeSlot0830,
            binding.proposeSlot0900,
            binding.proposeSlot0930,
            binding.proposeSlot1000,
            binding.proposeSlot1030,
            binding.proposeSlot1100,
            binding.proposeSlot1130,
        )
        slots.forEach { option ->
            val selected = option == slot
            option.isSelected = selected
            option.setTextColor(ContextCompat.getColor(this, if (selected) R.color.white else R.color.manager_ink))
        }
    }

    private fun sendProposal() {
        val time = selectedTime
        if (time == null) {
            Toast.makeText(this, R.string.manager_select_available_time, Toast.LENGTH_SHORT).show()
            return
        }
        val note = binding.proposeScheduleNote.text?.toString()?.trim().orEmpty()
        val message = getString(
            R.string.manager_proposal_sent_content,
            time,
            appointmentDate,
            note,
        ).trim()
        startActivity(
            Intent(this, PatientNotificationActivity::class.java)
                .putExtra(PatientNotificationActivity.EXTRA_SHOW_SENT_STATE, true)
                .putExtra(PatientNotificationActivity.EXTRA_SENT_CONTENT, message),
        )
    }

    companion object {
        const val EXTRA_DATE = "propose_schedule_date"
    }
}
