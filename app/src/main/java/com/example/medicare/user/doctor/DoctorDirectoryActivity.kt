package com.example.medicare.user.doctor

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.medicare.adapter.DoctorDirectoryAdapter
import com.example.medicare.data.doctor.DoctorManagementRepository
import com.example.medicare.databinding.ActivityDoctorDirectoryBinding
import com.example.medicare.model.Doctor
import android.widget.ArrayAdapter
import com.example.medicare.user.appointment.BookAppointmentActivity

class DoctorDirectoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDoctorDirectoryBinding
    private val repository by lazy { DoctorManagementRepository(this) }
    private val adapter = DoctorDirectoryAdapter(::openBooking)
    private var doctors: List<Doctor> = emptyList()
    private var selectedSpecialty: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorDirectoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.doctorDirectoryBack.setOnClickListener { finish() }
        binding.doctorDirectoryList.layoutManager = LinearLayoutManager(this)
        binding.doctorDirectoryList.adapter = adapter
        binding.doctorDirectorySearch.doAfterTextChanged { showDoctors(it?.toString().orEmpty()) }
        binding.doctorDirectorySpecialtyFilter.setOnItemClickListener { _, _, position, _ ->
            selectedSpecialty = if (position == 0) null else binding.doctorDirectorySpecialtyFilter.adapter.getItem(position).toString()
            showDoctors(binding.doctorDirectorySearch.text?.toString().orEmpty())
        }
        loadDoctors()
    }

    private fun loadDoctors() {
        repository.getActiveDoctors { result ->
            result.onSuccess {
                doctors = it
                val specialties = listOf(getString(com.example.medicare.R.string.patient_all_specialties)) + it.map(Doctor::specialty).filter(String::isNotBlank).distinct().sorted()
                binding.doctorDirectorySpecialtyFilter.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, specialties))
                showDoctors(binding.doctorDirectorySearch.text?.toString().orEmpty())
            }.onFailure {
                doctors = emptyList()
                adapter.submitList(emptyList())
                binding.doctorDirectoryEmpty.text = "Không tải được danh sách bác sĩ. Kiểm tra Firestore Rules."
                binding.doctorDirectoryEmpty.isVisible = true
                Toast.makeText(this, it.message ?: "Không tải được danh sách bác sĩ.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showDoctors(query: String) {
        val displayed = doctors.filter {
            (it.fullName.contains(query, ignoreCase = true) || it.specialty.contains(query, ignoreCase = true)) &&
                (selectedSpecialty == null || it.specialty == selectedSpecialty)
        }
        adapter.submitList(displayed)
        binding.doctorDirectoryEmpty.text = "Chưa có bác sĩ đang hoạt động."
        binding.doctorDirectoryEmpty.isVisible = displayed.isEmpty()
    }

    private fun openBooking(doctor: Doctor) {
        startActivity(
            Intent(this, BookAppointmentActivity::class.java)
                .putExtra(BookAppointmentActivity.EXTRA_DOCTOR_ID, doctor.id)
                .putExtra(BookAppointmentActivity.EXTRA_DOCTOR_NAME, doctor.fullName)
                .putExtra(BookAppointmentActivity.EXTRA_SPECIALTY, doctor.specialty),
        )
    }
}
