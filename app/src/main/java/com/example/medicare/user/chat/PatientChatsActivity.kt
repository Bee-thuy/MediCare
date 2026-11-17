package com.example.medicare.user.chat

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.data.appointment.AppointmentRepository
import com.example.medicare.databinding.ActivityPatientChatsBinding
import com.example.medicare.model.Appointment
import com.example.medicare.model.AppointmentStatus
import com.example.medicare.ui.chat.ConsultationChatActivity
import com.google.firebase.auth.FirebaseAuth

/** Shows the patient's real consultations, then opens the existing Firestore chat. */
class PatientChatsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPatientChatsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientChatsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.patientChatsBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        AppointmentRepository().getAppointmentsForPatient(uid) { result ->
            if (isFinishing || isDestroyed) return@getAppointmentsForPatient
            binding.patientChatsList.removeAllViews()
            val available = result.getOrDefault(emptyList()).filter {
                it.status in setOf(AppointmentStatus.CONFIRMED, AppointmentStatus.ARRIVED,
                    AppointmentStatus.IN_PROGRESS, AppointmentStatus.COMPLETED)
            }.sortedWith(compareByDescending<Appointment> { it.appointmentDate }.thenByDescending { it.startTime })
            binding.patientChatsEmpty.visibility = if (available.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            available.forEach { appointment ->
                val row = TextView(this).apply {
                    text = "💬  BS. ${appointment.doctorName}\n${appointment.specialtyName} · ${appointment.appointmentDate}"
                    setTextColor(getColor(R.color.manager_ink))
                    textSize = 13f
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(15), dp(10), dp(15), dp(10))
                    setBackgroundResource(R.drawable.manager_card_background)
                    setOnClickListener {
                        startActivity(Intent(this@PatientChatsActivity, ConsultationChatActivity::class.java)
                            .putExtra(ConsultationChatActivity.EXTRA_APPOINTMENT_ID, appointment.id)
                            .putExtra(ConsultationChatActivity.EXTRA_TITLE, appointment.doctorName))
                    }
                }
                binding.patientChatsList.addView(row, LinearLayout.LayoutParams(-1, dp(72)).apply {
                    bottomMargin = dp(9)
                })
            }
            result.exceptionOrNull()?.let { binding.patientChatsEmpty.text = it.message ?: "Không tải được cuộc trò chuyện." }
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
}
