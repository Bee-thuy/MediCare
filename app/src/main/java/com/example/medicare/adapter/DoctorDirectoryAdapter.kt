package com.example.medicare.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.medicare.databinding.ItemUserDoctorBinding
import com.example.medicare.model.Doctor

class DoctorDirectoryAdapter(
    private val onDoctorSelected: (Doctor) -> Unit,
) : RecyclerView.Adapter<DoctorDirectoryAdapter.DoctorViewHolder>() {
    private var doctors: List<Doctor> = emptyList()

    fun submitList(value: List<Doctor>) {
        doctors = value
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder = DoctorViewHolder(
        ItemUserDoctorBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) = holder.bind(doctors[position])

    override fun getItemCount(): Int = doctors.size

    inner class DoctorViewHolder(private val binding: ItemUserDoctorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(doctor: Doctor) {
            binding.userDoctorInitials.text = doctor.fullName
                .split(" ")
                .filter(String::isNotBlank)
                .takeLast(2)
                .joinToString("") { it.first().toString() }
            binding.userDoctorName.text = doctor.fullName
            binding.userDoctorSpecialty.text = doctor.specialty
            binding.userDoctorExperience.text = doctor.experienceYears
                .takeIf(String::isNotBlank)
                ?.let { "$it năm kinh nghiệm" }
                .orEmpty()
            binding.root.setOnClickListener { onDoctorSelected(doctor) }
        }
    }
}
