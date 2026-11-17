package com.example.medicare.user.review

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.medicare.R
import com.example.medicare.data.review.DoctorReviewRepository
import com.example.medicare.databinding.ActivityDoctorReviewBinding

class ReviewDoctorActivity : AppCompatActivity() {
 private lateinit var binding: ActivityDoctorReviewBinding
 override fun onCreate(state: Bundle?) { super.onCreate(state); binding = ActivityDoctorReviewBinding.inflate(layoutInflater); setContentView(binding.root); binding.reviewBack.setOnClickListener { finish() }; binding.reviewDoctorName.text = intent.getStringExtra(EXTRA_DOCTOR_NAME).orEmpty(); binding.reviewSave.setOnClickListener { save() } }
 private fun save() { val rating = binding.reviewRating.rating.toInt(); if (rating == 0) { Toast.makeText(this, R.string.review_rating_required, Toast.LENGTH_SHORT).show(); return }; binding.reviewSave.isEnabled = false; DoctorReviewRepository().save(intent.getStringExtra(EXTRA_APPOINTMENT_ID).orEmpty(), intent.getStringExtra(EXTRA_DOCTOR_ID).orEmpty(), intent.getStringExtra(EXTRA_DOCTOR_NAME).orEmpty(), rating, binding.reviewComment.text.toString().trim()) { result -> binding.reviewSave.isEnabled = true; result.onSuccess { finish() }.onFailure { Toast.makeText(this, it.message, Toast.LENGTH_LONG).show() } } }
 companion object { const val EXTRA_APPOINTMENT_ID="appointment_id"; const val EXTRA_DOCTOR_ID="doctor_id"; const val EXTRA_DOCTOR_NAME="doctor_name" }
}
