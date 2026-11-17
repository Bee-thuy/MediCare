package com.example.medicare.manager.schedule

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.data.schedule.DoctorLeaveRequestRepository
import com.example.medicare.databinding.ActivityLeaveRequestsBinding
import com.example.medicare.model.DoctorLeaveRequest

class LeaveRequestsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLeaveRequestsBinding
    private val repository = DoctorLeaveRequestRepository()
    override fun onCreate(state: Bundle?) { super.onCreate(state); binding = ActivityLeaveRequestsBinding.inflate(layoutInflater); setContentView(binding.root); binding.leaveRequestsBack.setOnClickListener { finish() } }
    override fun onResume() { super.onResume(); load() }
    private fun load() { repository.getPending { result -> result.onSuccess(::render).onFailure { Toast.makeText(this, it.message, Toast.LENGTH_LONG).show() } } }
    private fun render(requests: List<DoctorLeaveRequest>) {
        binding.leaveRequestsList.removeAllViews(); binding.leaveRequestsEmpty.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
        requests.forEach { request ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 16, 16, 16); setBackgroundResource(R.drawable.manager_card_background); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 } }
            row.addView(TextView(this).apply { text = "${request.date} · ${request.reason}"; textSize = 16f; setTextColor(getColor(R.color.manager_ink)) })
            row.addView(TextView(this).apply { text = "Nhấn để duyệt và khóa lịch làm việc trong ngày."; textSize = 13f; setTextColor(getColor(R.color.manager_muted)); setPadding(0, 8, 0, 0) })
            row.setOnClickListener { decide(request) }; binding.leaveRequestsList.addView(row)
        }
    }
    private fun decide(request: DoctorLeaveRequest) { AlertDialog.Builder(this).setTitle(R.string.manager_leave_requests).setMessage("Duyệt yêu cầu sẽ khóa các lịch làm việc của bác sĩ trong ngày ${request.date}.").setNegativeButton(R.string.manager_reject) { _, _ -> saveDecision(request, false) }.setPositiveButton(R.string.manager_accept) { _, _ -> saveDecision(request, true) }.show() }
    private fun saveDecision(request: DoctorLeaveRequest, approved: Boolean) { repository.decide(request, approved) { result -> result.onSuccess { load() }.onFailure { Toast.makeText(this, it.message ?: getString(R.string.manager_save_failed), Toast.LENGTH_LONG).show() } } }
}
