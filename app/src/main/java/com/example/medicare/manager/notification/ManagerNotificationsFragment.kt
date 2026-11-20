package com.example.medicare.manager.notification

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.medicare.R
import com.example.medicare.data.notification.NotificationRepository
import com.example.medicare.databinding.FragmentManagerNotificationsBinding
import com.example.medicare.manager.appointment.PatientNotificationActivity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Manager notification templates and history of broadcast messages stored in Firestore. */
class ManagerNotificationsFragment : Fragment() {
    private var _binding: FragmentManagerNotificationsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentManagerNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.managerNotificationsBroadcast.setOnClickListener { compose() }
        binding.managerNotificationsCompose.setOnClickListener { compose() }
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun compose() {
        startActivity(Intent(requireContext(), PatientNotificationActivity::class.java)
            .putExtra(PatientNotificationActivity.EXTRA_SEND_TO_ALL, true))
    }

    private fun loadHistory() {
        NotificationRepository().getAdminSentHistory { result ->
            if (_binding == null) return@getAdminSentHistory
            binding.managerNotificationsHistoryList.removeAllViews()
            result.onSuccess { messages ->
                binding.managerNotificationsHistoryEmpty.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
                messages.forEach { item ->
                    val date = if (item.createdAt <= 0L) "" else Instant.ofEpochMilli(item.createdAt)
                        .atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    val row = TextView(requireContext()).apply {
                        val audience = when (item.audience) {
                            "ALL" -> "Tất cả tài khoản"
                            "DOCTOR" -> "Bác sĩ"
                            "USER" -> "Bệnh nhân"
                            else -> if (item.type == "GENERAL") "Tất cả bệnh nhân" else "Bệnh nhân"
                        }
                        text = "🔔  ${item.body}\nGửi tới: $audience${if (date.isBlank()) "" else " · $date"}"
                        textSize = 12f
                        setTextColor(requireContext().getColor(R.color.manager_ink))
                        setPadding(dp(14), dp(12), dp(14), dp(12))
                        setBackgroundResource(R.drawable.manager_card_background)
                    }
                    binding.managerNotificationsHistoryList.addView(row,
                        LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) })
                }
            }.onFailure {
                binding.managerNotificationsHistoryEmpty.text = "Không tải được lịch sử thông báo lúc này."
                binding.managerNotificationsHistoryEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
