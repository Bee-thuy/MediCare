package com.example.medicare.manager.dashboard

import android.os.Bundle
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.data.appointment.AppointmentRepository
import com.example.medicare.databinding.ActivityManagerReportBinding
import com.example.medicare.model.AppointmentStatus
import com.example.medicare.model.Appointment
import java.time.LocalDate

class ManagerReportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManagerReportBinding
    private var latest: List<Appointment> = emptyList()
    private var visible: List<Appointment> = emptyList()
    private val createCsv = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) return@registerForActivityResult
        runCatching { contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write("\uFEFF" + toCsv(visible)) } }
            .onSuccess { Toast.makeText(this, "Đã xuất báo cáo CSV.", Toast.LENGTH_SHORT).show() }
            .onFailure { Toast.makeText(this, it.message ?: "Không thể xuất báo cáo.", Toast.LENGTH_LONG).show() }
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        binding = ActivityManagerReportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.reportBack.setOnClickListener { finish() }
        val choices = listOf("7 ngày gần nhất", "Tháng hiện tại", "Năm hiện tại")
        binding.reportPeriod.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, choices)
        binding.reportPeriod.setSelection(1)
        binding.reportPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = render()
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        binding.reportExport.setOnClickListener {
            if (visible.isEmpty()) Toast.makeText(this, "Chưa có dữ liệu để xuất.", Toast.LENGTH_SHORT).show()
            else createCsv.launch("bao-cao-lich-kham.csv")
        }
        load()
    }

    private fun load() = AppointmentRepository().getAppointments { result ->
        if (isFinishing || isDestroyed) return@getAppointments
        result.onSuccess { appointments -> latest = appointments; render() }
            .onFailure { Toast.makeText(this, it.message ?: "Không thể tải báo cáo.", Toast.LENGTH_LONG).show() }
    }

    private fun render() {
        val today = LocalDate.now()
        visible = latest.filter { appointment ->
            val date = runCatching { LocalDate.parse(appointment.appointmentDate) }.getOrNull() ?: return@filter false
            when (binding.reportPeriod.selectedItemPosition) {
                0 -> !date.isBefore(today.minusDays(6)) && !date.isAfter(today)
                2 -> date.year == today.year
                else -> date.year == today.year && date.month == today.month
            }
        }
        showKpi(binding.reportTotal, "📋", visible.size, "Yêu cầu đặt khám")
        showKpi(binding.reportConfirmed, "✅", visible.count { it.status == AppointmentStatus.CONFIRMED }, "Đã xác nhận")
        showKpi(binding.reportCompleted, "🩺", visible.count { it.status == AppointmentStatus.COMPLETED }, "Hoàn thành")
        showKpi(binding.reportDeclined, "📌", visible.count { it.status in setOf(AppointmentStatus.CANCELLED, AppointmentStatus.REJECTED) }, "Hủy / từ chối")
        showChart(today)
        showSpecialties()
    }

    private fun showKpi(view: TextView, icon: String, count: Int, title: String) {
        val value = "$icon\n$count\n$title"
        val text = SpannableString(value)
        val firstBreak = value.indexOf('\n')
        val secondBreak = value.indexOf('\n', firstBreak + 1)
        text.setSpan(AbsoluteSizeSpan(19, true), 0, firstBreak, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(AbsoluteSizeSpan(22, true), firstBreak + 1, secondBreak, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(StyleSpan(Typeface.BOLD), firstBreak + 1, secondBreak, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(AbsoluteSizeSpan(11, true), secondBreak + 1, value.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(ForegroundColorSpan(getColor(com.example.medicare.R.color.manager_muted)), secondBreak + 1, value.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        view.text = text
    }

    private fun showChart(today: LocalDate) {
        binding.reportChart.removeAllViews()
        val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
        val counts = days.map { day -> visible.count { it.appointmentDate == day.toString() } }
        val maximum = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
        days.forEachIndexed { index, day ->
            val column = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
            val count = TextView(this).apply {
                text = counts[index].toString(); textSize = 10f; gravity = Gravity.CENTER
                setTextColor(getColor(com.example.medicare.R.color.manager_muted))
            }
            val bar = View(this).apply {
                background = GradientDrawable().apply {
                    setColor(getColor(com.example.medicare.R.color.manager_blue))
                    cornerRadius = dp(5).toFloat()
                }
            }
            val label = TextView(this).apply {
                text = if (index == 6) "Hôm nay" else day.dayOfMonth.toString()
                textSize = 10f; gravity = Gravity.CENTER
                setTextColor(getColor(com.example.medicare.R.color.manager_muted))
            }
            column.addView(count, LinearLayout.LayoutParams(-1, dp(18)))
            column.addView(bar, LinearLayout.LayoutParams(dp(18), dp(90 * counts[index] / maximum).coerceAtLeast(dp(3))))
            column.addView(label, LinearLayout.LayoutParams(-1, dp(24)))
            binding.reportChart.addView(column, LinearLayout.LayoutParams(0, -1, 1f))
        }
    }

    private fun showSpecialties() {
        binding.reportSpecialties.removeAllViews()
        val counts = visible.groupingBy { it.specialtyName.ifBlank { "Chưa xác định" } }
            .eachCount().entries.sortedByDescending { it.value }.take(5)
        if (counts.isEmpty()) {
            binding.reportSpecialties.addView(TextView(this).apply {
                text = "Chưa có dữ liệu trong khoảng thời gian này."
                setTextColor(getColor(com.example.medicare.R.color.manager_muted))
                setPadding(dp(8), dp(12), 0, dp(12))
            })
            return
        }
        counts.forEach { entry ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                setBackgroundResource(com.example.medicare.R.drawable.manager_card_background)
            }
            val title = TextView(this).apply {
                text = "${entry.key}   ·   ${entry.value} lượt"
                textSize = 12f
                setTextColor(getColor(com.example.medicare.R.color.manager_ink))
            }
            val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = visible.size.coerceAtLeast(1)
                this.progress = entry.value
                progressTintList = ColorStateList.valueOf(getColor(com.example.medicare.R.color.manager_blue))
                progressBackgroundTintList = ColorStateList.valueOf(Color.rgb(232, 238, 247))
            }
            card.addView(title)
            card.addView(progress, LinearLayout.LayoutParams(-1, dp(8)).apply { topMargin = dp(10) })
            binding.reportSpecialties.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun toCsv(all: List<Appointment>): String = buildString {
        appendLine("Mã lịch,Ngày,Giờ bắt đầu,Giờ kết thúc,Bệnh nhân,Bác sĩ,Chuyên khoa,Trạng thái,Lý do")
        all.forEach { a -> appendLine(listOf(a.id, a.appointmentDate, a.startTime, a.endTime, a.patientName, a.doctorName, a.specialtyName, a.status.name, a.reason).joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }) }
    }
}
