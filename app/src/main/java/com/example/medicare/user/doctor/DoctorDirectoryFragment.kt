package com.example.medicare.user.doctor

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.medicare.R
import com.example.medicare.adapter.DoctorDirectoryAdapter
import com.example.medicare.data.doctor.DoctorManagementRepository
import com.example.medicare.data.review.DoctorReviewRepository
import com.example.medicare.databinding.FragmentPatientSearchBinding
import com.example.medicare.model.Doctor
import com.example.medicare.user.PatientHomeActivity
import com.example.medicare.user.PatientDetailSheets

class DoctorDirectoryFragment : Fragment() {
    private var _binding: FragmentPatientSearchBinding? = null
    private val binding get() = _binding!!
    private val adapter = DoctorDirectoryAdapter(::bookDoctor, ::showDetails)
    private var doctors: List<Doctor> = emptyList()
    private var selectedSpecialty: String? = null
    private var ratings: Map<String, Pair<Double, Int>> = emptyMap()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentPatientSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        binding.patientSearchList.layoutManager = LinearLayoutManager(requireContext())
        binding.patientSearchList.adapter = adapter
        binding.patientSearchInput.doAfterTextChanged { showDoctors(it?.toString().orEmpty()) }
        loadDoctors()
        DoctorReviewRepository().getAll { result ->
            if (_binding == null) return@getAll
            ratings = result.getOrDefault(emptyList()).groupBy { it.doctorId }.mapValues { (_, reviews) ->
                reviews.map { it.rating }.average() to reviews.size
            }
            adapter.updateRatings(ratings)
        }
    }

    private fun loadDoctors() {
        DoctorManagementRepository(requireContext()).getActiveDoctors { result ->
            if (_binding == null) return@getActiveDoctors
            result.onSuccess {
                doctors = it.sortedBy(Doctor::fullName)
                renderChips()
                showDoctors(binding.patientSearchInput.text?.toString().orEmpty())
            }.onFailure {
                doctors = emptyList()
                adapter.submitList(emptyList())
                binding.patientSearchEmpty.text = "Không tải được danh sách bác sĩ. Kiểm tra kết nối và quyền truy cập."
                binding.patientSearchEmpty.visibility = View.VISIBLE
                Toast.makeText(requireContext(), it.message ?: "Không tải được danh sách bác sĩ.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun renderChips() {
        binding.patientSearchChips.removeAllViews()
        val options = listOf(null) + doctors.map(Doctor::specialty).filter(String::isNotBlank).distinct().sorted()
        options.forEach { specialty ->
            val chip = TextView(requireContext()).apply {
                text = specialty ?: "Tất cả"
                textSize = 10f
                gravity = Gravity.CENTER
                isSelected = specialty == selectedSpecialty
                setTextColor(requireContext().getColor(if (isSelected) R.color.white else R.color.manager_muted))
                setBackgroundResource(R.drawable.patient_search_chip_background)
                setPadding(dp(12), 0, dp(12), 0)
                setOnClickListener {
                    selectedSpecialty = specialty
                    renderChips()
                    showDoctors(binding.patientSearchInput.text?.toString().orEmpty())
                }
            }
            binding.patientSearchChips.addView(chip, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(30)).apply { marginEnd = dp(6) })
        }
    }

    private fun showDoctors(query: String) {
        if (_binding == null) return
        val displayed = doctors.filter {
            (it.fullName.contains(query, true) || it.specialty.contains(query, true)) &&
                (selectedSpecialty == null || it.specialty == selectedSpecialty)
        }
        adapter.submitList(displayed)
        binding.patientSearchCount.text = "Kết quả tìm kiếm (${displayed.size})"
        binding.patientSearchEmpty.text = if (doctors.isEmpty()) "Chưa có bác sĩ đang hoạt động." else "Không tìm thấy bác sĩ phù hợp."
        binding.patientSearchEmpty.visibility = if (displayed.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showDetails(doctor: Doctor) {
        PatientDetailSheets.doctor(requireActivity(), doctor, ratings[doctor.id]) { bookDoctor(doctor) }
    }

    private fun bookDoctor(doctor: Doctor) {
        (activity as? PatientHomeActivity)?.showBooking(doctor)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()

    override fun onDestroyView() {
        binding.patientSearchList.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
