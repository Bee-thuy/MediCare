package com.example.medicare.ui.auth

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.core.view.WindowCompat
import com.example.medicare.R
import com.example.medicare.data.auth.AuthRepository
import com.example.medicare.data.auth.SessionState
import com.example.medicare.util.AuthNavigator
import com.example.medicare.ui.base.FullscreenActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : FullscreenActivity() {
    private val authRepository = AuthRepository()

    private lateinit var nameLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var phoneLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout
    private lateinit var registerButton: MaterialButton
    private lateinit var loading: View
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
        setContentView(R.layout.activity_register)

        nameLayout = findViewById(R.id.registerNameLayout)
        emailLayout = findViewById(R.id.registerEmailLayout)
        phoneLayout = findViewById(R.id.registerPhoneLayout)
        passwordLayout = findViewById(R.id.registerPasswordLayout)
        confirmPasswordLayout = findViewById(R.id.registerConfirmPasswordLayout)
        registerButton = findViewById(R.id.registerButton)
        loading = findViewById(R.id.registerLoading)

        findViewById<View>(R.id.registerBackButton).setOnClickListener { finish() }
        findViewById<TextView>(R.id.registerLoginLink).setOnClickListener { finish() }
        registerButton.setOnClickListener { register() }
    }

    private fun register() {
        val fullName = input(R.id.registerNameInput)
        val email = input(R.id.registerEmailInput)
        val phone = input(R.id.registerPhoneInput)
        val password = input(R.id.registerPasswordInput)
        val confirmPassword = input(R.id.registerConfirmPasswordInput)
        val acceptsTerms = findViewById<MaterialCheckBox>(R.id.registerTerms).isChecked

        nameLayout.error = if (fullName.isBlank()) getString(R.string.auth_required_name) else null
        emailLayout.error = if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) getString(R.string.auth_invalid_email) else null
        phoneLayout.error = if (PHONE_PATTERN.matches(phone)) null else getString(R.string.auth_invalid_phone)
        passwordLayout.error = if (password.length >= MINIMUM_PASSWORD_LENGTH) null else getString(R.string.auth_invalid_password)
        confirmPasswordLayout.error = if (password == confirmPassword) null else getString(R.string.auth_password_mismatch)
        if (!acceptsTerms) showMessage(getString(R.string.auth_accept_terms))

        if (nameLayout.error != null || emailLayout.error != null || phoneLayout.error != null ||
            passwordLayout.error != null || confirmPasswordLayout.error != null || !acceptsTerms) return

        setLoading(true)
        authRepository.registerPatient(fullName, email, phone, password) { result ->
            setLoading(false)
            result.onSuccess {
                showMessage(getString(R.string.auth_verification_sent))
                registerButton.isEnabled = false
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

    private fun input(id: Int): String = findViewById<TextInputEditText>(id).text?.toString().orEmpty().trim()

    private fun setLoading(isLoading: Boolean) {
        loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        registerButton.isEnabled = !isLoading
    }

    private fun showMessage(message: String) {
        findViewById<TextView>(R.id.registerMessage).apply {
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
