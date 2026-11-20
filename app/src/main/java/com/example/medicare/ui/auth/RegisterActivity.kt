package com.example.medicare.ui.auth

import android.os.Bundle
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.view.WindowCompat
import com.example.medicare.R
import com.example.medicare.data.auth.AuthRepository
import com.example.medicare.databinding.ActivityRegisterBinding
import com.example.medicare.model.SessionState
import com.example.medicare.util.AuthNavigator
import com.example.medicare.ui.base.FullscreenActivity
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : FullscreenActivity() {
    private val authRepository = AuthRepository()

    private lateinit var binding: ActivityRegisterBinding
    private var isCheckingVerification = false
    private var isVerificationPolling = false
    private val verificationHandler = Handler(Looper.getMainLooper())
    private val verificationPoll = object : Runnable {
        override fun run() {
            checkEmailVerification()
            if (isVerificationPolling) {
                verificationHandler.postDelayed(this, VERIFICATION_POLL_INTERVAL_MS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.registerBackButton.setOnClickListener { finish() }
        binding.registerLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            finish()
        }
        binding.registerForgotLink.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
            finish()
        }
        binding.registerButton.setOnClickListener { register() }
    }

    private fun register() {
        val fullName = binding.registerNameInput.text?.toString().orEmpty().trim()
        val email = binding.registerEmailInput.text?.toString().orEmpty().trim()
        val phone = binding.registerPhoneInput.text?.toString().orEmpty().trim()
        val password = binding.registerPasswordInput.text?.toString().orEmpty().trim()
        val confirmPassword = binding.registerConfirmPasswordInput.text?.toString().orEmpty().trim()
        val acceptsTerms = binding.registerTerms.isChecked

        binding.registerNameInput.error = if (fullName.isBlank()) getString(R.string.auth_required_name) else null
        binding.registerEmailInput.error = if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) getString(R.string.auth_invalid_email) else null
        binding.registerPhoneInput.error = if (PHONE_PATTERN.matches(phone)) null else getString(R.string.auth_invalid_phone)
        binding.registerPasswordInput.error = if (password.length >= MINIMUM_PASSWORD_LENGTH) null else getString(R.string.auth_invalid_password)
        binding.registerConfirmPasswordInput.error = if (password == confirmPassword) null else getString(R.string.auth_password_mismatch)
        if (!acceptsTerms) showMessage(getString(R.string.auth_accept_terms))

        if (binding.registerNameInput.error != null || binding.registerEmailInput.error != null || binding.registerPhoneInput.error != null ||
            binding.registerPasswordInput.error != null || binding.registerConfirmPasswordInput.error != null || !acceptsTerms) return

        setLoading(true)
        authRepository.registerPatient(fullName, email, phone, password) { result ->
            setLoading(false)
            result.onSuccess {
                showMessage(getString(R.string.auth_verification_sent))
                binding.registerButton.isEnabled = false
                startVerificationPolling()
            }.onFailure { showMessage(it.message ?: getString(R.string.auth_registration_failed)) }
        }
    }

    override fun onResume() {
        super.onResume()
        if (FirebaseAuth.getInstance().currentUser != null) {
            checkEmailVerification()
            startVerificationPolling()
        }
    }

    override fun onPause() {
        stopVerificationPolling()
        super.onPause()
    }

    private fun checkEmailVerification() {
        if (FirebaseAuth.getInstance().currentUser == null || isCheckingVerification) return
        isCheckingVerification = true
        authRepository.resolveSession { state ->
            isCheckingVerification = false
            when (state) {
                is SessionState.Authenticated -> {
                    stopVerificationPolling()
                    AuthNavigator.openHome(this, state.role)
                }
                is SessionState.Error -> Unit
                SessionState.SignedOut -> stopVerificationPolling()
            }
        }
    }

    private fun startVerificationPolling() {
        if (isVerificationPolling) return
        isVerificationPolling = true
        verificationHandler.post(verificationPoll)
    }

    private fun stopVerificationPolling() {
        isVerificationPolling = false
        verificationHandler.removeCallbacks(verificationPoll)
    }

    private fun setLoading(isLoading: Boolean) {
        binding.registerLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.registerButton.isEnabled = !isLoading
    }

    private fun showMessage(message: String) {
        binding.registerMessage.apply {
            text = message
            visibility = View.VISIBLE
        }
    }

    private companion object {
        const val MINIMUM_PASSWORD_LENGTH = 8
        const val VERIFICATION_POLL_INTERVAL_MS = 3_000L
        val PHONE_PATTERN = Regex("^[0-9+][0-9 -]{7,14}$")
    }
}
