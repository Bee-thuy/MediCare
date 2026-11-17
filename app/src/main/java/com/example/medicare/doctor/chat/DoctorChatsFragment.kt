package com.example.medicare.doctor.chat

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.example.medicare.R
import com.example.medicare.data.appointment.AppointmentRepository
import com.example.medicare.data.chat.ConsultationRepository
import com.example.medicare.databinding.FragmentDoctorChatsBinding
import com.example.medicare.model.Appointment
import com.example.medicare.model.AppointmentStatus
import com.example.medicare.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

/** One active Firebase conversation, matching the doctor chat screen in the HTML prototype. */
class DoctorChatsFragment : Fragment() {
    private var _binding: FragmentDoctorChatsBinding? = null
    private val binding get() = _binding!!
    private val repository = ConsultationRepository()
    private var conversations: List<Appointment> = emptyList()
    private var active: Appointment? = null
    private var messageListener: ListenerRegistration? = null
    private var typingListener: ListenerRegistration? = null
    private var isTyping = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentDoctorChatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        binding.doctorChatPatientCard.setOnClickListener { chooseConversation() }
        binding.doctorChatSend.setOnClickListener { sendMessage() }
        binding.doctorChatInput.doAfterTextChanged {
            val shouldType = !it.isNullOrBlank() && active != null
            if (isTyping != shouldType) {
                isTyping = shouldType
                active?.id?.let { id -> repository.setTyping(id, shouldType) }
            }
        }
        showEmpty("Đang tải cuộc trò chuyện…")
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
                }.sortedWith(compareByDescending<Appointment> { it.appointmentDate }.thenByDescending { it.startTime })
                val selected = conversations.find { it.id == active?.id } ?: conversations.firstOrNull()
                if (selected == null) {
                    active = null
                    binding.doctorChatPatientName.text = "Chưa có cuộc trò chuyện"
                    binding.doctorChatPatientStatus.text = "Lịch khám được duyệt sẽ xuất hiện tại đây"
                    binding.doctorChatInput.isEnabled = false
                    binding.doctorChatSend.isEnabled = false
                    showEmpty("Chưa có lịch khám để tư vấn.")
                } else selectConversation(selected)
            }.onFailure {
                showEmpty(it.message ?: "Không tải được cuộc trò chuyện.")
            }
        }
    }

    private fun chooseConversation() {
        if (conversations.isEmpty()) return
        val labels = conversations.map {
            "${it.patientName.ifBlank { "Bệnh nhân" }} · ${it.appointmentDate} ${it.startTime}"
        }.toTypedArray()
        AlertDialog.Builder(requireContext()).setTitle("Chọn cuộc trò chuyện")
            .setSingleChoiceItems(labels, conversations.indexOfFirst { it.id == active?.id }) { dialog, index ->
                dialog.dismiss()
                selectConversation(conversations[index])
            }.setNegativeButton("Đóng", null).show()
    }

    private fun selectConversation(appointment: Appointment) {
        val previousId = active?.id
        if (isTyping && previousId != null) repository.setTyping(previousId, false)
        isTyping = false
        active = appointment
        messageListener?.remove()
        typingListener?.remove()
        binding.doctorChatPatientName.text = appointment.patientName.ifBlank { "Bệnh nhân" }
        binding.doctorChatPatientStatus.text = "🟢 Lịch khám ${appointment.appointmentDate} · Chạm để đổi"
        binding.doctorChatInput.isEnabled = true
        binding.doctorChatSend.isEnabled = true
        binding.doctorChatInput.text?.clear()
        showEmpty("Đang tải tin nhắn…")
        messageListener = repository.observeMessages(appointment.id) { result ->
            if (_binding == null || active?.id != appointment.id) return@observeMessages
            result.onSuccess { messages ->
                repository.markIncomingMessagesRead(appointment.id, messages)
                renderMessages(messages)
            }.onFailure { showEmpty(it.message ?: "Không tải được tin nhắn.") }
        }
        typingListener = repository.observeTyping(appointment.id) { result ->
            if (_binding == null || active?.id != appointment.id) return@observeTyping
            val mine = FirebaseAuth.getInstance().currentUser?.uid
            binding.doctorChatTyping.visibility = if (result.getOrDefault(emptyList()).any { it != mine })
                View.VISIBLE else View.GONE
        }
    }

    private fun renderMessages(messages: List<ChatMessage>) {
        binding.doctorChatMessages.removeAllViews()
        if (messages.isEmpty()) {
            showEmpty("Chưa có tin nhắn. Bạn có thể bắt đầu tư vấn tại đây.")
            return
        }
        val mine = FirebaseAuth.getInstance().currentUser?.uid
        messages.forEach { message ->
            val own = message.senderId == mine
            val bubble = TextView(requireContext()).apply {
                text = message.content
                textSize = 11f
                setTextColor(if (own) Color.WHITE else requireContext().getColor(R.color.manager_ink))
                setPadding(dp(13), dp(11), dp(13), dp(11))
                background = GradientDrawable().apply {
                    setColor(if (own) requireContext().getColor(R.color.doctor_teal) else Color.WHITE)
                    cornerRadii = if (own) floatArrayOf(dp(15).toFloat(), dp(15).toFloat(), dp(15).toFloat(), dp(15).toFloat(), dp(4).toFloat(), dp(4).toFloat(), dp(15).toFloat(), dp(15).toFloat())
                        else floatArrayOf(dp(15).toFloat(), dp(15).toFloat(), dp(15).toFloat(), dp(15).toFloat(), dp(15).toFloat(), dp(15).toFloat(), dp(4).toFloat(), dp(4).toFloat())
                    if (!own) setStroke(dp(1), requireContext().getColor(R.color.manager_border))
                }
                maxWidth = resources.displayMetrics.widthPixels * 3 / 4
            }
            binding.doctorChatMessages.addView(bubble, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = if (own) Gravity.END else Gravity.START
                topMargin = dp(10)
            })
        }
        binding.doctorChatScroll.post { _binding?.doctorChatScroll?.fullScroll(View.FOCUS_DOWN) }
    }

    private fun showEmpty(message: String) {
        if (_binding == null) return
        binding.doctorChatMessages.removeAllViews()
        binding.doctorChatMessages.addView(TextView(requireContext()).apply {
            text = message
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(requireContext().getColor(R.color.manager_muted))
            setPadding(0, dp(26), 0, 0)
        })
    }

    private fun sendMessage() {
        val appointment = active ?: return
        val content = binding.doctorChatInput.text.toString().trim()
        if (content.isEmpty()) return
        binding.doctorChatSend.isEnabled = false
        repository.sendMessage(appointment.id, content) { result ->
            if (_binding == null) return@sendMessage
            binding.doctorChatSend.isEnabled = true
            result.onSuccess { binding.doctorChatInput.text?.clear() }
                .onFailure { Toast.makeText(requireContext(), it.message ?: "Không gửi được tin nhắn.", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()

    override fun onDestroyView() {
        if (isTyping) active?.id?.let { repository.setTyping(it, false) }
        messageListener?.remove()
        typingListener?.remove()
        messageListener = null
        typingListener = null
        _binding = null
        super.onDestroyView()
    }
}
