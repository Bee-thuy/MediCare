package com.example.medicare.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.example.medicare.R
import com.example.medicare.data.auth.AuthRepository
import com.example.medicare.ui.base.FullscreenActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ForgotPasswordActivity : FullscreenActivity() {
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        findViewById<TextView>(R.id.forgotBack).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.forgotButton).setOnClickListener { sendResetEmail() }
    }

    private fun sendResetEmail() {
        val layout = findViewById<TextInputLayout>(R.id.forgotEmailLayout)
        val email = findViewById<TextInputEditText>(R.id.forgotEmailInput).text?.toString().orEmpty().trim()
        layout.error = if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) null else getString(R.string.auth_invalid_email)
        if (layout.error != null) return

        val button = findViewById<MaterialButton>(R.id.forgotButton)
        button.isEnabled = false
        findViewById<View>(R.id.forgotLoading).visibility = View.VISIBLE
        authRepository.sendPasswordReset(email) { result ->
            button.isEnabled = true
            findViewById<View>(R.id.forgotLoading).visibility = View.GONE
            findViewById<TextView>(R.id.forgotMessage).apply {
                text = result.fold(
                    onSuccess = { getString(R.string.auth_reset_sent) },
                    onFailure = { it.message ?: getString(R.string.auth_reset_failed) },
                )
                visibility = View.VISIBLE
            }
        }
    }
}
