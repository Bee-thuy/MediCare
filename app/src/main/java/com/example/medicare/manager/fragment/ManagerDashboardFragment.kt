package com.example.medicare.manager.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.medicare.R
import com.example.medicare.databinding.FragmentManagerDashboardBinding
import com.example.medicare.manager.docter.AddDoctorActivity
import com.example.medicare.manager.docter.DoctorManagementRepository
import com.example.medicare.manager.docter.SpecialtyActivity
import com.example.medicare.manager.docter.WorkShiftActivity
import java.text.NumberFormat
import java.util.Locale

class ManagerDashboardFragment : Fragment() {
    private var _binding: FragmentManagerDashboardBinding? = null
    private val binding get() = _binding!!
    private val repository by lazy { DoctorManagementRepository(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentManagerDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.dashboardQuickAddDoctor.root.setOnClickListener {
            startActivity(Intent(requireContext(), AddDoctorActivity::class.java))
        }
        binding.dashboardQuickAddSpecialty.root.setOnClickListener {
            startActivity(Intent(requireContext(), SpecialtyActivity::class.java))
        }
        binding.dashboardQuickAddWorkShift.root.setOnClickListener {
            startActivity(Intent(requireContext(), WorkShiftActivity::class.java))
        }
        binding.dashboardQuickApproveAppointments.root.setOnClickListener {
            Toast.makeText(
                requireContext(),
                R.string.manager_appointments_unavailable,
                Toast.LENGTH_SHORT,
            ).show()
        }
        loadSummary()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) loadSummary()
    }

    private fun loadSummary() {
        repository.getDoctors { result ->
            val currentBinding = _binding ?: return@getDoctors
            val count = result.getOrNull()?.size
            currentBinding.dashboardDoctorsCard.dashboardDoctorsValue.text = count?.let(::formatCount)
                ?: getString(R.string.manager_no_data)
        }
        repository.getPatientCount { result ->
            val currentBinding = _binding ?: return@getPatientCount
            val count = result.getOrNull()
            currentBinding.dashboardPatientsCard.dashboardPatientsValue.text = count?.let(::formatCount)
                ?: getString(R.string.manager_no_data)
        }
    }

    private fun formatCount(value: Int): String {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(value)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
