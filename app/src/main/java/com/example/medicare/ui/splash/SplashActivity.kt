package com.example.medicare.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.example.medicare.R
import com.example.medicare.data.auth.AuthRepository
import com.example.medicare.data.auth.SessionState
import com.example.medicare.ui.base.FullscreenActivity
import com.example.medicare.ui.auth.LoginActivity
import com.example.medicare.ui.language.LanguageActivity
import com.example.medicare.ui.onboarding.OnboardingActivity
import com.example.medicare.util.AppPreferences
import com.example.medicare.util.AuthNavigator

class SplashActivity : FullscreenActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val authRepository = AuthRepository()
    private var initialDelayFinished = false
    private var sessionState: SessionState? = null
    private var didNavigate = false

    private val completeInitialDelay = Runnable {
        initialDelayFinished = true
        routeWhenReady()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        handler.postDelayed(completeInitialDelay, SPLASH_DELAY_MS)
        authRepository.resolveSession {
            sessionState = it
            routeWhenReady()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(completeInitialDelay)
        super.onDestroy()
    }

    private fun routeWhenReady() {
        val state = sessionState ?: return
        if (!initialDelayFinished || didNavigate || isFinishing) return
        didNavigate = true

        when (state) {
            is SessionState.Authenticated -> AuthNavigator.openHome(this, state.role)
            SessionState.SignedOut -> openFirstRunOrLogin()
            is SessionState.Error -> startActivity(
                Intent(this, LoginActivity::class.java)
                    .putExtra(LoginActivity.EXTRA_AUTH_MESSAGE, state.message),
            ).also { finish() }
        }
    }

    private fun openFirstRunOrLogin() {
        val destination = when {
            !AppPreferences.isOnboardingComplete(this) && !AppPreferences.hasLanguage(this) -> LanguageActivity::class.java
            !AppPreferences.isOnboardingComplete(this) -> OnboardingActivity::class.java
            else -> LoginActivity::class.java
        }
        startActivity(Intent(this, destination))
        finish()
    }

    private companion object {
        const val SPLASH_DELAY_MS = 3_000L
    }
}
