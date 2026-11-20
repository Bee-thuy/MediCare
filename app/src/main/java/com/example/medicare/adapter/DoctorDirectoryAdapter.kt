package com.example.medicare.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.medicare.databinding.ItemUserDoctorBinding
import com.example.medicare.model.Doctor

class DoctorDirectoryAdapter(
    private val onDoctorSelected: (Doctor) -> Unit,
    private val onDetails: (Doctor) -> Unit = onDoctorSelected,
) : RecyclerView.Adapter<DoctorDirectoryAdapter.DoctorViewHolder>() {
    private var doctors: List<Doctor> = emptyList()
    private var ratings: Map<String, Pair<Double, Int>> = emptyMap()

    fun submitList(value: List<Doctor>) {
        doctors = value
        notifyDataSetChanged()
    }

    fun updateRatings(value: Map<String, Pair<Double, Int>>) {
        ratings = value
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder = DoctorViewHolder(
        ItemUserDoctorBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )

    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) = holder.bind(doctors[position])

    override fun getItemCount(): Int = doctors.size

    inner class DoctorViewHolder(private val binding: ItemUserDoctorBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(doctor: Doctor) {
            binding.userDoctorInitials.text = "👨‍⚕️"
            binding.userDoctorName.text = "BS. ${doctor.fullName}"
            binding.userDoctorSpecialty.text = listOfNotNull(doctor.specialty.takeIf(String::isNotBlank),
                doctor.experienceYears.takeIf(String::isNotBlank)?.let { "$it năm kinh nghiệm" })
                .joinToString(" · ")
            val rating = ratings[doctor.id]
            binding.userDoctorRating.text = if (rating == null) "⭐ Chưa có đánh giá" else
                "⭐ ${"%.1f".format(rating.first)} · ${rating.second} đánh giá"
            binding.root.setOnClickListener { onDetails(doctor) }
            binding.userDoctorDetails.setOnClickListener { onDetails(doctor) }
            binding.userDoctorBook.setOnClickListener { onDoctorSelected(doctor) }
        }
    }
}
