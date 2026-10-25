package com.example.medicare.util

import android.content.Context

object AppPreferences {
    private const val PREFERENCES_NAME = "medicare_preferences"
    private const val PREF_ONBOARDING_COMPLETE = "onboarding_complete"
    private const val PREF_LANGUAGE = "selected_language"

    fun isOnboardingComplete(context: Context): Boolean =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getBoolean(PREF_ONBOARDING_COMPLETE, false)

    fun setOnboardingComplete(context: Context) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_ONBOARDING_COMPLETE, true)
            .apply()
    }

    fun hasLanguage(context: Context): Boolean =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(PREF_LANGUAGE, null) != null

    fun setLanguage(context: Context, language: String) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_LANGUAGE, language)
            .apply()
    }
}
