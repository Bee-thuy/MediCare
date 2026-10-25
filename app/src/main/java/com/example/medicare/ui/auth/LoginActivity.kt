package com.example.medicare.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.example.medicare.R
import com.example.medicare.data.auth.AuthRepository
import com.example.medicare.data.auth.SessionState
import com.example.medicare.model.UserRole
import com.example.medicare.ui.base.FullscreenActivity
import com.example.medicare.util.AuthNavigator
import com.example.medicare.util.DebugMockAdminAccount
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : FullscreenActivity() {
    private val authRepository = AuthRepository()

    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var loading: View
    private var isCheckingExistingSession = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        emailLayout = findViewById(R.id.loginEmailLayout)
        passwordLayout = findViewById(R.id.loginPasswordLayout)
        emailInput = findViewById(R.id.loginEmailInput)
        passwordInput = findViewById(R.id.loginPasswordInput)
        loginButton = findViewById(R.id.loginButton)
        loading = findViewById(R.id.loginLoading)

        findViewById<TextView>(R.id.loginRegisterLink).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        findViewById<TextView>(R.id.loginForgotPasswordLink).setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.loginDebugAdminButton).apply {
            visibility = if (resources.getBoolean(R.bool.debug_demo_accounts_enabled)) {
                View.VISIBLE
            } else {
                View.GONE
            }
            setOnClickListener {
                emailInput.setText("admin2003@")
                passwordInput.setText("06102003")
                signIn()
            }
        }
        loginButton.setOnClickListener { signIn() }

        intent.getStringExtra(EXTRA_AUTH_MESSAGE)?.let {
            findViewById<TextView>(R.id.loginMessage).apply {
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
        val email = emailInput.text?.toString().orEmpty().trim()
        val password = passwordInput.text?.toString().orEmpty()
        if (DebugMockAdminAccount.matches(this, email, password)) {
            AuthNavigator.openHome(this, UserRole.ADMIN)
            return
        }

        emailLayout.error = if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) null else getString(R.string.auth_invalid_email)
        passwordLayout.error = if (password.isNotBlank()) null else getString(R.string.auth_required_password)
        if (emailLayout.error != null || passwordLayout.error != null) return

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
        loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        loginButton.isEnabled = !isLoading
    }

    private fun showMessage(message: String) {
        findViewById<TextView>(R.id.loginMessage).apply {
            text = message
            visibility = View.VISIBLE
        }
    }

    companion object {
        const val EXTRA_AUTH_MESSAGE = "auth_message"
    }
}
