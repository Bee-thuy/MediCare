package com.example.medicare.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.example.medicare.R
import com.example.medicare.ui.base.FullscreenActivity
import com.example.medicare.ui.language.LanguageActivity
import com.example.medicare.ui.main.MainActivity
import com.example.medicare.ui.onboarding.OnboardingActivity

class SplashActivity : FullscreenActivity() {

    private val handler = Handler(Looper.getMainLooper())

    private val nextScreen = Runnable {
        val preferences = getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE)
        val destination = when {
            preferences.getBoolean(MainActivity.PREF_ONBOARDING_COMPLETE, false) -> MainActivity::class.java
            preferences.getString(MainActivity.PREF_LANGUAGE, null) == null -> LanguageActivity::class.java
            else -> OnboardingActivity::class.java
        }
        startActivity(Intent(this, destination))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        handler.postDelayed(nextScreen, SPLASH_DELAY_MS)
    }

    override fun onDestroy() {
        handler.removeCallbacks(nextScreen)
        super.onDestroy()
    }

    private companion object {
        const val SPLASH_DELAY_MS = 3_000L
    }
}
