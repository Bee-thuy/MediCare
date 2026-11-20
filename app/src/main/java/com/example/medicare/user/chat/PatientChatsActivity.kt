package com.example.medicare.user.chat

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.WindowManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.example.medicare.R
import com.example.medicare.data.appointment.AppointmentRepository
import com.example.medicare.data.chat.ConsultationRepository
import com.example.medicare.databinding.ActivityPatientChatsBinding
import com.example.medicare.model.Appointment
import com.example.medicare.model.AppointmentStatus
import com.example.medicare.model.ChatMessage
import com.example.medicare.user.PatientScreenChrome
import com.example.medicare.ui.chat.ChatKeyboardInsets
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

/** Patient conversation shown directly in the HTML-style page, with real Firestore messages. */
class PatientChatsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPatientChatsBinding
    private val repository = ConsultationRepository()
    private var conversations: List<Appointment> = emptyList()
    private var eligibleAppointments: List<Appointment> = emptyList()
    private var active: Appointment? = null
    private var messageListeners: List<ListenerRegistration> = emptyList()
    private var typingListener: ListenerRegistration? = null
    private var isTyping = false
    private var requestedDoctorId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        binding = ActivityPatientChatsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestedDoctorId = intent.getStringExtra(EXTRA_DOCTOR_ID)
        binding.patientChatsBack.setOnClickListener { finish() }
        binding.patientChatDoctorCard.setOnClickListener { chooseConversation() }
        binding.patientChatSend.setOnClickListener { sendMessage() }
        binding.patientChatInput.doAfterTextChanged { text ->
            val typing = !text.isNullOrBlank() && active != null
            if (typing != isTyping) {
                isTyping = typing
                active?.id?.let { repository.setTyping(it, typing) }
            }
        }
        ChatKeyboardInsets.bind(binding.root, binding.patientChatBottomNavigation.root) {
            binding.patientChatScroll.post { binding.patientChatScroll.fullScroll(View.FOCUS_DOWN) }
        }
        PatientScreenChrome.bind(this, "Chat tư vấn bác sĩ")
        showEmpty("Đang tải cuộc trò chuyện…")
    }

    override fun onResume() {
        super.onResume()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        AppointmentRepository().getAppointmentsForPatient(uid) { result ->
            if (isFinishing || isDestroyed) return@getAppointmentsForPatient
            result.onSuccess { appointments ->
                eligibleAppointments = appointments.filter {
                    it.status in setOf(AppointmentStatus.CONFIRMED, AppointmentStatus.ARRIVED,
                        AppointmentStatus.IN_PROGRESS, AppointmentStatus.COMPLETED)
                }.sortedWith(compareByDescending<Appointment> { it.appointmentDate }.thenByDescending { it.startTime })
                conversations = eligibleAppointments.distinctBy { it.doctorId }
                val selected = conversations.find { it.doctorId == requestedDoctorId }
                    ?: conversations.find { it.doctorId == active?.doctorId } ?: conversations.firstOrNull()
                requestedDoctorId = null
                if (selected == null) {
                    binding.patientChatDoctorName.text = "Chưa có cuộc trò chuyện"
                    binding.patientChatDoctorStatus.text = "Lịch đã duyệt sẽ xuất hiện tại đây"
                    binding.patientChatInput.isEnabled = false
                    binding.patientChatSend.isEnabled = false
                    showEmpty("Chưa có lịch khám được duyệt để trò chuyện.")
                } else selectConversation(selected)
            }.onFailure { showEmpty(it.message ?: "Không tải được cuộc trò chuyện.") }
        }
    }

    private fun chooseConversation() {
        if (conversations.isEmpty()) return
        val labels = conversations.map { "BS. ${it.doctorName}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Chọn cuộc trò chuyện")
            .setSingleChoiceItems(labels, conversations.indexOfFirst { it.id == active?.id }) { dialog, index ->
                dialog.dismiss()
                selectConversation(conversations[index])
            }.setNegativeButton("Đóng", null).show()
    }

    private fun selectConversation(appointment: Appointment) {
        if (isTyping) active?.id?.let { repository.setTyping(it, false) }
        isTyping = false
        messageListeners.forEach { it.remove() }
        typingListener?.remove()
        active = appointment
        binding.patientChatDoctorName.text = "BS. ${appointment.doctorName}"
        binding.patientChatDoctorStatus.text = "🟢 ${appointment.specialtyName} · Chạm để đổi người"
        binding.patientChatInput.isEnabled = true
        binding.patientChatSend.isEnabled = true
        binding.patientChatInput.text?.clear()
        showEmpty("Đang tải tin nhắn…")
        messageListeners = repository.observeConversation(eligibleAppointments.filter { it.doctorId == appointment.doctorId }.map { it.id }) { result ->
            if (isFinishing || isDestroyed || active?.id != appointment.id) return@observeConversation
            result.onSuccess { messages ->
                repository.markConversationRead(messages)
                renderMessages(messages)
            }.onFailure { showEmpty(it.message ?: "Không tải được tin nhắn.") }
        }
        typingListener = repository.observeTyping(appointment.id) { result ->
            if (isFinishing || isDestroyed || active?.id != appointment.id) return@observeTyping
            val mine = FirebaseAuth.getInstance().currentUser?.uid
            binding.patientChatTyping.visibility = if (result.getOrDefault(emptyList()).any { it != mine })
                View.VISIBLE else View.GONE
        }
    }

    private fun renderMessages(messages: List<ChatMessage>) {
        binding.patientChatMessages.removeAllViews()
        if (messages.isEmpty()) {
            showEmpty("Chưa có tin nhắn. Bạn có thể bắt đầu cuộc trò chuyện tại đây.")
            return
        }
        val mine = FirebaseAuth.getInstance().currentUser?.uid
        messages.forEach { message ->
            val own = message.senderId == mine
            val bubble = TextView(this).apply {
                text = message.content
                textSize = 11f
                setTextColor(if (own) Color.WHITE else getColor(R.color.manager_ink))
                setPadding(dp(13), dp(11), dp(13), dp(11))
                maxWidth = resources.displayMetrics.widthPixels * 3 / 4
                background = GradientDrawable().apply {
                    setColor(if (own) getColor(R.color.patient_purple) else Color.WHITE)
                    cornerRadius = dp(15).toFloat()
                    if (!own) setStroke(dp(1), getColor(R.color.manager_border))
                }
            }
            binding.patientChatMessages.addView(bubble, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = if (own) Gravity.END else Gravity.START
                topMargin = dp(10)
            })
        }
        binding.patientChatScroll.post { binding.patientChatScroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun showEmpty(message: String) {
        binding.patientChatMessages.removeAllViews()
        binding.patientChatMessages.addView(TextView(this).apply {
            text = message
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.manager_muted))
            setPadding(0, dp(26), 0, 0)
        })
    }

    private fun sendMessage() {
        val appointment = active ?: return
        val content = binding.patientChatInput.text.toString().trim()
        if (content.isBlank()) return
        binding.patientChatSend.isEnabled = false
        repository.sendMessage(appointment.id, content) { result ->
            if (isFinishing || isDestroyed) return@sendMessage
            binding.patientChatSend.isEnabled = true
            result.onSuccess { binding.patientChatInput.text?.clear() }
                .onFailure { Toast.makeText(this, it.message ?: "Không gửi được tin nhắn.", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()

    override fun onStop() {
        if (isTyping) active?.id?.let { repository.setTyping(it, false) }
        isTyping = false
        messageListeners.forEach { it.remove() }
        typingListener?.remove()
        messageListeners = emptyList()
        typingListener = null
        super.onStop()
    }

    companion object { const val EXTRA_DOCTOR_ID = "doctor_id" }
}
