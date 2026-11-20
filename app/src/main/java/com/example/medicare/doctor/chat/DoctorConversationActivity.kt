package com.example.medicare.doctor.chat

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.example.medicare.R
import com.example.medicare.data.appointment.AppointmentRepository
import com.example.medicare.data.chat.ConsultationRepository
import com.example.medicare.databinding.ActivityDoctorConversationBinding
import com.example.medicare.model.Appointment
import com.example.medicare.model.AppointmentStatus
import com.example.medicare.model.ChatMessage
import com.example.medicare.ui.chat.ChatKeyboardInsets
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

/** One screen per patient; messages from every eligible appointment share this view. */
class DoctorConversationActivity : AppCompatActivity() {
    companion object { const val EXTRA_PATIENT_ID = "patient_id" }

    private lateinit var binding: ActivityDoctorConversationBinding
    private val repository = ConsultationRepository()
    private var active: Appointment? = null
    private var messageListeners: List<ListenerRegistration> = emptyList()
    private var typingListener: ListenerRegistration? = null
    private var isTyping = false
    private var visible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        binding = ActivityDoctorConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ChatKeyboardInsets.bind(binding.root) {
            binding.doctorConversationScroll.post {
                binding.doctorConversationScroll.fullScroll(View.FOCUS_DOWN)
            }
        }
        binding.doctorConversationBack.setOnClickListener { finish() }
        binding.doctorConversationSend.setOnClickListener { sendMessage() }
        binding.doctorConversationInput.doAfterTextChanged { text ->
            val typing = !text.isNullOrBlank() && active != null
            if (typing != isTyping) {
                isTyping = typing
                active?.id?.let { repository.setTyping(it, typing) }
            }
        }
        binding.doctorConversationInput.isEnabled = false
        binding.doctorConversationSend.isEnabled = false
        showMessage("Đang tải cuộc trò chuyện…")
    }

    override fun onResume() {
        super.onResume()
        visible = true
        val doctorId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val patientId = intent.getStringExtra(EXTRA_PATIENT_ID).orEmpty()
        if (patientId.isBlank()) { finish(); return }
        AppointmentRepository().getAppointmentsForDoctor(doctorId) { result ->
            if (!visible || isFinishing || isDestroyed) return@getAppointmentsForDoctor
            result.onSuccess { appointments ->
                val visits = appointments.filter {
                    it.patientId == patientId && it.status in setOf(AppointmentStatus.CONFIRMED,
                        AppointmentStatus.ARRIVED, AppointmentStatus.IN_PROGRESS,
                        AppointmentStatus.COMPLETED)
                }.sortedWith(compareByDescending<Appointment> { it.appointmentDate }
                    .thenByDescending { it.startTime })
                if (visits.isEmpty()) {
                    binding.doctorConversationName.text = "Bệnh nhân"
                    showMessage("Chưa có lịch khám được duyệt để trò chuyện.")
                    return@onSuccess
                }
                openConversation(visits)
            }.onFailure { showMessage(it.message ?: "Không tải được cuộc trò chuyện.") }
        }
    }

    private fun openConversation(visits: List<Appointment>) {
        stopListeners()
        active = visits.first()
        binding.doctorConversationName.text = active?.patientName.orEmpty().ifBlank { "Bệnh nhân" }
        binding.doctorConversationInput.isEnabled = true
        binding.doctorConversationSend.isEnabled = true
        showMessage("Đang tải tin nhắn…")
        messageListeners = repository.observeConversation(visits.map { it.id }) { result ->
            if (!visible || isFinishing || isDestroyed) return@observeConversation
            result.onSuccess { messages ->
                repository.markConversationRead(messages)
                renderMessages(messages)
            }.onFailure { showMessage(it.message ?: "Không tải được tin nhắn.") }
        }
        typingListener = repository.observeTyping(visits.first().id) { result ->
            if (!visible || isFinishing || isDestroyed) return@observeTyping
            val mine = FirebaseAuth.getInstance().currentUser?.uid
            binding.doctorConversationTyping.visibility =
                if (result.getOrDefault(emptyList()).any { it != mine }) View.VISIBLE else View.GONE
        }
    }

    private fun renderMessages(messages: List<ChatMessage>) {
        binding.doctorConversationMessages.removeAllViews()
        if (messages.isEmpty()) {
            showMessage("Chưa có tin nhắn. Bạn có thể bắt đầu tư vấn tại đây.")
            return
        }
        val mine = FirebaseAuth.getInstance().currentUser?.uid
        messages.forEach { message ->
            val own = message.senderId == mine
            val bubble = TextView(this).apply {
                text = message.content
                textSize = 13f
                setTextColor(if (own) Color.WHITE else getColor(R.color.manager_ink))
                setPadding(dp(13), dp(10), dp(13), dp(10))
                maxWidth = resources.displayMetrics.widthPixels * 3 / 4
                background = GradientDrawable().apply {
                    cornerRadius = dp(15).toFloat()
                    setColor(if (own) getColor(R.color.doctor_teal) else Color.WHITE)
                    if (!own) setStroke(dp(1), getColor(R.color.manager_border))
                }
            }
            binding.doctorConversationMessages.addView(bubble, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = if (own) Gravity.END else Gravity.START
                topMargin = dp(9)
            })
        }
        binding.doctorConversationScroll.post {
            binding.doctorConversationScroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun showMessage(message: String) {
        binding.doctorConversationMessages.removeAllViews()
        binding.doctorConversationMessages.addView(TextView(this).apply {
            text = message
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.manager_muted))
            setPadding(0, dp(28), 0, 0)
        })
    }

    private fun sendMessage() {
        val appointment = active ?: return
        val content = binding.doctorConversationInput.text.toString().trim()
        if (content.isBlank()) return
        binding.doctorConversationSend.isEnabled = false
        repository.sendMessage(appointment.id, content) { result ->
            if (isFinishing || isDestroyed) return@sendMessage
            binding.doctorConversationSend.isEnabled = true
            result.onSuccess { binding.doctorConversationInput.text?.clear() }
                .onFailure { Toast.makeText(this,
                    it.message ?: "Không gửi được tin nhắn.", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun stopListeners() {
        if (isTyping) active?.id?.let { repository.setTyping(it, false) }
        isTyping = false
        messageListeners.forEach { it.remove() }
        typingListener?.remove()
        messageListeners = emptyList()
        typingListener = null
        binding.doctorConversationTyping.visibility = View.GONE
    }

    override fun onStop() {
        visible = false
        stopListeners()
        super.onStop()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()
}
