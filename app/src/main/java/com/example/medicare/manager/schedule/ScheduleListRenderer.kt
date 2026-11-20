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
import com.example.medicare.model.Appointment
import com.example.medicare.model.AppointmentStatus
import java.time.LocalTime

/** Compact shift rows using the HTML schedule's time, status and action rhythm. */
internal object ScheduleListRenderer {
    fun renderSlots(
        context: Context,
        container: LinearLayout,
        emptyView: View,
        schedules: List<DoctorSchedule>,
        appointments: List<Appointment>,
        onScheduleClick: (DoctorSchedule) -> Unit,
        onSlotToggle: (DoctorSchedule, String, Boolean) -> Unit,
    ) {
        container.removeAllViews()
        var count = 0
        schedules.forEach { schedule ->
            val start = runCatching { LocalTime.parse(schedule.startTime) }.getOrNull() ?: return@forEach
            val end = runCatching { LocalTime.parse(schedule.endTime) }.getOrNull() ?: return@forEach
            val duration = schedule.appointmentDurationMinutes.coerceAtLeast(1).toLong()
            if (count < 100 && !start.plusMinutes(duration).isAfter(end)) {
                container.addView(text(context,
                    "${schedule.shiftName}  ·  ${schedule.startTime}–${schedule.endTime}  ·  ${schedule.room.ifBlank { "Chưa có phòng" }}",
                    11, R.color.manager_blue, true),
                    LinearLayout.LayoutParams(-1, -2).apply {
                        topMargin = dp(context, 11); bottomMargin = dp(context, 9)
                        marginStart = dp(context, 4)
                    })
            }
            var time = start
            while (!time.plusMinutes(duration).isAfter(end) && count < 100) {
                val slot = time.toString()
                val matches = appointments.filter { it.scheduleId == schedule.id && it.startTime == slot }
                val booked = matches.any { it.status in setOf(AppointmentStatus.CONFIRMED,
                    AppointmentStatus.ARRIVED, AppointmentStatus.IN_PROGRESS, AppointmentStatus.COMPLETED) }
                val pending = matches.any { it.status == AppointmentStatus.PENDING }
                val locked = schedule.isLocked || slot in schedule.lockedSlots
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setBackgroundResource(R.drawable.manager_card_background)
                    setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12))
                    layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 9) }
                }
                row.addView(TextView(context).apply {
                    text = if (booked || locked) "🔒" else "🕒"
                    textSize = 19f
                    gravity = Gravity.CENTER
                    background = GradientDrawable().apply {
                        setColor(if (booked || locked) 0xFFFFE8E8.toInt() else 0xFFE8F8F1.toInt())
                        cornerRadius = dp(context, 14).toFloat()
                    }
                }, LinearLayout.LayoutParams(dp(context, 42), dp(context, 42)).apply { rightMargin = dp(context, 10) })
                val info = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
                info.addView(text(context, "$slot – ${time.plusMinutes(duration)}", 13, R.color.manager_ink, true))
                info.addView(text(context, "${schedule.doctorName} · ${schedule.shiftName}", 10, R.color.manager_muted, false))
                info.addView(text(context, when {
                    booked -> "Đã có lịch xác nhận"
                    pending -> "Có yêu cầu chờ duyệt"
                    locked -> "Đã khóa nhận lịch"
                    else -> "Còn trống"
                }, 10, when {
                    booked || locked -> R.color.manager_red
                    pending -> R.color.manager_orange
                    else -> R.color.manager_green
                }, false))
                row.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
                val button = text(context, when {
                    booked -> "Đã đặt"
                    pending -> "Chờ duyệt"
                    schedule.isLocked -> "Chi tiết"
                    locked -> "Mở ca"
                    else -> "Khóa ca"
                }, 10, R.color.manager_blue, true).apply {
                    setPadding(dp(context, 6), dp(context, 10), dp(context, 6), dp(context, 10))
                    setBackgroundResource(when {
                        booked -> R.drawable.manager_status_inactive_background
                        pending -> R.drawable.manager_appointment_pending_background
                        locked -> R.drawable.manager_status_active_background
                        else -> R.drawable.patient_secondary_button_background
                    })
                    setTextColor(context.getColor(when {
                        booked -> R.color.manager_red
                        pending -> R.color.manager_orange
                        locked -> R.color.manager_green
                        else -> R.color.manager_blue
                    }))
                    setOnClickListener {
                        when {
                            booked || pending || schedule.isLocked -> onScheduleClick(schedule)
                            else -> onSlotToggle(schedule, slot, !locked)
                        }
                    }
                }
                row.addView(button)
                row.setOnClickListener { onScheduleClick(schedule) }
                container.addView(row)
                count++
                time = time.plusMinutes(duration)
            }
        }
        emptyView.isVisible = count == 0
    }

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
