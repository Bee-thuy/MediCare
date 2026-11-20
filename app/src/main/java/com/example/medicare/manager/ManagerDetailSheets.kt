package com.example.medicare.manager

import android.app.Activity
import android.content.Intent
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.medicare.R
import com.example.medicare.data.appointment.AppointmentRepository
import com.example.medicare.data.review.DoctorReviewRepository
import com.example.medicare.model.AccountStatus
import com.example.medicare.model.Appointment
import com.example.medicare.model.AppointmentStatus
import com.example.medicare.model.Doctor
import com.example.medicare.model.Patient
import com.example.medicare.model.Specialty
import com.example.medicare.manager.doctor.SpecialtyVisuals
import com.example.medicare.manager.appointment.ProposeScheduleActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Admin detail modals use the same card and key/value structure as the HTML reference. */
object ManagerDetailSheets {
    fun specialty(activity: Activity, specialty: Specialty, doctorCount: Int,
                  onEdit: () -> Unit, onToggle: () -> Unit) {
        val (sheet, body) = create(activity, "Chi tiết chuyên khoa")
        val summary = card(activity, body)
        val header = horizontal(activity, summary)
        header.addView(ImageView(activity).apply {
            setImageResource(SpecialtyVisuals.forSpecialty(specialty).drawable)
            setPadding(dp(activity, 9), dp(activity, 9), dp(activity, 9), dp(activity, 9))
            setBackgroundResource(R.drawable.manager_appointment_avatar_background)
        }, LinearLayout.LayoutParams(dp(activity, 52), dp(activity, 52)))
        header.addView(text(activity, specialty.name, 15, true),
            LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(activity, 12) })
        if (specialty.description.isNotBlank()) summary.addView(text(activity, specialty.description, 11, false).apply {
            setTextColor(activity.getColor(R.color.manager_muted))
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(activity, 10) })
        detail(activity, summary, "Trạng thái", if (specialty.isActive) "Hiển thị" else "Đã ẩn")
        detail(activity, summary, "Số bác sĩ", "$doctorCount bác sĩ")
        val actions = horizontal(activity, body).apply { setPadding(0, dp(activity, 10), 0, 0) }
        action(activity, actions, "Sửa tên", false) { sheet.dismiss(); onEdit() }
        action(activity, actions, if (specialty.isActive) "Ẩn chuyên khoa" else "Hiển thị", false) {
            sheet.dismiss(); onToggle()
        }
        activity.window.decorView.post { if (!activity.isFinishing) sheet.show() }
    }

    fun appointment(activity: Activity, appointment: Appointment, occupied: Boolean, onChanged: () -> Unit) {
        val (sheet, body) = create(activity, "Chi tiết lịch khám")
        val summary = card(activity, body)
        val header = horizontal(activity, summary)
        header.addView(text(activity, appointment.patientName.ifBlank { "Bệnh nhân" }, 14, true),
            LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(text(activity, status(appointment), 10, true).apply {
            setTextColor(activity.getColor(R.color.manager_blue))
        })
        detail(activity, summary, "Mã lịch", appointment.id)
        detail(activity, summary, "Bác sĩ", "BS. ${appointment.doctorName}")
        detail(activity, summary, "Chuyên khoa", appointment.specialtyName)
        detail(activity, summary, "Ngày khám", date(appointment.appointmentDate))
        detail(activity, summary, "Thời gian", appointment.startTime)
        detail(activity, summary, "Lý do", appointment.reason.ifBlank { "Chưa nhập" })
        if (appointment.status == AppointmentStatus.PENDING) {
            if (occupied) {
                val warning = text(activity,
                    "Khung ${appointment.startTime} đã có lịch được duyệt. Hãy đề xuất giờ khác hoặc từ chối yêu cầu.", 11, false).apply {
                    setTextColor(activity.getColor(R.color.manager_red))
                    setBackgroundResource(R.drawable.manager_status_inactive_background)
                    setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10))
                }
                body.addView(warning, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(activity, 8) })
            }
            val actions = horizontal(activity, body).apply { setPadding(0, dp(activity, 12), 0, 0) }
            if (!occupied) decision(activity, actions, "✓ Xác nhận", R.drawable.manager_status_active_background,
                R.color.manager_green, false) {
                AppointmentRepository().approveAppointmentWithNotification(appointment.id) { result ->
                    result.onSuccess {
                        sheet.dismiss(); onChanged()
                        Toast.makeText(activity, "Đã xác nhận lịch khám.", Toast.LENGTH_SHORT).show()
                    }.onFailure { Toast.makeText(activity, it.message ?: "Không xác nhận được lịch.", Toast.LENGTH_LONG).show() }
                }
            }
            decision(activity, actions, "✕ Từ chối", R.drawable.manager_status_inactive_background,
                R.color.manager_red, false) {
                AlertDialog.Builder(activity).setTitle("Từ chối lịch khám?")
                    .setMessage("Bệnh nhân sẽ được thông báo về quyết định này.")
                    .setNegativeButton("Đóng", null)
                    .setPositiveButton("Từ chối") { _, _ ->
                        AppointmentRepository().updateAppointmentStatusWithNotification(appointment.id,
                            AppointmentStatus.REJECTED, "Lịch khám bị từ chối",
                            "Yêu cầu đặt lịch ${date(appointment.appointmentDate)} · ${appointment.startTime} chưa được duyệt.") { result ->
                            result.onSuccess { sheet.dismiss(); onChanged() }
                                .onFailure { Toast.makeText(activity, it.message ?: "Không từ chối được lịch.", Toast.LENGTH_LONG).show() }
                        }
                    }.show()
            }
            decision(activity, body, "🔄 Đề xuất giờ khác", R.drawable.patient_secondary_button_background,
                R.color.manager_blue, true) {
                sheet.dismiss()
                activity.startActivity(Intent(activity, ProposeScheduleActivity::class.java)
                    .putExtra(ProposeScheduleActivity.EXTRA_APPOINTMENT_ID, appointment.id)
                    .putExtra(ProposeScheduleActivity.EXTRA_SCHEDULE_ID, appointment.scheduleId)
                    .putExtra(ProposeScheduleActivity.EXTRA_PATIENT_ID, appointment.patientId)
                    .putExtra(ProposeScheduleActivity.EXTRA_DATE, appointment.appointmentDate)
                    .putExtra(ProposeScheduleActivity.EXTRA_CURRENT_TIME, "${appointment.startTime} - ${appointment.endTime}"))
            }
        }
        activity.window.decorView.post { if (!activity.isFinishing) sheet.show() }
    }

    fun doctor(activity: Activity, doctor: Doctor, onEdit: () -> Unit, onToggle: () -> Unit) {
        val (sheet, body) = create(activity, "Thông tin bác sĩ")
        val profile = card(activity, body)
        profile.addView(text(activity, "👨🏻‍⚕️", 37, false).apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(-1, dp(activity, 56)))
        profile.addView(text(activity, "BS. ${doctor.fullName}", 16, true).apply { gravity = Gravity.CENTER })
        profile.addView(text(activity, doctor.specialty, 11, false).apply {
            gravity = Gravity.CENTER; setTextColor(activity.getColor(R.color.manager_muted))
        })
        val rating = text(activity, "⭐ Chưa có đánh giá · ${doctor.experienceYears.ifBlank { "0" }} năm kinh nghiệm", 11, true).apply {
            gravity = Gravity.CENTER; setTextColor(activity.getColor(R.color.manager_blue))
        }
        profile.addView(rating)
        detail(activity, profile, "Điện thoại", doctor.phone.ifBlank { "Chưa cập nhật" })
        detail(activity, profile, "Tài khoản", if (doctor.isActive) "Hoạt động" else "Đã khóa")
        val count = detail(activity, profile, "Lịch đã đặt", "Đang tải...")
        val actions = horizontal(activity, body).apply { setPadding(0, dp(activity, 14), 0, 0) }
        action(activity, actions, "Sửa thông tin", false) { sheet.dismiss(); onEdit() }
        action(activity, actions, if (doctor.isActive) "Khóa tài khoản" else "Mở khóa", false, danger = true) {
            sheet.dismiss(); onToggle()
        }
        activity.window.decorView.post {
            if (activity.isFinishing) return@post
            sheet.show()
            AppointmentRepository().getAppointmentsForDoctor(doctor.id) { result ->
                if (sheet.isShowing) count.text = "${result.getOrDefault(emptyList()).size} lịch"
            }
            DoctorReviewRepository().getAll { result ->
                if (!sheet.isShowing) return@getAll
                val values = result.getOrDefault(emptyList()).filter { it.doctorId == doctor.id }
                if (values.isNotEmpty()) rating.text = "⭐ ${"%.1f".format(values.map { it.rating }.average())} · ${doctor.experienceYears.ifBlank { "0" }} năm kinh nghiệm"
            }
        }
    }

    fun patient(activity: Activity, patient: Patient, onToggle: () -> Unit) {
        val (sheet, body) = create(activity, "Chi tiết bệnh nhân")
        val profile = card(activity, body)
        val head = horizontal(activity, profile)
        head.addView(text(activity, "👤", 23, false).apply {
            gravity = Gravity.CENTER; setBackgroundResource(R.drawable.manager_appointment_avatar_background)
        }, LinearLayout.LayoutParams(dp(activity, 43), dp(activity, 43)))
        val summary = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(activity, 10), 0, 0, 0) }
        summary.addView(text(activity, patient.fullName.ifBlank { "Bệnh nhân" }, 12, true))
        summary.addView(text(activity, if (patient.accountStatus == AccountStatus.ACTIVE) "Đang dùng" else "Đã khóa", 10, false).apply {
            setTextColor(activity.getColor(if (patient.accountStatus == AccountStatus.ACTIVE) R.color.manager_green else R.color.manager_red))
        })
        head.addView(summary, LinearLayout.LayoutParams(0, -2, 1f))
        detail(activity, profile, "Số điện thoại", patient.phone.ifBlank { "Chưa cập nhật" })
        detail(activity, profile, "Email", patient.email.ifBlank { "Chưa cập nhật" })
        val count = detail(activity, profile, "Lịch liên quan", "Đang tải...")
        body.addView(text(activity, "Lịch sử đặt khám", 13, true), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(activity, 17) })
        val history = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        body.addView(history, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(activity, 8) })
        action(activity, body, if (patient.accountStatus == AccountStatus.ACTIVE) "Khóa tài khoản" else "Mở tài khoản", true, danger = true) {
            sheet.dismiss(); onToggle()
        }
        activity.window.decorView.post {
            if (activity.isFinishing) return@post
            sheet.show()
            AppointmentRepository().getAppointmentsForPatient(patient.id) { result ->
                if (!sheet.isShowing) return@getAppointmentsForPatient
                result.onSuccess { appointments ->
                    count.text = "${appointments.size} lần"
                    history.removeAllViews()
                    if (appointments.isEmpty()) history.addView(text(activity, "Bệnh nhân chưa có lịch khám.", 11, false))
                    appointments.take(8).forEach { appointment ->
                        val item = card(activity, history)
                        item.addView(text(activity, "${appointment.id} · ${date(appointment.appointmentDate)} ${appointment.startTime}", 11, true))
                        item.addView(text(activity, status(appointment), 10, false).apply {
                            setTextColor(activity.getColor(R.color.manager_muted))
                        })
                    }
                }.onFailure {
                    count.text = "Không tải được"
                    Toast.makeText(activity, it.message ?: "Không tải được lịch sử.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun create(activity: Activity, title: String): Pair<BottomSheetDialog, LinearLayout> {
        val sheet = BottomSheetDialog(activity)
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(activity, 20), dp(activity, 18), dp(activity, 20), dp(activity, 24))
            setBackgroundResource(R.drawable.patient_record_sheet_background)
        }
        val heading = horizontal(activity, root)
        heading.addView(text(activity, title, 17, true), LinearLayout.LayoutParams(0, -2, 1f))
        heading.addView(text(activity, "×", 22, false).apply {
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.manager_avatar_placeholder)
            setOnClickListener { sheet.dismiss() }
        }, LinearLayout.LayoutParams(dp(activity, 32), dp(activity, 32)))
        val scroll = ScrollView(activity).apply { isVerticalScrollBarEnabled = false }
        root.addView(scroll, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(activity, 12) })
        val body = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(body)
        sheet.setContentView(root)
        return sheet to body
    }

    private fun card(activity: Activity, parent: LinearLayout): LinearLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(activity, 14), dp(activity, 13), dp(activity, 14), dp(activity, 13))
        setBackgroundResource(R.drawable.manager_card_background)
        parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(activity, 8) })
    }

    private fun horizontal(activity: Activity, parent: LinearLayout): LinearLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        parent.addView(this, LinearLayout.LayoutParams(-1, -2))
    }

    private fun detail(activity: Activity, parent: LinearLayout, title: String, value: String): TextView {
        val line = horizontal(activity, parent)
        line.setPadding(0, dp(activity, 8), 0, dp(activity, 8))
        line.addView(text(activity, title, 11, false).apply { setTextColor(activity.getColor(R.color.manager_muted)) },
            LinearLayout.LayoutParams(dp(activity, 105), -2))
        val answer = text(activity, value, 11, true).apply { gravity = Gravity.END }
        line.addView(answer, LinearLayout.LayoutParams(0, -2, 1f))
        return answer
    }

    private fun action(activity: Activity, parent: LinearLayout, label: String, full: Boolean,
                       danger: Boolean = false, click: () -> Unit) {
        val view = text(activity, label, 11, true).apply {
            gravity = Gravity.CENTER
            setTextColor(activity.getColor(if (danger) R.color.manager_red else R.color.manager_blue))
            setBackgroundResource(R.drawable.patient_secondary_button_background)
            setOnClickListener { click() }
        }
        parent.addView(view, LinearLayout.LayoutParams(if (full) -1 else 0, dp(activity, 42), if (full) 0f else 1f).apply {
            topMargin = if (full) dp(activity, 12) else 0
            marginEnd = if (full) 0 else dp(activity, 5)
        })
    }

    private fun decision(activity: Activity, parent: LinearLayout, label: String,
                         background: Int, color: Int, full: Boolean, click: () -> Unit) {
        parent.addView(text(activity, label, 11, true).apply {
            gravity = Gravity.CENTER
            setTextColor(activity.getColor(color))
            setBackgroundResource(background)
            setOnClickListener { click() }
        }, LinearLayout.LayoutParams(if (full) -1 else 0, dp(activity, 42), if (full) 0f else 1f).apply {
            topMargin = if (full) dp(activity, 10) else 0
            marginEnd = if (full) 0 else dp(activity, 5)
        })
    }

    private fun text(activity: Activity, value: String, size: Int, bold: Boolean) = TextView(activity).apply {
        text = value; textSize = size.toFloat()
        setTextColor(activity.getColor(R.color.manager_ink))
        if (bold) typeface = activity.resources.getFont(R.font.inter_bold)
    }

    private fun status(appointment: Appointment) = when (appointment.status) {
        AppointmentStatus.PENDING -> "Chờ duyệt"
        AppointmentStatus.CONFIRMED -> "Đã xác nhận"
        AppointmentStatus.ARRIVED -> "Đã đến"
        AppointmentStatus.IN_PROGRESS -> "Đang khám"
        AppointmentStatus.COMPLETED -> "Hoàn thành"
        AppointmentStatus.REJECTED -> "Từ chối"
        AppointmentStatus.CANCELLED -> "Đã hủy"
    }

    private fun date(raw: String) = runCatching { LocalDate.parse(raw).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrDefault(raw)
    private fun dp(activity: Activity, value: Int) = (value * activity.resources.displayMetrics.density + .5f).toInt()
}
