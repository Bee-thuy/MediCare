package com.example.medicare.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import com.example.medicare.adapter.OnboardingPagerAdapter
import com.example.medicare.databinding.ActivityOnboardingBinding
import com.example.medicare.ui.base.FullscreenActivity
import com.example.medicare.ui.splash.SplashActivity
import com.example.medicare.util.AppPreferences

class OnboardingActivity : FullscreenActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.onboardingPager.adapter = OnboardingPagerAdapter(::onPageAction)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.onboardingPager.currentItem == 0) {
                    finish()
                } else {
                    binding.onboardingPager.currentItem -= 1
                }
            }
        })
    }

    private fun onPageAction(position: Int) {
        if (position == LAST_PAGE_INDEX) {
            AppPreferences.setOnboardingComplete(this)
            startActivity(Intent(this, SplashActivity::class.java))
            finish()
        } else {
            binding.onboardingPager.currentItem = position + 1
        }
    }

    private companion object {
        const val LAST_PAGE_INDEX = 2
    }
}
