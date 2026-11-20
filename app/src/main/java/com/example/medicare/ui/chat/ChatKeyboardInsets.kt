package com.example.medicare.ui.chat

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/** Keeps the composer above the keyboard in the app's edge-to-edge window. */
object ChatKeyboardInsets {
    fun bind(root: View, bottomNavigation: View? = null, onKeyboardShown: () -> Unit = {}) {
        val originalBottomPadding = root.paddingBottom
        var keyboardWasVisible = false
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val keyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val keyboardBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            // IME insets already include the navigation area on Samsung devices.
            val extraBottom = if (keyboardVisible) keyboardBottom else 0
            view.updatePadding(bottom = originalBottomPadding + extraBottom)
            bottomNavigation?.visibility = if (keyboardVisible) View.GONE else View.VISIBLE
            if (keyboardVisible && !keyboardWasVisible) onKeyboardShown()
            keyboardWasVisible = keyboardVisible
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }
}
