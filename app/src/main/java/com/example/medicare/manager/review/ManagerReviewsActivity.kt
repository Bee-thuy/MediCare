package com.example.medicare.manager.review

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.data.review.DoctorReviewRepository

/** Read-only moderation view; reviews are clinical service evidence and are never silently deleted. */
class ManagerReviewsActivity : AppCompatActivity() {
    private val repository = DoctorReviewRepository()
    private lateinit var list: LinearLayout
    private lateinit var summary: TextView
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(20)); setBackgroundColor(getColor(R.color.manager_background)) }
        root.addView(TextView(this).apply { text = "Đánh giá bác sĩ"; textSize = 24f; setTextColor(getColor(R.color.manager_ink)); setTypeface(typeface, 1) })
        summary = TextView(this).apply { setPadding(0, dp(14), 0, dp(14)); setTextColor(getColor(R.color.manager_muted)); textSize = 15f }
        root.addView(summary)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(list); layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) })
        setContentView(root)
        load()
    }
    private fun load() = repository.getAll { result -> result.onSuccess { reviews ->
        summary.text = if (reviews.isEmpty()) "Chưa có đánh giá." else "${reviews.size} đánh giá · Điểm trung bình: ${"%.1f".format(reviews.map { it.rating }.average())}/5"
        list.removeAllViews()
        reviews.forEach { review ->
            list.addView(TextView(this).apply {
                text = "${review.doctorName.ifBlank { "Bác sĩ" }} · ${"★".repeat(review.rating)}${"☆".repeat((5 - review.rating).coerceAtLeast(0))}\n${review.comment.ifBlank { "Không có nhận xét." }}"
                setTextColor(getColor(R.color.manager_ink)); textSize = 15f; setPadding(dp(16), dp(14), dp(16), dp(14)); setBackgroundResource(R.drawable.manager_card_background)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(10) }
            })
        }
    }.onFailure { summary.text = it.message ?: "Không thể tải đánh giá." } }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
