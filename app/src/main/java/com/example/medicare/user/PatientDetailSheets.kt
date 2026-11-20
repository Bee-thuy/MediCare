package com.example.medicare.user

import android.app.Activity
import android.content.Intent
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.medicare.R
import com.example.medicare.data.appointment.AppointmentRepository
import com.example.medicare.data.schedule.ScheduleRepository
import com.example.medicare.model.Appointment
import com.example.medicare.model.AppointmentStatus
import com.example.medicare.model.Doctor
import com.example.medicare.user.review.MyReviewsActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Native patient detail sheets following the HTML card and key/value modal layout. */
object PatientDetailSheets {
    fun appointment(activity: Activity, appointment: Appointment, onChanged: () -> Unit = {}) {
        val (sheet, body) = create(activity, "Chi tiết lịch khám")
        val card = card(activity, body)
        val heading = row(activity, card)
        heading.addView(label(activity, appointment.patientName.ifBlank { "Bệnh nhân" }, 14, true),
            LinearLayout.LayoutParams(0, -2, 1f))
        heading.addView(label(activity, status(appointment.status), 10, true).apply {
            setTextColor(activity.getColor(R.color.patient_purple))
        })
        detail(activity, card, "Mã lịch", appointment.id)
        detail(activity, card, "Bác sĩ", "BS. ${appointment.doctorName}")
        detail(activity, card, "Chuyên khoa", appointment.specialtyName)
        detail(activity, card, "Ngày khám", date(appointment.appointmentDate))
        detail(activity, card, "Thời gian", listOf(appointment.startTime, appointment.endTime).filter(String::isNotBlank).joinToString(" – "))
        detail(activity, card, "Lý do", appointment.reason.ifBlank { "Chưa nhập" })
        if (appointment.rescheduledFromId.isNotBlank()) detail(activity, card, "Lịch đổi từ", appointment.rescheduledFromId)
        if (appointment.status in setOf(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED)) {
            button(activity, body, "✕ Hủy yêu cầu", false) {
                AlertDialog.Builder(activity).setTitle("Hủy lịch khám")
                    .setMessage("Bạn muốn hủy lịch khám này?")
                    .setNegativeButton("Giữ lịch", null)
                    .setPositiveButton("Hủy lịch") { _, _ ->
                        AppointmentRepository().cancelAppointmentByPatient(appointment.id, "Bệnh nhân hủy lịch") { result ->
                            result.onSuccess {
                                sheet.dismiss()
                                onChanged()
                            }.onFailure { Toast.makeText(activity, it.message ?: "Không hủy được lịch khám.", Toast.LENGTH_LONG).show() }
                        }
                    }.show()
            }
        }
        if (appointment.status == AppointmentStatus.COMPLETED) button(activity, body, "⭐ Đánh giá bác sĩ") {
            sheet.dismiss()
            activity.startActivity(Intent(activity, MyReviewsActivity::class.java)
                .putExtra(MyReviewsActivity.EXTRA_OPEN_APPOINTMENT_ID, appointment.id))
        }
        activity.window.decorView.post { if (!activity.isFinishing) sheet.show() }
    }

    fun doctor(activity: Activity, doctor: Doctor, rating: Pair<Double, Int>? = null, onBook: () -> Unit) {
        val (sheet, body) = create(activity, "Chi tiết bác sĩ")
        val card = card(activity, body)
        card.addView(label(activity, "👨🏻‍⚕️", 38, false).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(-1, dp(activity, 58)))
        card.addView(label(activity, "BS. ${doctor.fullName}", 17, true).apply { gravity = Gravity.CENTER })
        card.addView(label(activity, "${doctor.specialty} · ${doctor.experienceYears.ifBlank { "Chưa cập nhật" }} năm kinh nghiệm", 11, false)
            .apply { gravity = Gravity.CENTER; setTextColor(activity.getColor(R.color.manager_muted)) })
        card.addView(label(activity, if (rating == null) "⭐ Chưa có đánh giá" else "⭐ ${"%.1f".format(rating.first)} (${rating.second} đánh giá)", 12, true)
            .apply { gravity = Gravity.CENTER; setTextColor(activity.getColor(R.color.patient_purple)) })
        detail(activity, card, "Nơi làm việc", "Chưa cập nhật")
        detail(activity, card, "Chuyên khoa", doctor.specialty)
        detail(activity, card, "Giờ khám", "Theo lịch làm việc")
        body.addView(label(activity, "Khung giờ còn trống", 13, true), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(activity, 16) })
        val schedules = label(activity, "Đang tải lịch làm việc...", 11, false).apply { setTextColor(activity.getColor(R.color.manager_muted)) }
        body.addView(schedules, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(activity, 8) })
        button(activity, body, "📅 Đặt lịch với bác sĩ này") { sheet.dismiss(); onBook() }
        activity.window.decorView.post {
            if (activity.isFinishing) return@post
            sheet.show()
            ScheduleRepository().getOpenSchedulesForDoctor(doctor.id) { result ->
                if (!sheet.isShowing) return@getOpenSchedulesForDoctor
                val upcoming = result.getOrDefault(emptyList()).take(3)
                schedules.text = if (upcoming.isEmpty()) "Chưa có khung giờ trống." else upcoming.joinToString("   ·   ") {
                    "${date(it.workDate)} ${it.startTime}–${it.endTime}"
                }
            }
        }
    }

    private fun create(activity: Activity, title: String): Pair<BottomSheetDialog, LinearLayout> {
        val sheet = BottomSheetDialog(activity)
        val outer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(activity, 20), dp(activity, 18), dp(activity, 20), dp(activity, 24))
            setBackgroundResource(R.drawable.patient_record_sheet_background)
        }
        val head = row(activity, outer)
        head.addView(label(activity, title, 17, true), LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(label(activity, "×", 24, false).apply {
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.patient_avatar_background)
            setOnClickListener { sheet.dismiss() }
        }, LinearLayout.LayoutParams(dp(activity, 32), dp(activity, 32)))
        val scroll = android.widget.ScrollView(activity).apply { isFillViewport = false; isVerticalScrollBarEnabled = false }
        outer.addView(scroll, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(activity, 12) })
        val body = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(body)
        sheet.setContentView(outer)
        return sheet to body
    }

    private fun card(activity: Activity, parent: LinearLayout): LinearLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(activity, 15), dp(activity, 15), dp(activity, 15), dp(activity, 15))
        setBackgroundResource(R.drawable.manager_card_background)
        parent.addView(this, LinearLayout.LayoutParams(-1, -2))
    }

    private fun row(activity: Activity, parent: LinearLayout): LinearLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        parent.addView(this, LinearLayout.LayoutParams(-1, -2))
    }

    private fun detail(activity: Activity, parent: LinearLayout, title: String, value: String) {
        val row = row(activity, parent)
        row.setPadding(0, dp(activity, 9), 0, dp(activity, 9))
        row.addView(label(activity, title, 11, false).apply { setTextColor(activity.getColor(R.color.manager_muted)) },
            LinearLayout.LayoutParams(dp(activity, 105), -2))
        row.addView(label(activity, value, 11, true).apply { gravity = Gravity.END }, LinearLayout.LayoutParams(0, -2, 1f))
        parent.addView(View(activity).apply { setBackgroundResource(R.drawable.patient_record_divider) }, LinearLayout.LayoutParams(-1, dp(activity, 1)))
    }

    private fun label(activity: Activity, text: String, size: Int, bold: Boolean) = TextView(activity).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(activity.getColor(R.color.manager_ink))
        if (bold) typeface = activity.resources.getFont(R.font.inter_bold)
    }

    private fun button(activity: Activity, parent: LinearLayout, text: String, primary: Boolean = true, action: () -> Unit) {
        parent.addView(label(activity, text, 11, true).apply {
            gravity = Gravity.CENTER
            setTextColor(activity.getColor(if (primary) R.color.white else R.color.manager_red))
            setBackgroundResource(if (primary) R.drawable.patient_booking_button_background else R.drawable.patient_feedback_field_background)
            setOnClickListener { action() }
        }, LinearLayout.LayoutParams(-1, dp(activity, 42)).apply { topMargin = dp(activity, 14) })
    }

    private fun status(value: AppointmentStatus) = when (value) {
        AppointmentStatus.PENDING -> "Chờ duyệt"
        AppointmentStatus.CONFIRMED -> "Đã xác nhận"
        AppointmentStatus.ARRIVED -> "Đã đến"
        AppointmentStatus.IN_PROGRESS -> "Đang khám"
        AppointmentStatus.COMPLETED -> "Hoàn thành"
        AppointmentStatus.REJECTED -> "Đã từ chối"
        AppointmentStatus.CANCELLED -> "Đã hủy"
    }

    private fun date(value: String) = runCatching { LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrDefault(value)
    private fun dp(activity: Activity, value: Int) = (value * activity.resources.displayMetrics.density + .5f).toInt()
}
