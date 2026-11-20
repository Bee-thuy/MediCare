package com.example.medicare.ui.chat

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
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.data.chat.ConsultationRepository
import com.example.medicare.databinding.ActivityConsultationChatBinding
import com.example.medicare.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

/** Shared support room: one thread for each doctor and all administrators. */
class StaffChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConsultationChatBinding
    private val repository = ConsultationRepository()
    private var listener: ListenerRegistration? = null
    private lateinit var doctorId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        binding = ActivityConsultationChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ChatKeyboardInsets.bind(binding.root) {
            binding.consultationChatScroll.post {
                binding.consultationChatScroll.fullScroll(View.FOCUS_DOWN)
            }
        }
        doctorId = intent.getStringExtra(EXTRA_DOCTOR_ID).orEmpty()
        if (doctorId.isBlank()) { finish(); return }
        binding.consultationChatTitle.text = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Chat với Admin" }
        binding.consultationChatBack.setOnClickListener { finish() }
        binding.consultationChatTyping.visibility = View.GONE
        binding.consultationChatSend.setOnClickListener {
            val content = binding.consultationChatInput.text.toString().trim()
            if (content.isEmpty()) return@setOnClickListener
            binding.consultationChatSend.isEnabled = false
            repository.sendStaffMessage(doctorId, content) { result ->
                if (isFinishing || isDestroyed) return@sendStaffMessage
                binding.consultationChatSend.isEnabled = true
                result.onSuccess { binding.consultationChatInput.text?.clear() }
                    .onFailure { Toast.makeText(this, it.message ?: "Không gửi được tin nhắn.", Toast.LENGTH_SHORT).show() }
            }
        }
        listener = repository.observeStaffMessages(doctorId) { result ->
            if (isFinishing || isDestroyed) return@observeStaffMessages
            result.onSuccess(::renderMessages).onFailure {
                Toast.makeText(this, it.message ?: "Không tải được tin nhắn.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderMessages(messages: List<ChatMessage>) {
        repository.markStaffMessagesRead(doctorId, messages)
        binding.consultationChatMessages.removeAllViews()
        binding.consultationChatEmpty.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        messages.forEach { message ->
            val mine = message.senderId == uid
            binding.consultationChatMessages.addView(TextView(this).apply {
                text = message.content
                textSize = 15f
                setTextColor(if (mine) Color.WHITE else getColor(R.color.manager_ink))
                setPadding(dp(14), dp(10), dp(14), dp(10))
                maxWidth = resources.displayMetrics.widthPixels * 3 / 4
                background = GradientDrawable().apply {
                    cornerRadius = dp(16).toFloat()
                    setColor(if (mine) getColor(R.color.doctor_teal) else Color.WHITE)
                }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = if (mine) Gravity.END else Gravity.START
                topMargin = dp(8)
            })
        }
        binding.consultationChatScroll.post { binding.consultationChatScroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()
    override fun onDestroy() { listener?.remove(); super.onDestroy() }

    companion object {
        const val EXTRA_DOCTOR_ID = "doctor_id"
        const val EXTRA_TITLE = "chat_title"
    }
}
