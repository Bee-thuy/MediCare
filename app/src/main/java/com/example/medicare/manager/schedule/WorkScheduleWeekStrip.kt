package com.example.medicare.manager.schedule

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.example.medicare.R
import java.time.LocalDate

internal object WorkScheduleWeekStrip {
    fun render(context: Context, container: LinearLayout, selected: LocalDate,
               backgroundRes: Int = R.drawable.manager_week_day_background,
               onSelect: (LocalDate) -> Unit) {
        container.removeAllViews()
        val monday = selected.minusDays((selected.dayOfWeek.value - 1).toLong())
        val labels = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
        labels.forEachIndexed { index, label ->
            val date = monday.plusDays(index.toLong())
            val day = TextView(context).apply {
                text = "$label\n${date.dayOfMonth}"
                textSize = 10f
                gravity = Gravity.CENTER
                isSelected = date == selected
                setTextColor(context.getColor(if (isSelected) R.color.white else R.color.manager_muted))
                setBackgroundResource(backgroundRes)
                setOnClickListener { onSelect(date) }
            }
            container.addView(day, LinearLayout.LayoutParams(0, dp(context, 50), 1f).apply {
                if (index > 0) leftMargin = dp(context, 4)
            })
        }
    }

    fun formatDate(date: LocalDate) = "%02d/%02d/%04d".format(date.dayOfMonth, date.monthValue, date.year)

    private fun dp(context: Context, value: Int) = (value * context.resources.displayMetrics.density + 0.5f).toInt()
}
