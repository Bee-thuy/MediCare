package com.example.medicare.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowCompat
import com.example.medicare.BuildConfig
import com.example.medicare.R
import com.example.medicare.data.auth.AuthRepository
import com.example.medicare.databinding.ActivityLoginBinding
import com.example.medicare.model.SessionState
import com.example.medicare.ui.base.FullscreenActivity
import com.example.medicare.util.AuthNavigator

class LoginActivity : FullscreenActivity() {
    private val authRepository = AuthRepository()

    private lateinit var binding: ActivityLoginBinding
    private var isCheckingExistingSession = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginBackButton.setOnClickListener { finish() }
        binding.loginRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.loginForgotPasswordLink.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
        binding.loginButton.setOnClickListener { signIn() }
        if (BuildConfig.DEBUG && listOf(
                BuildConfig.TEST_ADMIN_EMAIL, BuildConfig.TEST_ADMIN_PASSWORD,
                BuildConfig.TEST_DOCTOR_EMAIL, BuildConfig.TEST_DOCTOR_PASSWORD,
                BuildConfig.TEST_PATIENT_EMAIL, BuildConfig.TEST_PATIENT_PASSWORD,
            ).all { it.isNotBlank() }) {
            binding.loginQuickAccounts.visibility = View.VISIBLE
            binding.loginQuickAdmin.text = "Quản trị viên · ${BuildConfig.TEST_ADMIN_EMAIL}"
            binding.loginQuickDoctor.text = "Bác sĩ · ${BuildConfig.TEST_DOCTOR_EMAIL}"
            binding.loginQuickPatient.text = "Bệnh nhân · ${BuildConfig.TEST_PATIENT_EMAIL}"
            binding.loginQuickAdmin.setOnClickListener {
                authenticate(BuildConfig.TEST_ADMIN_EMAIL, BuildConfig.TEST_ADMIN_PASSWORD)
            }
            binding.loginQuickDoctor.setOnClickListener {
                authenticate(BuildConfig.TEST_DOCTOR_EMAIL, BuildConfig.TEST_DOCTOR_PASSWORD)
            }
            binding.loginQuickPatient.setOnClickListener {
                authenticate(BuildConfig.TEST_PATIENT_EMAIL, BuildConfig.TEST_PATIENT_PASSWORD)
            }
        }

        intent.getStringExtra(EXTRA_AUTH_MESSAGE)?.let {
            binding.loginMessage.apply {
                text = it
                visibility = View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isCheckingExistingSession) return

        isCheckingExistingSession = true
        authRepository.resolveSession { state ->
            isCheckingExistingSession = false
            if (state is SessionState.Authenticated) {
                AuthNavigator.openHome(this, state.role)
            }
        }
    }

    private fun signIn() {
        val email = binding.loginEmailInput.text?.toString().orEmpty().trim()
        val password = binding.loginPasswordInput.text?.toString().orEmpty()
        binding.loginEmailInput.error = if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) null else getString(R.string.auth_invalid_email)
        binding.loginPasswordInput.error = if (password.isNotBlank()) null else getString(R.string.auth_required_password)
        if (binding.loginEmailInput.error != null || binding.loginPasswordInput.error != null) return

        authenticate(email, password)
    }

    private fun authenticate(email: String, password: String) {
        binding.loginMessage.visibility = View.GONE
        setLoading(true)
        authRepository.signIn(email, password) { state ->
            setLoading(false)
            when (state) {
                is SessionState.Authenticated -> AuthNavigator.openHome(this, state.role)
                is SessionState.Error -> showMessage(state.message)
                SessionState.SignedOut -> showMessage(getString(R.string.auth_session_unavailable))
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.loginLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.loginButton.isEnabled = !isLoading
        binding.loginQuickAdmin.isEnabled = !isLoading
        binding.loginQuickDoctor.isEnabled = !isLoading
        binding.loginQuickPatient.isEnabled = !isLoading
    }

    private fun showMessage(message: String) {
        binding.loginMessage.apply {
            text = message
            visibility = View.VISIBLE
        }
    }

    companion object {
        const val EXTRA_AUTH_MESSAGE = "auth_message"
    }
}
