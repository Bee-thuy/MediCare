package com.example.medicare.doctor.chat

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.example.medicare.R
import com.example.medicare.data.appointment.AppointmentRepository
import com.example.medicare.databinding.FragmentDoctorChatsBinding
import com.example.medicare.databinding.ItemDoctorChatPatientBinding
import com.example.medicare.model.Appointment
import com.example.medicare.model.AppointmentStatus
import com.example.medicare.ui.chat.StaffChatActivity
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** The doctor chooses a patient before entering a conversation. */
class DoctorChatsFragment : Fragment() {
    companion object { const val ARG_PATIENT_ID = "patient_id" }

    private var _binding: FragmentDoctorChatsBinding? = null
    private val binding get() = _binding!!
    private var conversations: List<Appointment> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentDoctorChatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        binding.doctorChatAdmin.setOnClickListener {
            FirebaseAuth.getInstance().currentUser?.uid?.let { doctorId ->
                startActivity(Intent(requireContext(), StaffChatActivity::class.java)
                    .putExtra(StaffChatActivity.EXTRA_DOCTOR_ID, doctorId)
                    .putExtra(StaffChatActivity.EXTRA_TITLE, "Chat với Admin"))
            }
        }
        binding.doctorChatSearch.doAfterTextChanged { renderList(it.toString()) }
        showMessage("Đang tải danh sách bệnh nhân…")
    }

    override fun onResume() {
        super.onResume()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        AppointmentRepository().getAppointmentsForDoctor(uid) { result ->
            if (_binding == null) return@getAppointmentsForDoctor
            result.onSuccess { appointments ->
                conversations = appointments.filter {
                    it.status in setOf(AppointmentStatus.CONFIRMED, AppointmentStatus.ARRIVED,
                        AppointmentStatus.IN_PROGRESS, AppointmentStatus.COMPLETED)
                }.sortedWith(compareByDescending<Appointment> { it.appointmentDate }
                    .thenByDescending { it.startTime })
                    .distinctBy { it.patientId }
                renderList(binding.doctorChatSearch.text.toString())
                val requestedId = arguments?.getString(ARG_PATIENT_ID)
                arguments?.remove(ARG_PATIENT_ID)
                conversations.firstOrNull { it.patientId == requestedId }?.let(::openConversation)
            }.onFailure { showMessage(it.message ?: "Không tải được danh sách bệnh nhân.") }
        }
    }

    private fun renderList(query: String) {
        if (_binding == null) return
        binding.doctorChatPatientList.removeAllViews()
        val matches = conversations.filter {
            it.patientName.contains(query.trim(), ignoreCase = true)
        }
        if (matches.isEmpty()) {
            showMessage(if (conversations.isEmpty()) "Chưa có bệnh nhân để trò chuyện."
                else "Không tìm thấy bệnh nhân phù hợp.")
            return
        }
        matches.forEach { appointment ->
            val row = ItemDoctorChatPatientBinding.inflate(layoutInflater,
                binding.doctorChatPatientList, false)
            row.doctorChatRowName.text = appointment.patientName.ifBlank { "Bệnh nhân" }
            val date = runCatching { LocalDate.parse(appointment.appointmentDate)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }
                .getOrDefault(appointment.appointmentDate)
            row.doctorChatRowSubtitle.text = "Lần khám gần nhất · $date"
            row.root.layoutParams = LinearLayout.LayoutParams(-1, dp(72)).apply {
                bottomMargin = dp(10)
            }
            row.root.setOnClickListener { openConversation(appointment) }
            binding.doctorChatPatientList.addView(row.root)
        }
    }

    private fun openConversation(appointment: Appointment) {
        startActivity(Intent(requireContext(), DoctorConversationActivity::class.java)
            .putExtra(DoctorConversationActivity.EXTRA_PATIENT_ID, appointment.patientId))
    }

    private fun showMessage(message: String) {
        if (_binding == null) return
        binding.doctorChatPatientList.removeAllViews()
        binding.doctorChatPatientList.addView(TextView(requireContext()).apply {
            text = message
            gravity = Gravity.CENTER
            textSize = 12f
            setTextColor(requireContext().getColor(R.color.manager_muted))
            setPadding(dp(12), dp(32), dp(12), dp(12))
        })
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
