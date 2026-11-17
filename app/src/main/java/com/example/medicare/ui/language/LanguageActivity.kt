package com.example.medicare.ui.language

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.example.medicare.R
import com.example.medicare.databinding.ActivityLanguageBinding
import com.example.medicare.ui.base.FullscreenActivity
import com.example.medicare.ui.onboarding.OnboardingActivity
import com.example.medicare.util.AppPreferences
import com.google.android.material.card.MaterialCardView

class LanguageActivity : FullscreenActivity() {

    private lateinit var binding: ActivityLanguageBinding
    private var selectedLanguage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.languageVietnameseCard.setOnClickListener { selectLanguage(LANGUAGE_VIETNAMESE) }
        binding.languageEnglishCard.setOnClickListener { selectLanguage(LANGUAGE_ENGLISH) }
        binding.languageContinueButton.setOnClickListener { continueToIntro() }
    }

    private fun selectLanguage(language: String) {
        selectedLanguage = language
        updateSelection(binding.languageVietnameseCard, binding.languageVietnameseSelectedIndicator, language == LANGUAGE_VIETNAMESE)
        updateSelection(binding.languageEnglishCard, binding.languageEnglishSelectedIndicator, language == LANGUAGE_ENGLISH)
        binding.languageContinueButton.isEnabled = true
        binding.languageContinueButton.alpha = 1f
    }

    private fun updateSelection(card: MaterialCardView, indicator: View, isSelected: Boolean) {
        card.strokeColor = getColor(
            if (isSelected) R.color.language_selected_stroke else R.color.language_card_stroke,
        )
        card.strokeWidth = resources.getDimensionPixelSize(
            if (isSelected) R.dimen.language_selected_stroke_width
            else R.dimen.language_card_stroke_width,
        )
        card.setCardBackgroundColor(
            getColor(if (isSelected) R.color.language_selected_background else R.color.white),
        )
        indicator.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
    }

    private fun continueToIntro() {
        val language = selectedLanguage ?: return
        AppPreferences.setLanguage(this, language)
        startActivity(Intent(this, OnboardingActivity::class.java))
        finish()
    }

    private companion object {
        const val LANGUAGE_VIETNAMESE = "vi"
        const val LANGUAGE_ENGLISH = "en"
    }
}
