package com.example.medicare.ui.main

import android.content.Intent
import android.os.Bundle
import com.example.medicare.R
import com.example.medicare.ui.base.FullscreenActivity
import com.example.medicare.ui.splash.SplashActivity

class MainActivity : FullscreenActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
                .getBoolean(PREF_ONBOARDING_COMPLETE, false)) {
            startActivity(Intent(this, SplashActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

    }

    companion object {
        const val PREFERENCES_NAME = "medicare_preferences"
        const val PREF_ONBOARDING_COMPLETE = "onboarding_complete"
        const val PREF_LANGUAGE = "selected_language"
    }
}
