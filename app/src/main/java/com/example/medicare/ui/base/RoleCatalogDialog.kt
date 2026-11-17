package com.example.medicare.ui.base

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Role navigation drawer shared by the doctor and patient shells. */
object RoleCatalogDialog {
    fun show(context: Context, title: String, accent: Int, entries: Array<String>,
             selectedIndex: Int = -1, footerLabel: String? = null, onFooter: (() -> Unit)? = null,
             onSelect: (Int) -> Unit) {
        val dialog = Dialog(context)
        val density = context.resources.displayMetrics.density
        fun dp(value: Int) = (value * density + 0.5f).toInt()
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(30), dp(14), dp(18))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadii = floatArrayOf(0f, 0f, dp(24).toFloat(), dp(24).toFloat(),
                    dp(24).toFloat(), dp(24).toFloat(), 0f, 0f)
            }
        }
        val heading = TextView(context).apply {
            text = "📱  $title"
            textSize = 18f
            setTextColor(Color.rgb(22, 44, 75))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(4), dp(12), 0, dp(17))
        }
        panel.addView(heading)
        val count = TextView(context).apply {
            text = "${entries.size} NHÓM CHỨC NĂNG MOBILE"
            textSize = 10f
            letterSpacing = 0.08f
            setTextColor(Color.rgb(128, 145, 166))
            setPadding(dp(8), dp(5), 0, dp(10))
        }
        panel.addView(count)
        val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        entries.forEachIndexed { index, label ->
            list.addView(TextView(context).apply {
                text = label
                textSize = 13f
                setTextColor(if (index == selectedIndex) accent else Color.rgb(53, 80, 111))
                if (index == selectedIndex) {
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    background = GradientDrawable().apply {
                        setColor((accent and 0x00FFFFFF) or 0x15000000)
                        cornerRadius = dp(12).toFloat()
                    }
                }
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(11), 0, dp(5), 0)
                if (index != selectedIndex) background = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
                    .let { attributes -> attributes.getDrawable(0).also { attributes.recycle() } }
                setOnClickListener { dialog.dismiss(); onSelect(index) }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)))
        }
        if (footerLabel != null && onFooter != null) {
            list.addView(TextView(context).apply {
                text = footerLabel
                textSize = 12f
                setTextColor(accent)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(11), 0, dp(5), 0)
                setOnClickListener { dialog.dismiss(); onFooter() }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)).apply {
                topMargin = dp(14)
            })
        }
        panel.addView(ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            addView(list)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        dialog.setContentView(panel)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply {
                width = (context.resources.displayMetrics.widthPixels * 0.88f).toInt()
                height = WindowManager.LayoutParams.MATCH_PARENT
                gravity = Gravity.START
                dimAmount = 0.55f
            }
        }
        dialog.show()
        dialog.window?.setLayout((context.resources.displayMetrics.widthPixels * 0.88f).toInt(),
            WindowManager.LayoutParams.MATCH_PARENT)
    }
}
