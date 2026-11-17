package com.example.medicare.manager.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.medicare.data.auth.AuthRepository
import com.example.medicare.databinding.FragmentManagerMoreBinding
import com.example.medicare.manager.doctor.SpecialtyActivity
import com.example.medicare.manager.doctor.WorkShiftActivity
import com.example.medicare.ui.auth.LoginActivity
import com.example.medicare.manager.schedule.LeaveRequestsActivity
import com.example.medicare.manager.dashboard.ManagerReportActivity
import com.example.medicare.manager.appointment.PatientNotificationActivity
import com.example.medicare.manager.review.ManagerReviewsActivity
import com.example.medicare.manager.ManagerActivity

class ManagerMoreFragment : Fragment() {
    private var _binding: FragmentManagerMoreBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentManagerMoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.managerMoreDashboard.setOnClickListener {
            (activity as? ManagerActivity)?.showTab(ManagerActivity.TAB_HOME)
        }
        binding.managerMoreAppointments.setOnClickListener {
            (activity as? ManagerActivity)?.showTab(ManagerActivity.TAB_APPOINTMENTS)
        }
        binding.managerMoreDoctors.setOnClickListener {
            (activity as? ManagerActivity)?.showTab(ManagerActivity.TAB_DOCTORS)
        }
        binding.managerMoreSchedules.setOnClickListener {
            (activity as? ManagerActivity)?.showTab(ManagerActivity.TAB_WORK_SCHEDULE)
        }
        binding.managerMoreSpecialties.setOnClickListener {
            startActivity(Intent(requireContext(), SpecialtyActivity::class.java))
        }
        binding.managerMoreWorkShifts.setOnClickListener {
            startActivity(Intent(requireContext(), WorkShiftActivity::class.java))
        }
        binding.managerMorePatients.setOnClickListener {
            (activity as? ManagerActivity)?.showTab(ManagerActivity.TAB_PATIENTS)
        }
        binding.managerMoreLeaveRequests.setOnClickListener {
            startActivity(Intent(requireContext(), LeaveRequestsActivity::class.java))
        }
        binding.managerMoreReports.setOnClickListener { startActivity(Intent(requireContext(), ManagerReportActivity::class.java)) }
        binding.managerMoreBroadcast.setOnClickListener {
            startActivity(Intent(requireContext(), PatientNotificationActivity::class.java)
                .putExtra(PatientNotificationActivity.EXTRA_SEND_TO_ALL, true))
        }
        binding.managerMoreReviews.setOnClickListener { startActivity(Intent(requireContext(), ManagerReviewsActivity::class.java)) }
        binding.managerMoreNotifications.setOnClickListener {
            (activity as? ManagerActivity)?.showTab(ManagerActivity.TAB_NOTIFICATIONS)
        }
        binding.managerMoreSignOut.setOnClickListener {
            AuthRepository().signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
