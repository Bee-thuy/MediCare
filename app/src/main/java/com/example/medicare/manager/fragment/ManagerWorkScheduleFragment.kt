package com.example.medicare.manager.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.medicare.databinding.ActivityWorkScheduleBinding
import com.example.medicare.manager.work_schedule.AssignDoctorScheduleActivity
import com.example.medicare.manager.work_schedule.WeeklyScheduleActivity
import com.example.medicare.manager.work_schedule.WorkScheduleDetailActivity

/** Work-schedule tab hosted directly by ManagerActivity. */
class ManagerWorkScheduleFragment : Fragment() {
    private var _binding: ActivityWorkScheduleBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = ActivityWorkScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.workScheduleBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.workScheduleWeekly.setOnClickListener {
            startActivity(Intent(requireContext(), WeeklyScheduleActivity::class.java))
        }
        binding.workScheduleAssign.setOnClickListener {
            startActivity(Intent(requireContext(), AssignDoctorScheduleActivity::class.java))
        }
        binding.workScheduleDoctorA.setOnClickListener { openDetail("BS. Nguyễn Văn A", "Tim mạch") }
        binding.workScheduleDoctorB.setOnClickListener { openDetail("BS. Trần Thị B", "Nội khoa") }
    }

    private fun openDetail(name: String, specialty: String) {
        startActivity(
            Intent(requireContext(), WorkScheduleDetailActivity::class.java)
                .putExtra(WorkScheduleDetailActivity.EXTRA_DOCTOR_NAME, name)
                .putExtra(WorkScheduleDetailActivity.EXTRA_SPECIALTY, specialty),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
