package com.example.medicare.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.viewpager2.widget.ViewPager2
import com.example.medicare.R
import com.example.medicare.ui.base.FullscreenActivity
import com.example.medicare.ui.main.MainActivity

class OnboardingActivity : FullscreenActivity() {

    private lateinit var pager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        pager = findViewById(R.id.onboardingPager)
        pager.adapter = OnboardingPagerAdapter(::onPageAction)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (pager.currentItem == 0) {
                    finish()
                } else {
                    pager.currentItem -= 1
                }
            }
        })
    }

    private fun onPageAction(position: Int) {
        if (position == LAST_PAGE_INDEX) {
            getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(MainActivity.PREF_ONBOARDING_COMPLETE, true)
                .apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            pager.currentItem = position + 1
        }
    }

    private companion object {
        const val LAST_PAGE_INDEX = 2
    }
}
