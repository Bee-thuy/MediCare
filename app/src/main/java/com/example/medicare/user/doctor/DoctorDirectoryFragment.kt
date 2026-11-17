package com.example.medicare.user.doctor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.medicare.R
import com.example.medicare.adapter.DoctorDirectoryAdapter
import com.example.medicare.data.doctor.DoctorManagementRepository
import com.example.medicare.databinding.ActivityDoctorDirectoryBinding
import com.example.medicare.model.Doctor
import com.example.medicare.user.PatientHomeActivity

class DoctorDirectoryFragment : Fragment() {
    private var _binding: ActivityDoctorDirectoryBinding? = null
    private val binding get() = _binding!!
    private val adapter = DoctorDirectoryAdapter { doctor ->
        (activity as? PatientHomeActivity)?.showBooking(doctor)
    }
    private var doctors: List<Doctor> = emptyList()
    private var selectedSpecialty: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = ActivityDoctorDirectoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        binding.doctorDirectoryHeader.visibility = View.GONE
        binding.doctorDirectoryList.layoutManager = LinearLayoutManager(requireContext())
        binding.doctorDirectoryList.adapter = adapter
        binding.doctorDirectorySearch.doAfterTextChanged { showDoctors(it?.toString().orEmpty()) }
        binding.doctorDirectorySpecialtyFilter.setOnClickListener {
            binding.doctorDirectorySpecialtyFilter.showDropDown()
        }
        binding.doctorDirectorySpecialtyFilter.setOnItemClickListener { _, _, position, _ ->
            selectedSpecialty = if (position == 0) null
                else binding.doctorDirectorySpecialtyFilter.adapter.getItem(position).toString()
            showDoctors(binding.doctorDirectorySearch.text?.toString().orEmpty())
        }
        loadDoctors()
    }

    private fun loadDoctors() {
        DoctorManagementRepository(requireContext()).getActiveDoctors { result ->
            if (_binding == null) return@getActiveDoctors
            result.onSuccess {
                doctors = it
                val specialties = listOf(getString(R.string.patient_all_specialties)) +
                    it.map(Doctor::specialty).filter(String::isNotBlank).distinct().sorted()
                binding.doctorDirectorySpecialtyFilter.setAdapter(
                    ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, specialties))
                showDoctors(binding.doctorDirectorySearch.text?.toString().orEmpty())
            }.onFailure {
                doctors = emptyList()
                adapter.submitList(emptyList())
                binding.doctorDirectoryEmpty.text = "Không tải được danh sách bác sĩ. Kiểm tra kết nối và quyền truy cập."
                binding.doctorDirectoryEmpty.isVisible = true
                Toast.makeText(requireContext(), it.message ?: "Không tải được danh sách bác sĩ.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showDoctors(query: String) {
        if (_binding == null) return
        val displayed = doctors.filter {
            (it.fullName.contains(query, true) || it.specialty.contains(query, true)) &&
                (selectedSpecialty == null || it.specialty == selectedSpecialty)
        }
        adapter.submitList(displayed)
        binding.doctorDirectoryEmpty.text = if (query.isBlank() && selectedSpecialty == null)
            "Chưa có bác sĩ đang hoạt động." else "Không tìm thấy bác sĩ phù hợp."
        binding.doctorDirectoryEmpty.isVisible = displayed.isEmpty()
    }

    override fun onDestroyView() {
        binding.doctorDirectoryList.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
