package com.example.medicare.manager.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.medicare.R
import com.example.medicare.databinding.FragmentManagerDoctorsBinding
import com.example.medicare.databinding.ItemManagedDoctorBinding
import com.example.medicare.manager.docter.AddDoctorActivity
import com.example.medicare.manager.docter.DoctorManagementRepository
import com.example.medicare.manager.docter.ManagedSpecialty

class ManagerDoctorsFragment : Fragment() {
    private var _binding: FragmentManagerDoctorsBinding? = null
    private val binding get() = _binding!!
    private var doctors: List<com.example.medicare.manager.docter.ManagedDoctor> = emptyList()
    private var selectedSpecialty = ""
    private var selectedStatus = ""
    private val addDoctor = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && _binding != null) renderDoctors()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentManagerDoctorsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.managerDoctorsBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.managerDoctorsAdd.setOnClickListener { openAddDoctor() }
        binding.managerDoctorsSearch.doAfterTextChanged { query -> showDoctors(query?.toString().orEmpty()) }
        binding.managerDoctorsSpecialtyFilter.setOnItemClickListener { _, _, position, _ ->
            selectedSpecialty = if (position == 0) "" else binding.managerDoctorsSpecialtyFilter.adapter.getItem(position).toString()
            showDoctors(binding.managerDoctorsSearch.text?.toString().orEmpty())
        }
        binding.managerDoctorsStatusFilter.setOnItemClickListener { _, _, position, _ ->
            selectedStatus = if (position == 0) "" else binding.managerDoctorsStatusFilter.adapter.getItem(position).toString()
            showDoctors(binding.managerDoctorsSearch.text?.toString().orEmpty())
        }
        loadSpecialties()
        renderDoctors()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) renderDoctors()
    }

    private fun openAddDoctor() {
        addDoctor.launch(Intent(requireContext(), AddDoctorActivity::class.java))
    }

    private fun loadSpecialties() {
        DoctorManagementRepository(requireContext()).getSpecialties { result ->
            if (_binding == null) return@getSpecialties
            val labels = listOf(getString(R.string.manager_all_specialties)) +
                result.getOrDefault(emptyList<ManagedSpecialty>()).map { it.name }
            binding.managerDoctorsSpecialtyFilter.setAdapter(
                android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels),
            )
            binding.managerDoctorsStatusFilter.setAdapter(
                android.widget.ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    listOf(getString(R.string.manager_all_statuses), getString(R.string.manager_active), getString(R.string.manager_inactive)),
                ),
            )
        }
    }

    private fun renderDoctors() {
        DoctorManagementRepository(requireContext()).getDoctors { result ->
            if (_binding == null) return@getDoctors
            doctors = result.getOrDefault(emptyList())
            showDoctors(binding.managerDoctorsSearch.text?.toString().orEmpty())
        }
    }

    private fun showDoctors(query: String) {
        if (_binding == null) return
        val displayedDoctors = doctors.filter { doctor ->
            (doctor.fullName.contains(query, ignoreCase = true) ||
                doctor.specialty.contains(query, ignoreCase = true)) &&
                (selectedSpecialty.isEmpty() || doctor.specialty == selectedSpecialty) &&
                (selectedStatus.isEmpty() ||
                    (selectedStatus == getString(R.string.manager_active) && doctor.isActive) ||
                    (selectedStatus == getString(R.string.manager_inactive) && !doctor.isActive))
        }
        binding.managerDoctorsList.removeAllViews()
        binding.managerDoctorsEmpty.visibility = if (displayedDoctors.isEmpty()) View.VISIBLE else View.GONE
        displayedDoctors.forEach { doctor ->
                val itemBinding = ItemManagedDoctorBinding.inflate(layoutInflater, binding.managerDoctorsList, false)
                itemBinding.managedDoctorName.text = doctor.fullName
                itemBinding.managedDoctorSpecialty.text = doctor.specialty
                itemBinding.managedDoctorStatus.apply {
                    text = getString(if (doctor.isActive) R.string.manager_active else R.string.manager_inactive)
                    setTextColor(ContextCompat.getColor(requireContext(), if (doctor.isActive) R.color.manager_green else R.color.manager_red))
                    setBackgroundResource(if (doctor.isActive) R.drawable.manager_status_active_background else R.drawable.manager_status_inactive_background)
                }
                binding.managerDoctorsList.addView(itemBinding.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
