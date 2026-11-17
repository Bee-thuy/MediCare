package com.example.medicare.manager.schedule

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.example.medicare.R
import com.example.medicare.model.DoctorSchedule

/** Compact shift rows using the HTML schedule's time, status and action rhythm. */
internal object ScheduleListRenderer {
    fun render(
        context: Context,
        container: LinearLayout,
        emptyView: View,
        schedules: List<DoctorSchedule>,
        onScheduleClick: ((DoctorSchedule) -> Unit)? = null,
    ) {
        container.removeAllViews()
        emptyView.isVisible = schedules.isEmpty()
        schedules.forEach { schedule ->
            container.addView(scheduleRow(context, schedule).apply {
                setOnClickListener { onScheduleClick?.invoke(schedule) }
            })
        }
    }

    private fun scheduleRow(context: Context, schedule: DoctorSchedule): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.manager_card_background)
            setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 9) }
        }
        val avatar = TextView(context).apply {
            text = if (schedule.isLocked) "🔒" else "🕒"
            textSize = 19f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(if (schedule.isLocked) 0xFFFFE8E8.toInt() else 0xFFE8F8F1.toInt())
                cornerRadius = dp(context, 14).toFloat()
            }
        }
        row.addView(avatar, LinearLayout.LayoutParams(dp(context, 42), dp(context, 42)).apply {
            rightMargin = dp(context, 10)
        })
        val information = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        information.addView(text(context, "${schedule.startTime} – ${schedule.endTime}", 13, R.color.manager_ink, true))
        information.addView(text(context, "${schedule.doctorName} · ${schedule.shiftName}", 10, R.color.manager_muted, false))
        information.addView(text(context, if (schedule.isLocked) "Đã khóa nhận lịch" else "Đang nhận lịch", 10,
            if (schedule.isLocked) R.color.manager_red else R.color.manager_green, false))
        row.addView(information, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(text(context, "Chi tiết  ›", 10, R.color.manager_blue, true))
        return row
    }

    private fun text(context: Context, value: String, sizeSp: Int, colorRes: Int, bold: Boolean) = TextView(context).apply {
        text = value
        setTextColor(context.getColor(colorRes))
        textSize = sizeSp.toFloat()
        if (bold) setTypeface(typeface, Typeface.BOLD)
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
    }

    private fun dp(context: Context, value: Int) = (value * context.resources.displayMetrics.density + 0.5f).toInt()
}
