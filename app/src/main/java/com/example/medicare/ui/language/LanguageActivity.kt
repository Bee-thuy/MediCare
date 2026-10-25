package com.example.medicare.ui.language

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.example.medicare.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.example.medicare.ui.base.FullscreenActivity
import com.example.medicare.ui.main.MainActivity
import com.example.medicare.ui.onboarding.OnboardingActivity

class LanguageActivity : FullscreenActivity() {

    private lateinit var vietnameseCard: MaterialCardView
    private lateinit var englishCard: MaterialCardView
    private lateinit var continueButton: MaterialButton
    private var selectedLanguage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language)

        vietnameseCard = findViewById(R.id.languageVietnameseCard)
        englishCard = findViewById(R.id.languageEnglishCard)
        continueButton = findViewById(R.id.languageContinueButton)

        vietnameseCard.setOnClickListener { selectLanguage(LANGUAGE_VIETNAMESE) }
        englishCard.setOnClickListener { selectLanguage(LANGUAGE_ENGLISH) }
        continueButton.setOnClickListener { continueToIntro() }
    }

    private fun selectLanguage(language: String) {
        selectedLanguage = language
        updateSelection(vietnameseCard, language == LANGUAGE_VIETNAMESE)
        updateSelection(englishCard, language == LANGUAGE_ENGLISH)
        continueButton.isEnabled = true
        continueButton.alpha = 1f
    }

    private fun updateSelection(card: MaterialCardView, isSelected: Boolean) {
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
        card.findViewById<View>(R.id.languageSelectedIndicator).visibility =
            if (isSelected) View.VISIBLE else View.INVISIBLE
    }

    private fun continueToIntro() {
        val language = selectedLanguage ?: return
        getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE)
            .edit()
            .putString(MainActivity.PREF_LANGUAGE, language)
            .apply()
        startActivity(Intent(this, OnboardingActivity::class.java))
        finish()
    }

    private companion object {
        const val LANGUAGE_VIETNAMESE = "vi"
        const val LANGUAGE_ENGLISH = "en"
    }
}
