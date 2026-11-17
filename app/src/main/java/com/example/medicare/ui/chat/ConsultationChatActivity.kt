package com.example.medicare.ui.chat

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.example.medicare.R
import com.example.medicare.data.chat.ConsultationRepository
import com.example.medicare.databinding.ActivityConsultationChatBinding
import com.example.medicare.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class ConsultationChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConsultationChatBinding
    private val consultationRepository = ConsultationRepository()
    private var messageListener: ListenerRegistration? = null
    private var typingListener: ListenerRegistration? = null
    private var appointmentId: String = ""
    private val typingHandler = Handler(Looper.getMainLooper())
    private var isTyping = false
    private val typingDotAnimations = mutableListOf<AnimatorSet>()
    private val typingStopRunnable = Runnable { setTyping(false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConsultationChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appointmentId = intent.getStringExtra(EXTRA_APPOINTMENT_ID).orEmpty()
        binding.consultationChatTitle.text = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank {
            getString(R.string.chat_title)
        }
        binding.consultationChatBack.setOnClickListener { finish() }
        binding.consultationChatSend.setOnClickListener { sendMessage() }
        binding.consultationChatInput.doAfterTextChanged { input ->
            if (input.isNullOrBlank()) {
                setTyping(false)
            } else {
                setTyping(true)
                typingHandler.removeCallbacks(typingStopRunnable)
                typingHandler.postDelayed(typingStopRunnable, TYPING_STOP_DELAY_MS)
            }
        }
        if (appointmentId.isBlank()) {
            Toast.makeText(this, R.string.chat_unavailable, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        observeMessages()
        observeTyping()
    }

    private fun observeMessages() {
        messageListener = consultationRepository.observeMessages(appointmentId) { result ->
            result.onSuccess(::renderMessages).onFailure { error ->
                Toast.makeText(this, error.message ?: getString(R.string.chat_load_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeTyping() {
        typingListener = consultationRepository.observeTyping(appointmentId) { result ->
            result.onSuccess { typingUserIds ->
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                val otherPersonIsTyping = typingUserIds.any { it != currentUserId }
                showTypingIndicator(otherPersonIsTyping)
            }
        }
    }

    private fun renderMessages(messages: List<ChatMessage>) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        consultationRepository.markIncomingMessagesRead(appointmentId, messages)
        binding.consultationChatMessages.removeAllViews()
        binding.consultationChatEmpty.visibility = if (messages.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        val latestMine = messages.lastOrNull { it.senderId == currentUserId }
        messages.forEach { message ->
            val mine = message.senderId == currentUserId
            val bubble = TextView(this).apply {
                text = message.content
                textSize = 15f
                setTextColor(if (mine) Color.WHITE else getColor(R.color.manager_ink))
                setPadding(dp(14), dp(10), dp(14), dp(10))
                background = GradientDrawable().apply {
                    cornerRadius = dp(16).toFloat()
                    setColor(if (mine) getColor(R.color.manager_blue) else Color.WHITE)
                }
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    gravity = if (mine) Gravity.END else Gravity.START
                    topMargin = dp(6)
                }
            }
            binding.consultationChatMessages.addView(bubble)
            if (message.id == latestMine?.id && message.readBy.any { it != currentUserId }) {
                binding.consultationChatMessages.addView(TextView(this).apply {
                    text = getString(R.string.chat_seen)
                    textSize = 11f
                    setTextColor(getColor(R.color.manager_muted))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ).apply {
                        gravity = Gravity.END
                        topMargin = dp(3)
                        marginEnd = dp(4)
                    }
                })
            }
        }
        binding.consultationChatScroll.post { binding.consultationChatScroll.fullScroll(android.view.View.FOCUS_DOWN) }
    }

    private fun sendMessage() {
        val content = binding.consultationChatInput.text.toString().trim()
        if (content.isBlank()) return
        setTyping(false)
        binding.consultationChatSend.isEnabled = false
        consultationRepository.sendMessage(appointmentId, content) { result ->
            if (isFinishing || isDestroyed) return@sendMessage
            binding.consultationChatSend.isEnabled = true
            result.onSuccess { binding.consultationChatInput.text?.clear() }
                .onFailure { error -> Toast.makeText(this, error.message ?: getString(R.string.chat_send_failed), Toast.LENGTH_SHORT).show() }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun showTypingIndicator(show: Boolean) {
        if (show && binding.consultationChatTyping.visibility != android.view.View.VISIBLE) {
            binding.consultationChatTyping.visibility = android.view.View.VISIBLE
            val dots = listOf(
                binding.consultationChatTypingDotOne,
                binding.consultationChatTypingDotTwo,
                binding.consultationChatTypingDotThree,
            )
            dots.forEachIndexed { index, dot ->
                val animation = AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(dot, "translationY", 0f, -dp(5).toFloat(), 0f).apply {
                            duration = 540L
                            startDelay = index * 130L
                            repeatCount = ValueAnimator.INFINITE
                        },
                        ObjectAnimator.ofFloat(dot, "alpha", .45f, 1f, .45f).apply {
                            duration = 540L
                            startDelay = index * 130L
                            repeatCount = ValueAnimator.INFINITE
                        },
                    )
                }
                typingDotAnimations += animation
                animation.start()
            }
        } else if (!show && binding.consultationChatTyping.visibility != android.view.View.GONE) {
            typingDotAnimations.forEach { it.cancel() }
            typingDotAnimations.clear()
            binding.consultationChatTyping.visibility = android.view.View.GONE
        }
    }

    private fun setTyping(typing: Boolean) {
        if (appointmentId.isBlank() || isTyping == typing) return
        isTyping = typing
        consultationRepository.setTyping(appointmentId, typing) { result ->
            result.onFailure {
                // Keep the UI truthful: retry is possible on the next keystroke instead of
                // leaving the other participant with a stale typing indicator.
                isTyping = false
            }
        }
    }

    override fun onStop() {
        setTyping(false)
        super.onStop()
    }

    override fun onDestroy() {
        typingHandler.removeCallbacks(typingStopRunnable)
        typingDotAnimations.forEach { it.cancel() }
        messageListener?.remove()
        typingListener?.remove()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_APPOINTMENT_ID = "appointment_id"
        const val EXTRA_TITLE = "chat_title"
        const val TYPING_STOP_DELAY_MS = 1_200L
    }
}
