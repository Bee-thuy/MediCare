package com.example.medicare.manager.schedule

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.medicare.R
import com.example.medicare.data.doctor.DoctorManagementRepository
import com.example.medicare.data.schedule.DoctorLeaveRequestRepository
import com.example.medicare.databinding.ActivityLeaveRequestsBinding
import com.example.medicare.databinding.ItemLeaveRequestBinding
import com.example.medicare.databinding.SheetLeaveDecisionBinding
import com.example.medicare.model.DoctorLeaveRequest
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LeaveRequestsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLeaveRequestsBinding
    private val repository = DoctorLeaveRequestRepository()
    private val doctors by lazy { DoctorManagementRepository(this) }
    private var doctorNames: Map<String, String> = emptyMap()
    private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        binding = ActivityLeaveRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.leaveRequestsBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadDoctorsThenRequests()
    }

    private fun loadDoctorsThenRequests() {
        doctors.getDoctors { result ->
            if (isFinishing || isDestroyed) return@getDoctors
            result.onSuccess { list ->
                doctorNames = list.associate { it.id to it.fullName }
                load()
            }.onFailure {
                doctorNames = emptyMap()
                load()
            }
        }
    }

    private fun load() {
        repository.getPending { result ->
            if (isFinishing || isDestroyed) return@getPending
            result.onSuccess(::render).onFailure {
                Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun render(requests: List<DoctorLeaveRequest>) {
        binding.leaveRequestsList.removeAllViews()
        binding.leaveRequestsEmpty.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
        requests.forEach { request ->
            val row = ItemLeaveRequestBinding.inflate(layoutInflater, binding.leaveRequestsList, false)
            val isChange = request.reason.startsWith("Đổi ca:")
            val typeLabel = if (isChange) "Đổi ca" else "Xin nghỉ"
            val reasonBody = request.reason
                .removePrefix("Đổi ca:")
                .removePrefix("Xin nghỉ:")
                .trim()
                .ifBlank { request.reason }
            row.leaveRequestDoctor.text = doctorNames[request.doctorId]
                ?.let { "BS. $it" }
                ?: "Bác sĩ"
            row.leaveRequestType.text = typeLabel
            row.leaveRequestType.setBackgroundResource(
                if (isChange) R.drawable.leave_type_badge_change else R.drawable.leave_type_badge_rest,
            )
            row.leaveRequestType.setTextColor(
                ContextCompat.getColor(this, if (isChange) R.color.manager_blue else R.color.manager_orange),
            )
            row.leaveRequestDate.text = "Ngày áp dụng: ${formatDate(request.date)}"
            row.leaveRequestReason.text = reasonBody
            row.root.setOnClickListener { decide(request) }
            binding.leaveRequestsList.addView(row.root)
        }
    }

    private fun decide(request: DoctorLeaveRequest) {
        val isChange = request.reason.startsWith("Đổi ca:")
        val sheet = SheetLeaveDecisionBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(sheet.root)
        sheet.leaveDecisionTitle.text = if (isChange) "Duyệt đề nghị đổi ca?" else "Duyệt yêu cầu xin nghỉ?"
        sheet.leaveDecisionMessage.text = if (isChange) {
            "Đề nghị đổi ca ngày ${formatDate(request.date)}.\nSau khi duyệt, hãy tạo ca mới cho bác sĩ."
        } else {
            "Duyệt sẽ khóa lịch làm việc của bác sĩ trong ngày ${formatDate(request.date)} và đánh dấu các lịch khám cần sắp xếp lại."
        }
        sheet.leaveDecisionApprove.setOnClickListener {
            dialog.dismiss()
            saveDecision(request, true)
        }
        sheet.leaveDecisionReject.setOnClickListener {
            dialog.dismiss()
            saveDecision(request, false)
        }
        dialog.show()
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
    }

    private fun saveDecision(request: DoctorLeaveRequest, approved: Boolean) {
        repository.decide(request, approved) { result ->
            if (isFinishing || isDestroyed) return@decide
            result.onSuccess {
                Toast.makeText(
                    this,
                    if (approved) "Đã duyệt yêu cầu." else "Đã từ chối yêu cầu.",
                    Toast.LENGTH_SHORT,
                ).show()
                load()
            }.onFailure {
                Toast.makeText(this, it.message ?: getString(R.string.manager_save_failed), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun formatDate(raw: String): String =
        runCatching { LocalDate.parse(raw).format(dateFormat) }.getOrDefault(raw)
}
