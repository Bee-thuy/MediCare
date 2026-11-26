package com.example.medicare.ui.auth

import android.os.Bundle
import android.content.Intent
import android.view.View
import com.example.medicare.R
import com.example.medicare.data.auth.AuthRepository
import com.example.medicare.databinding.ActivityForgotPasswordBinding
import com.example.medicare.ui.base.FullscreenActivity

class ForgotPasswordActivity : FullscreenActivity() {
    private val authRepository = AuthRepository()
    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.forgotBack.setOnClickListener { finish() }
        binding.forgotLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            finish()
        }
        binding.forgotRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
        binding.forgotButton.setOnClickListener { sendResetEmail() }
    }

    private fun sendResetEmail() {
        val email = binding.forgotEmailInput.text?.toString().orEmpty().trim()
        binding.forgotEmailInput.error = if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) null else getString(R.string.auth_invalid_email)
        if (binding.forgotEmailInput.error != null) return

        binding.forgotButton.visibility = View.GONE
        binding.forgotLoading.visibility = View.VISIBLE
        authRepository.sendPasswordReset(email) { result ->
            binding.forgotButton.visibility = View.VISIBLE
            binding.forgotLoading.visibility = View.GONE
            binding.forgotMessage.apply {
                text = result.fold(
                    onSuccess = { getString(R.string.auth_reset_sent) },
                    onFailure = { it.message ?: getString(R.string.auth_reset_failed) },
                )
                visibility = View.VISIBLE
            }
        }
    }
}
